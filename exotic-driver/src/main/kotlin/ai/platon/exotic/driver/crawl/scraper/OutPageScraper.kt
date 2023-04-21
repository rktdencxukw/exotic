package ai.platon.exotic.driver.crawl.scraper

import ai.platon.exotic.driver.common.DEV_MAX_OUT_PAGES
import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.common.PRODUCT_MAX_OUT_PAGES
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.entity.ResultItem
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.driver.DriverSettings
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList

class Markdown(val content: String) {
}

class WeChatMarkdownMsg(val title: String, val content: String) {
    @SerializedName("msgtype")
    val msgType = "markdown"
    lateinit var markdown: Markdown

    init {
        val content = """
           ${DateTimes.now()}\n
           title: ${title}\n\n
           content: $content
       """
        markdown = Markdown(content)
    }
}

open class OutPageScraper(
    private val driverSettings: DriverSettings,
    private val reportServer: String,
    private val mongoTemplate: MongoTemplate,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val ohObjectMapper: ObjectMapper? = null,
) : AutoCloseable {
    var logger: Logger = LoggerFactory.getLogger(OutPageScraper::class.java)

    val httpTimeout: Duration = Duration.ofMinutes(3)
    val hc = HttpClient.newHttpClient()

    val taskSubmitter: TaskSubmitter = TaskSubmitter(driverSettings, reportServer, true, mongoTemplate, simpMessagingTemplate)

    fun scrape(sql: String) {
        val a = """
            select
                dom_uri(dom) as uri,
                dom_first_text(dom, '.itemInfo-wrap .sku-name') as product_title
            from
                load_out_pages(
                    'https://list.jd.com/list.html?cat=670,671,672&page=1 -taskId r2 -refresh',
                    '#J_goodsList li[data-sku] a[href~=item]'
                )
        """.trimIndent()

        val (prefix, part2) = sql.split("load_out_pages")
        val (portalUrl, outLinkSelector) = part2
            .substringAfterLast("(")
            .substringBeforeLast(")")
            .split(",")

        val portalSQLTemplate = """
            select
                dom_all_hrefs(dom, '$outLinkSelector') as hrefs
            from
                load_and_select('{{url}}', 'body');
        """.trimIndent()
        val itemSQLTemplate = "$prefix load_and_select('{{url}}', 'body')"
    }

    fun scrape(task: ListenableScrapeTask) {
        taskSubmitter.scrape(task)
    }

    fun scrapeEntity(listenablePortalTask: ListenablePortalTask) {
        val task = listenablePortalTask.task
        val rule = task.rule
        if (rule == null) {
            logger.info("No rule for task {}", task.id)
            return
        }

        val sqlTemplate = rule.sqlTemplate?.trim()
        val args = if (rule.renderType == RenderType.Resource.toString()) {
            buildEntityWithoutBrowserArgs(rule, listenablePortalTask.refresh)
        } else {
            buildPortalArgs(rule, listenablePortalTask.refresh)
        }
        val priority = task.priority

        val scrapeTask = ScrapeTask(task.url, args, priority, sqlTemplate!!)
        scrapeTask.companionPortalTask = task

        val listenableScrapeTask = ListenableScrapeTask(scrapeTask).also { it ->
            it.task.companionPortalTask = task
            it.onSubmitted = { listenablePortalTask.onSubmitted(it.task) }
            it.onRetry = { listenablePortalTask.onRetry(it.task) }
            it.onSuccess = {

                val resultSet = it.task.response.resultSet
                if (resultSet.isNullOrEmpty()) {
                    logger.warn("No result set | {}", it.task.configuredUrl)
                    var msg = WeChatMarkdownMsg("No result set", it.task.configuredUrl)
                    var gson = Gson()
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=5932e314-7ffe-47bd-a097-87e9a39af354"))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(gson.toJson(msg))).build()
                    hc.send(request, BodyHandlers.ofString())
                } else {
                    var ids = resultSet[0]["ids"] as? ArrayList<String>
                    if (ids.isNullOrEmpty()) {
                        logger.warn("No ids in task #{} | {}", task.id, it.task.configuredUrl)
                        var msg = WeChatMarkdownMsg("No ids in task #{} | ${task.id}", it.task.configuredUrl)
                        var gson = Gson()
                        val request = HttpRequest.newBuilder()
                            .uri(URI.create("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=5932e314-7ffe-47bd-a097-87e9a39af354"))
                            .header("Content-Type", "application/json")
                            .POST(BodyPublishers.ofString(gson.toJson(msg))).build()
                        hc.send(request, BodyHandlers.ofString())
                    } else {
                        var titles = resultSet[0]["titles"] as ArrayList<String>
                        var contents = resultSet[0]["contents"] as? ArrayList<String>
                        var hrefs = resultSet[0]["hrefs"] as? ArrayList<String>
                        val oldSet: Set<String> = rule.idsOfLast.split(",").map { it.trim() }.toSet()
                        var deltaResultCount = 0
                        for (i in ids.indices) {
                            if (oldSet.contains(ids[i])) {
                                break
                            }
                            ++deltaResultCount
                            var content: String = (contents?.get(i)) ?: ""
                            var href: String = (hrefs?.get(i)) ?: ""
                            if (href.isNotEmpty()) {
                                // get host from url
                                var url = URL(task.url);
                                val host = url.host;
                                href = "$host/$href"
                            }
                            val resultItem = ResultItem(task.rule!!.id!!, task.id!!, ids[i], titles[i], href, content)
                            mongoTemplate.save(resultItem)
                            // TODO 通知界面端 websocket
                            simpMessagingTemplate!!.convertAndSend(
                                "/topic/result_feed",
                                ohObjectMapper!!.writeValueAsString(resultItem)
                            )
//                            var msg = WeChatMarkdownMsg(titles[i], content)
//                            var gson = Gson()
//                            val request = HttpRequest.newBuilder()
//                                .uri(URI.create("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=5932e314-7ffe-47bd-a097-87e9a39af354"))
//                                .header("Content-Type", "application/json")
//                                .POST(BodyPublishers.ofString(gson.toJson(msg))).build()
//                            hc.send(request, BodyHandlers.ofString())
                            // val response = hc.send(request, BodyHandlers.ofString()).body()
                        }
                        it.task.companionPortalTask!!.deltaResultCount = deltaResultCount
                        it.task.companionPortalTask!!.resultCount = ids.size
                    }
                }

                // 会更改rules ids_of_last， 放到最后
                listenablePortalTask.onSuccess(it.task)
            }
            it.onFailed = { listenablePortalTask.onFailed(it.task) }
            it.onFinished = { listenablePortalTask.onFinished(it.task) }
            it.onTimeout = { listenablePortalTask.onTimeout(it.task) }
        }

        taskSubmitter.scrape(listenableScrapeTask)
    }

    fun scrape(listenablePortalTask: ListenablePortalTask) {
        val task = listenablePortalTask.task
        val rule = task.rule
        if (rule == null) {
            logger.info("No rule for task {}", task.id)
            return
        }

        val outLinkSelector = rule.outLinkSelector
        if (outLinkSelector == null) {
            logger.info("No out link selector for task {}", task.id)
            return
        }

        val args = buildPortalArgs(rule, listenablePortalTask.refresh)
        val priority = task.priority

        val portalSQLTemplate = """
            select
                dom_all_hrefs(dom, '$outLinkSelector') as hrefs
            from
                load_and_select('{{url}}', 'body');
        """.trimIndent()
        val scrapeTask = ScrapeTask(task.url, args, priority, portalSQLTemplate)
        scrapeTask.companionPortalTask = task

        val listenableScrapeTask = ListenableScrapeTask(scrapeTask).also {
            it.task.companionPortalTask = task
            it.onSubmitted = { listenablePortalTask.onSubmitted(it.task) }
            it.onRetry = { listenablePortalTask.onRetry(it.task) }
            it.onSuccess = {
                listenablePortalTask.onSuccess(it.task)
                createChildTasks(listenablePortalTask, it)
            }
            it.onFailed = { listenablePortalTask.onFailed(it.task) }
            it.onFinished = { listenablePortalTask.onFinished(it.task) }
            it.onTimeout = { listenablePortalTask.onTimeout(it.task) }
        }

        taskSubmitter.scrape(listenableScrapeTask)
    }

    override fun close() {
        taskSubmitter.close()
    }

    private fun createChildTasks(
        listenablePortalTask: ListenablePortalTask,
        scrapeTask: ListenableScrapeTask
    ) {
        val portalTask = scrapeTask.task.companionPortalTask ?: return
        val rule = portalTask.rule ?: return

        val sqlTemplate = rule.sqlTemplate?.trim()
        if (sqlTemplate.isNullOrBlank()) {
            logger.warn("No SQL template in rule {}", rule.id)
            return
        }

        val urls = createOutLinks(portalTask, scrapeTask)
        val args = buildItemArgs(rule, portalTask.args.contains("-refresh"))
        val tasks = createChildTasks(listenablePortalTask, urls, sqlTemplate, args)

        taskSubmitter.scrapeAll(tasks)
    }

    private fun createOutLinks(portalTask: PortalTask, scrapeTask: ListenableScrapeTask): List<String> {
        val resultSet = scrapeTask.task.response.resultSet
        if (resultSet == null || resultSet.isEmpty()) {
            logger.info("No result set | {}", scrapeTask.task.configuredUrl)
            return listOf()
        }

        val outLinkSelector = portalTask.rule?.outLinkSelector
        var hrefs = resultSet[0]["hrefs"]?.toString()
        if (hrefs.isNullOrBlank()) {
            logger.info("No hrefs in task #{} | {}", portalTask.id, scrapeTask.task.configuredUrl)
            return listOf()
        }

        val maxOutPages = if (IS_DEVELOPMENT) DEV_MAX_OUT_PAGES else PRODUCT_MAX_OUT_PAGES
        hrefs = hrefs.removePrefix("(").removeSuffix(")")

        // TODO: normalization
        val urls = hrefs.split(",").asSequence()
            .filter { UrlUtils.isValidUrl(it) }
            .map { it.substringBeforeLast("#") }
            .map { it.trim() }
            .take(maxOutPages)
            .toList()

        if (urls.isEmpty()) {
            logger.info(
                "No out links in task #{} | <{}> | {}",
                portalTask.id, outLinkSelector, scrapeTask.task.configuredUrl
            )
        }

        return urls
    }

    private fun createChildTasks(
        listenablePortalTask: ListenablePortalTask,
        urls: List<String>,
        sqlTemplate: String,
        args: String
    ): List<ListenableScrapeTask> {
        val priority = listenablePortalTask.task.priority

        return urls.map { ScrapeTask(it, args, priority, sqlTemplate) }
            .map { createListenableScrapeTask(listenablePortalTask, it) }
    }

    private fun createListenableScrapeTask(
        listenablePortalTask: ListenablePortalTask,
        task: ScrapeTask,
    ): ListenableScrapeTask {
        return ListenableScrapeTask(task).also {
            it.onSubmitted = { listenablePortalTask.onItemSubmitted(it.task) }
            it.onRetry = { listenablePortalTask.onItemRetry(it.task) }
            it.onSuccess = { listenablePortalTask.onItemSuccess(it.task) }
            it.onFailed = { listenablePortalTask.onItemFailed(it.task) }
            it.onFinished = { listenablePortalTask.onItemFinished(it.task) }
            it.onTimeout = { listenablePortalTask.onItemTimeout(it.task) }
        }
    }

    /**
     * -scrollCount 25: scroll down 25 extra times
     * */
    private fun buildPortalArgs(rule: CrawlRule, refresh: Boolean): String {
        var args = rule.buildArgs() + " -scrollCount 25"
        args += if (refresh) " -refresh" else ""
        args += " -authToken " + driverSettings.authToken
        return args
    }

    /**
     * -scrollCount 25: scroll down 25 extra times
     * */
    private fun buildItemArgs(rule: CrawlRule, portalRefresh: Boolean): String {
        var args = rule.buildArgs() + " -scrollCount 20"
        args += if (portalRefresh) " -expires 2h" else " -expires 3600d"
        args += " -authToken " + driverSettings.authToken
        return args
    }

    // kcread 按 LoadOptions 格式组装参数，稍后会加入到 sql 向pulsar提交
    private fun buildEntityWithoutBrowserArgs(rule: CrawlRule, portalRefresh: Boolean): String {
        var args = rule.buildArgs() + " -resource"
        args += " -refresh"
        args += " -authToken " + driverSettings.authToken
        return args
    }
}
