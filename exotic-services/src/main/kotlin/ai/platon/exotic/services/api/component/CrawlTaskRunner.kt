package ai.platon.exotic.services.api.component

import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.driver.crawl.scraper.RuleStatus
import ai.platon.exotic.driver.crawl.scraper.ScrapeTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.api.persist.PortalTaskRepository
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.collect.queue.ConcurrentNEntrantQueue
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.driver.scrape_node.services.ScrapeNodeService
import ai.platon.pulsar.persist.metadata.IpType
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
class CrawlTaskRunner(
    val crawlRuleRepository: CrawlRuleRepository,
    val portalTaskRepository: PortalTaskRepository,
    val scraper: ExoticCrawler
) {
    private val parameterConcurrentTaskCnt: Int = 5
    private val logger = LoggerFactory.getLogger(CrawlTaskRunner::class.java)
    private val traceLogger = LoggerFactory.getLogger("com.kc.trace")

    private val retryingPortalTasks = ConcurrentNEntrantQueue<PortalTask>(5)
    private val retryingItemTasks = ConcurrentNEntrantQueue<ScrapeTask>(3)

    private val processingRules = ConcurrentHashMap<Long, Boolean>();

    private val scrapeNodeService = ScrapeNodeService.instance

    @Synchronized
    fun loadUnfinishedTasks() {
        // portalTaskRepository.findAllByStatus("Running")
    }

    @Synchronized
    fun startCreatedCrawlRules() {
        val now = Instant.now()

        val status = listOf(RuleStatus.Created).map { it.toString() }
        val sort = Sort.by(Sort.Order.desc("id"))
        val page = PageRequest.of(0, 1000, sort)

        val rules = crawlRuleRepository.findAllByStatusIn(status, page)
            .filter { it.startTime.epochSecond <= now.epochSecond }

        rules.forEach { rule -> startCrawl(rule) }
    }

    @Synchronized
    fun restartCrawlRulesNextRound() {
        traceLogger.info("restartCrawlRulesNextRound");
        val status = listOf(RuleStatus.Running, RuleStatus.Finished).map { it.toString() }
        val sort = Sort.by(Sort.Order.desc("id"))
        val page = PageRequest.of(0, 1000, sort)
        val rules = crawlRuleRepository.findAllByStatusIn(status, page)
            .filter { shouldRun(it) }

        rules.forEach { rule -> startCrawl(rule) }
    }

    fun shouldRun(rule: CrawlRule): Boolean {
        if (processingRules.containsKey(rule.id)) {
            return false
        }
        return try {
            shouldRun0(rule)
        } catch (e: Exception) {
            logger.warn(e.stringify())
            false
        }
    }

    @Synchronized
    fun startCrawl(rule: CrawlRule) {
        try {
            if (rule.ipTypeWant == IpType.RESIDENCE.name && scrapeNodeService.getNodesByIpType(IpType.RESIDENCE)
                    .isEmpty()
            ) {
                logger.warn("no RESIDENCE scrape node ready")
                return
            }
            val now = Instant.now()

            rule.status = RuleStatus.Running.toString()
            rule.crawlCount = rule.crawlCount?.inc()
            rule.lastCrawlTime = now
            crawlRuleRepository.save(rule)
            crawlRuleRepository.flush()

            val portalUrls = rule.portalUrls

            if (portalUrls.isBlank()) {
                rule.status = RuleStatus.Finished.toString()
                logger.info("No portal urls in rule #{}", rule.id)
                return
            }

//            val maxPages = if (IS_DEVELOPMENT) 2 else rule.maxPages
            val maxPages = 9999999
            val pagedPortalUrls = portalUrls.split("\n")
                .map { it.trim() }
                .filter { UrlUtils.isValidUrl(it) }
                .distinct()
                .flatMap { url -> createPagedUrls(url, maxPages) }
            if (pagedPortalUrls.isEmpty()) {
                logger.info("No portal urls in rule #{}", rule.id)
            }

            // the client controls the retry
            val portalTasks = pagedPortalUrls.map { it ->
                PortalTask(it, "-refresh", 3).also {
                    it.rule = rule
                    it.status = TaskStatus.CREATED
                }
            }

            crawlRuleRepository.save(rule)
            portalTaskRepository.saveAll(portalTasks)

            logger.debug("Created {} portal tasks", portalTasks.size)
            processingRules.put(rule.id!!, true)
            loadAndSubmitPortalTasks(parameterConcurrentTaskCnt)
        } catch (t: Throwable) {
            logger.warn(t.stringify())
        }
    }

    fun loadAndSubmitPortalTask(task: PortalTask) {
        task.startTime = Instant.now()
        task.status = TaskStatus.LOADED
        portalTaskRepository.save(task)
        scraper.scrapeOutPages(createListenablePortalTask(task, true))
    }

    @Synchronized
    fun loadAndSubmitPortalTasks(limit: Int) {
        val order = Sort.Order.asc("id")
        val pageRequest = PageRequest.of(0, limit, Sort.by(order))
        val portalTasks = portalTaskRepository.findAllByStatus(TaskStatus.CREATED, pageRequest)
        if (portalTasks.isEmpty) {
            return
        }

        portalTasks.forEach {
            it.startTime = Instant.now()
            it.status = TaskStatus.LOADED
        }
        portalTaskRepository.saveAll(portalTasks)

        portalTasks.shuffled()
            .asSequence().filter {
                if (it.rule!!.ipTypeWant == IpType.RESIDENCE.name && scrapeNodeService.getNodesByIpType(IpType.RESIDENCE)
                        .isEmpty()
                ) {
                    logger.warn("no RESIDENCE scrape node ready")
                    false
                }
                true
            }
            .map { createListenablePortalTask(it, true) }
            .forEach { task -> scraper.scrapeOutPages(task) }
    }

    fun submitRetryingScrapeTasks(limit: Int) {
        var n = limit
        while (n-- > 0) {
            retryingPortalTasks.poll()?.let {
                if (it.rule!!.ipTypeWant == IpType.RESIDENCE.name && scrapeNodeService.getNodesByIpType(IpType.RESIDENCE)
                        .isEmpty()
                ) {
                    logger.warn("no RESIDENCE scrape node ready")
                } else {
                    scraper.scrapeOutPages(createListenablePortalTask(it))
                }
            }
            // retryingItemTasks.poll()?.let { scraper.scrape(createListenableScrapeTask(it)) }
        }
    }

    fun createListenablePortalTask(portalTask: PortalTask, refresh: Boolean = false): ListenablePortalTask {
        return ListenablePortalTask(
            portalTask, refresh = refresh,

            onSubmitted = {
                val rule = portalTask.rule

                it.status = TaskStatus.SUBMITTED

                portalTask.serverTaskId = it.serverTaskId
                portalTask.status = TaskStatus.SUBMITTED
                portalTaskRepository.save(portalTask)
            },
            onRetry = {
                it.status = TaskStatus.RETRYING

                portalTask.status = TaskStatus.RETRYING
//                ++portalTask.retryCount
                portalTaskRepository.save(portalTask)

//                retryingPortalTasks.add(it)
                logger.info("Portal task is retrying #{} | {}", portalTask.id, portalTask.url)
            },
            onSuccess = {
                it.status = TaskStatus.OK

                portalTask.status = TaskStatus.OK
                portalTaskRepository.save(portalTask)
                processingRules.remove(portalTask.rule!!.id)

                val resultSet = it.response.resultSet
                if (resultSet.isNullOrEmpty()) {
                    logger.warn("No result set | {}", it.configuredUrl)
                } else {
                    var ids = resultSet[0]["ids"] as ArrayList<String>?
                    if (ids.isNullOrEmpty()) {
                        logger.warn("No ids in task #{} | {}", portalTask.id, it.configuredUrl)
                    } else {
                        val idsStr = ids.joinToString(",")
                        if (idsStr.length >= 1024) {
                            logger.error("ids too long, will not saved. {}, {}", it.url, idsStr)
                        }
                        val rule = portalTask.rule
                        rule!!.idsOfLast = idsStr
                        crawlRuleRepository.save(rule)
                        crawlRuleRepository.flush()
                    }
                }
            },
            onFailed = {
                it.status = TaskStatus.FAILED

                portalTask.status = TaskStatus.FAILED
                portalTaskRepository.save(portalTask)

                processingRules.remove(portalTask.rule!!.id)
                logger.info("Portal task is failed #{} | {}", portalTask.id, portalTask.url)
            },
            onFinished = {
//                logger.info("Portal task is finished #{} | {}", portalTask.id, portalTask.url)
                processingRules.remove(portalTask.rule!!.id)
            },
            onTimeout = {
                logger.info("Portal task is timeout #{} | {}", portalTask.id, portalTask.url)
            },

            onItemSubmitted = {
                it.status = TaskStatus.SUBMITTED

                ++portalTask.submittedCount
                portalTaskRepository.save(portalTask)
            },
            onItemRetry = {
                it.status = TaskStatus.RETRYING

                ++portalTask.retryCount
                portalTaskRepository.save(portalTask)

                logger.debug("Item task is retrying #{} {} | {}", portalTask.id, it.serverTaskId, it.url)

//                if (it.submitCount <= itemMaxSubmits) {
//                    it.status = TaskStatus.RETRYING
//                    retryingItemTasks.add(it)
//                } else {
//                    logger.info("Task if failed after {} tries | {}", itemMaxSubmits, it.url)
//                    it.status = TaskStatus.FAILED
//                }
            },
            onItemSuccess = {
                it.status = TaskStatus.OK
                ++portalTask.successCount
            },
            onItemFailed = {
                it.status = TaskStatus.FAILED
                ++portalTask.failedCount
                logger.info("Item task is failed #{} {} | {}", portalTask.id, it.serverTaskId, it.url)
            },
            onItemFinished = {
                ++portalTask.finishedCount
                portalTaskRepository.save(portalTask)
            },
            onItemTimeout = {
                logger.info("Item task is timeout #{} {} | {}", portalTask.id, it.serverTaskId, it.url)
            },
        )
    }

    private fun shouldRun0(rule: CrawlRule): Boolean {
        val lastCrawlTime = rule.lastCrawlTime
        if (rule.period.seconds > 0) {
            val now = Instant.now()
            if (lastCrawlTime + rule.period <= now) {
                return true
            }
        }

        val expression = rule.cronExpression
        if (expression.isNullOrBlank()) {
            return false
        }

        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
        val parser = CronParser(cronDefinition)
        val quartzCron: Cron = parser.parse(expression)
        quartzCron.validate()
        val executionTime = ExecutionTime.forCron(quartzCron)

        val zonedLastCrawlTime = lastCrawlTime.atZone(DateTimes.zoneId)
        val nextExecution = executionTime.nextExecution(zonedLastCrawlTime)
        // val timeToNextExecution = executionTime.timeToNextExecution(zonedLastCrawlTime) 
        // val timeToNextExecution = executionTime.timeToNextExecution(ZonedDateTime.now())
        // logger.warn("kcdebug. timeToNextExecution 0: {}, {}, {}, {}, {}, zonedLastCrawlTime:{}", timeToNextExecution.get().toSeconds(), rule.cronExpression, executionTime.toString(), DateTimes.zoneId, lastCrawlTime, zonedLastCrawlTime.toString())
        // logger.warn("kcdebug. timeToNextExecution: {}", timeToNextExecution)
        // if (timeToNextExecution.isPresent && timeToNextExecution.get().seconds <= 0) {
        if (nextExecution.isPresent && nextExecution.get() <= ZonedDateTime.now()) {
            return true
        }

        return false
    }

    private fun createPagedUrls(url: String, maxPages: Int): List<String> {
        return if (url.contains("{{page}}")) {
            IntRange(1, maxPages).map { pg -> url.replace("{{page}}", pg.toString()) }
        } else listOf(url)
    }
}
