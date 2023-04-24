package ai.platon.exotic.services.api.controller.api

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.driver.scrape_node.services.ScrapeNodeService
import ai.platon.pulsar.persist.metadata.IpType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors
import javax.annotation.PostConstruct
import javax.validation.Valid


@CrossOrigin(
    origins = ["*"],
    allowCredentials = "false",
    maxAge = -1,
    allowedHeaders = ["*"],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.HEAD]
)
@RestController
@RequestMapping(
    "api/crawl/rules",
//    consumes = [MediaType.TEXT_PLAIN_VALUE, "${MediaType.TEXT_PLAIN_VALUE};charset=UTF-8", MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class CrawlRuleController(
    private val repository: CrawlRuleRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
    @Autowired
    private val env: Environment,
    private val ohObjectMapper: ObjectMapper,
) {

    private val scrapeNodeService = ScrapeNodeService.instance

    private val amazonSeeds = LinkExtractors.fromResource("sites/amazon/best-sellers.txt")
    private val amazonItemSQLTemplate = ResourceLoader.readString("sites/amazon/sqls/x-item.sql").trim()
    private val sqlTemplate = """
select
  dom_all_attrs(dom, 'a.news-flash-item-title', 'href') as ids,
  dom_all_texts(dom, '.news-flash-item-title') as titles,
  dom_all_texts(dom, '.news-flash-item-content') as contents
from load_and_select('{{url}}', 'body');
    """

    lateinit var reportServer: String

    @PostConstruct
    fun init() {
        reportServer =
            "http://127.0.0.1:${env.getProperty("server.port")}${env.getProperty("server.servlet.context-path")}"
        println("kcdebug. Web server url: $reportServer")
    }

    @GetMapping("/tags")
    fun listTags(): ResponseEntity<OhJsonRespBody<List<String>>> {
        var tagsSet = HashSet<String>()
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        var page = 0
        while (true) {
            val pageable = PageRequest.of(page++, 100, sort, sortProperty)
            val rsp = repository.findAllTags(pageable)
            if (rsp.isEmpty) {
                break
            } else {
                for (s in rsp.content) {
                    s?.let {
                        val s2 = it.split(",")?.map { it -> it.trim() }
                        s2?.let{tagsSet.addAll(s2)}
                    }
                }
            }
        }
        return ResponseEntity.ok(OhJsonRespBody(tagsSet.toList()))
    }


    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 1,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
    ): ResponseEntity<OhJsonRespBody<Page<CrawlRule>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val rsp = repository.findAllByStatusNot(RuleStatus.Archived.toString(), pageable)
        return ResponseEntity.ok(OhJsonRespBody(rsp))
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: Long): ResponseEntity<OhJsonRespBody<CrawlRule>> {
        val rule = repository.getById(id)
        // TODO 分页
//        rule.portalTasks.sortedByDescending { it.id }
        return ResponseEntity.ok(OhJsonRespBody(rule))
    }

//    @GetMapping("/add")
//    fun showAddForm(model: Model): String {
//        val rule = CrawlRule()
//        rule.sqlTemplate = amazonItemSQLTemplate
//
//        val n = 2 + Random.nextInt(4)
//        rule.portalUrls = amazonSeeds.shuffled().take(n).joinToString("\n")
//        rule.outLinkSelector = "a[href~=/dp/]"
//        rule.nextPageSelector = "ul.a-pagination li.a-last a"
//
//        model.addAttribute("rule", rule)
//
//        return "crawl/rules/add"
//    }

//    @GetMapping("/add")
//    fun create(model: Model): String {
//        //        getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))
//        val rule = CrawlRule()
//        rule.id = 0L
//        rule.type = RuleType.Entity.toString()
//        rule.sqlTemplate = sqlTemplate
//        model.addAttribute("rule", rule)
//        model.addAttribute("id", 0L)
//        return "crawl/rules/edit"
//    }

//    @GetMapping("/jd/add")
//    fun showJdAddForm(model: Model): String {
//        val rule = CrawlRule()
//        model.addAttribute("rule", rule)
//        return "crawl/rules/jd/add"
//    }

    @PostMapping("/add")
    fun add(@Valid @RequestBody rule: CrawlRule, errors: Errors): ResponseEntity<OhJsonRespBody<String>> {
//        getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))

        if (errors.hasErrors()) {
            val msg = errors.allErrors
                .stream().map { x -> x.defaultMessage }
                .collect(Collectors.joining(","))
            return ResponseEntity.badRequest().body(OhJsonRespBody<String>().error(msg))
        }

        rule.createdDate = Instant.now()
        rule.status = RuleStatus.Created.toString()
        if (!rule.period.isNegative) { // 没用 cron
            rule.cronExpression = ""
        }
        if (rule.type == RuleType.Entity.toString()) {
            rule.outLinkSelector = ""
        }

        repository.save(rule)
        return ResponseEntity.ok().body(OhJsonRespBody.ok())
    }

    @PostMapping("/test_run")
    fun testRun(
        @Valid @RequestBody rule: CrawlRule,
        errors: Errors
    ): ResponseEntity<OhJsonRespBody<ScrapeTask?>> {
//        getLogger(this).info(prettyScentObjectWritter().writeValueAsString(rule))

        //If error, just return a 400 bad request, along with the error message
        if (errors.hasErrors()) {
            val msg = errors.allErrors
                .stream().map { x -> x.defaultMessage }
                .collect(Collectors.joining(","))
            return ResponseEntity.badRequest().body(OhJsonRespBody<ScrapeTask?>().error(msg))
        }
        // TODO not retry
        val args = if (rule.renderType == RenderType.Resource.toString()) {
            buildEntityWithoutBrowserArgs(rule, true, exoticCrawler.driverSettings.authToken)
        } else {
            buildPortalArgs(rule, true, exoticCrawler.driverSettings.authToken)
        }
        var portalTask = PortalTask(rule.portalUrls, args, 3)
        portalTask.rule = rule
        var scrapeTask = ScrapeTask(rule.portalUrls, args, 3, rule.sqlTemplate!!)
        scrapeTask.companionPortalTask = portalTask

        val listenableScrapeTask = ListenableScrapeTask(scrapeTask).also {
            it.onSubmitted = {
                it.task.status = TaskStatus.SUBMITTED
            }
            it.onRetry = {
                it.task.status = TaskStatus.RETRYING
            }
            it.onSuccess = {
                it.task.status = TaskStatus.OK
            }
            it.onFailed = {
                it.task.status = TaskStatus.FAILED
            }
            it.onFinished = {
                it.task.status = TaskStatus.OK
            }
            it.onTimeout = {
                it.task.status = TaskStatus.FAILED
            }
        }

        val taskSubmitter =
            TaskSubmitter(
                exoticCrawler.driverSettings,
                reportServer,
                mongoTemplate = exoticCrawler.mongoTemplate,
                simpMessagingTemplate = exoticCrawler.simpMessagingTemplate,
                ohObjectMapper = ohObjectMapper
            )
        taskSubmitter.scrape(listenableScrapeTask)

        val startTime = Instant.now()
        while (scrapeTask.status != TaskStatus.OK && scrapeTask.status != TaskStatus.FAILED) {
            if (Duration.between(startTime, Instant.now()).toSeconds() > 2 * 60) {
                return ResponseEntity.ok(OhJsonRespBody<ScrapeTask?>().error("timeout"))
            }
            // sleep 1s
            Thread.sleep(1000)

        }
        scrapeTask.companionPortalTask?.rule = null // 新建时由于没有rule会序列化失败。
        return ResponseEntity.ok(OhJsonRespBody(scrapeTask))
    }
    // FIXME 与别处重复，临时放置
    /**
     * -scrollCount 25: scroll down 25 extra times
     * */
    private fun buildPortalArgs(rule: CrawlRule, refresh: Boolean, token: String): String {
        var args = rule.buildArgs() + " -scrollCount 25"
        args += if (refresh) " -refresh" else ""
        args += " -authToken " + token
        return args
    }

    /**
     * -scrollCount 25: scroll down 25 extra times
     * */
    private fun buildItemArgs(rule: CrawlRule, portalRefresh: Boolean, token: String): String {
        var args = rule.buildArgs() + " -scrollCount 20"
        args += if (portalRefresh) " -expires 2h" else " -expires 3600d"
        args += " -authToken " + token
        return args
    }

    private fun buildEntityWithoutBrowserArgs(rule: CrawlRule, portalRefresh: Boolean, token: String): String {
        var args = rule.buildArgs() + " -resource"
        args += " -refresh"
        args += " -authToken " + token
        return args
    }

//    @GetMapping("/edit/{id}")
//    fun edit(@PathVariable("id") id: Long,
//             @Valid @RequestBody rule: CrawlRule,
//             errors: Errors
//             ): ResponseEntity<OhJsonRespBody<Any>>{
//        if (errors.hasErrors()) {
//            val msg = errors.allErrors
//                .stream().map { x -> x.defaultMessage }
//                .collect(Collectors.joining(","))
//            return ResponseEntity.badRequest().body(OhJsonRespBody.error(msg))
//        }
//        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
//        return ResponseEntity.ok(OhJsonRespBody.ok(rule))
//    }

    @PostMapping("set_status/{id}")
    fun setStatus(
        @PathVariable("id") id: Long, @Valid @RequestParam status: RuleStatus
    ): ResponseEntity<OhJsonRespBody<String>> {
        val old = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        old.status = status.toString()
        repository.save(old)
        return ResponseEntity.ok().body(OhJsonRespBody.ok())
    }

    @PostMapping("update/{id}")
    fun update(
        @PathVariable("id") id: Long, @Valid @RequestBody rule: CrawlRule,
        errors: Errors
    ): ResponseEntity<OhJsonRespBody<String>> {
        if (errors.hasErrors()) {
            val msg = errors.allErrors
                .stream().map { x -> x.defaultMessage }
                .collect(Collectors.joining(","))
            return ResponseEntity.badRequest().body(OhJsonRespBody<String>().error(msg))
        }
        if (id == 0L) {
            rule.createdDate = Instant.now()
            rule.status = RuleStatus.Created.toString()
        } else {
            val old = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
            rule.status = old.status
            rule.crawlCount = old.crawlCount
            rule.createdDate = old.createdDate
            rule.lastCrawlTime = old.lastCrawlTime
            rule.crawlHistory = old.crawlHistory
            rule.idsOfLast = old.idsOfLast
//            rule.type = old.type
        }

        if (!rule.period.isNegative) { // 没用 cron
            rule.cronExpression = ""
        }

        if (rule.type == RuleType.Entity.toString()) {
            rule.outLinkSelector = ""
        }

        val ruleT = repository.save(rule)

        return ResponseEntity.ok(OhJsonRespBody.ok())
    }

    @PostMapping("pause/{id}")
    fun pause(@PathVariable("id") id: Long): ResponseEntity<OhJsonRespBody<String>> {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

        rule.status = RuleStatus.Paused.toString()
        repository.save(rule)

        return ResponseEntity.ok(OhJsonRespBody.ok())
    }

    @PostMapping("start/{id}")
    fun start(@PathVariable("id") id: Long): ResponseEntity<OhJsonRespBody<String>> {
        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
        if (rule.ipTypeWant == IpType.RESIDENCE.name && scrapeNodeService.getAll().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(OhJsonRespBody<String>().error("规则${rule.id}的ipTypeWant为RESIDENCE，但是没有可用的节点，请先添加节点"))
        }

//        rule.status = RuleStatus.Created.toString()
//        rule.adjustFields()
//        repository.save(rule)

        crawlTaskRunner.startCrawl(rule)

        return ResponseEntity.ok(OhJsonRespBody.ok())
    }

//    @GetMapping("admin/")
//    fun adminList(
//        @RequestParam(defaultValue = "0") pageNumber: Int = 1,
//        @RequestParam(defaultValue = "500") pageSize: Int = 500,
//    ): String {
//        val sort = Sort.Direction.DESC
//        val sortProperty = "id"
//        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
//        val rules = repository.findAll(pageable)
//        model.addAttribute("rules", rules)
//        return "crawl/rules/admin/index"
//    }

//    @GetMapping("admin/archive/{id}")
//    fun adminArchive(@PathVariable("id") id: Long): String {
//        val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }

//        rule.status = RuleStatus.Archived.toString()
////        rule.adjustFields()
//        repository.save(rule)

//        return "redirect:/crawl/rules/admin/"
//    }

    @PostMapping("start_batch")
    fun startBatch(
        @RequestParam ids: List<Long> = listOf()
    ): ResponseEntity<OhJsonRespBody<String>> {
        var stringBuilder = StringBuilder()
        for (id in ids) {
            val rule = repository.findById(id).orElseThrow { IllegalArgumentException("Invalid rule Id: $id") }
            if (rule.ipTypeWant == IpType.RESIDENCE.name && scrapeNodeService.getAll().isEmpty()) {
                stringBuilder.append("规则${rule.id}的ipTypeWant为RESIDENCE，但是没有可用的节点，请先添加节点")
                continue
            }
            crawlTaskRunner.startCrawl(rule)
        }
        val msg = stringBuilder.toString()
        return if (msg.isNullOrEmpty()) {
            ResponseEntity.ok(OhJsonRespBody.ok())
        } else {
            ResponseEntity.badRequest().body(OhJsonRespBody<String>().error(msg))
        }
    }
}
