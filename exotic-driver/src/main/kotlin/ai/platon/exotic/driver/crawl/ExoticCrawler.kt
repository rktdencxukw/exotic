package ai.platon.exotic.driver.crawl

import ai.platon.exotic.driver.common.IS_DEVELOPMENT
import ai.platon.exotic.driver.crawl.entity.ItemDetail
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.driver.DriverSettings
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class ExoticCrawler(
    val env: Environment? = null,
    val mongoTemplate: MongoTemplate,
    simpMessagingTemplate: SimpMessagingTemplate? = null,
    ohObjectMapper: ObjectMapper ? = null,
): AutoCloseable {
    private val logger = LoggerFactory.getLogger(ExoticCrawler::class.java)

    val scrapeServer: String
        get() = env?.getProperty("scrape.server")
            ?: System.getProperty("scrape.server")
            ?: "localhost"
    val scrapeServerPort: Int
        get() = env?.getProperty("scrape.server.port")?.toIntOrNull()
            ?: System.getProperty("scrape.server.port")?.toIntOrNull()
            ?: 8182
    val scrapeServerContextPath: String
        get() = env?.getProperty("scrape.server.servlet.context-path")
            ?: System.getProperty("scrape.server.servlet.context-path")
            ?: "/api"
    val authToken: String
        get() = env?.getProperty("scrape.authToken")
            ?: System.getProperty("scrape.authToken")
            ?: "b06test42c13cb000f74539b20be9550b8a1a90b9"

    private val reportServer = env!!.getProperty("report.server.uri", "http://127.0.0.1:${env!!.getProperty("server.port")}${env!!.getProperty("server.servlet.context-path")}")
    // TODO 获取局域网地址
//    private val reportServer = "http://192.168.68.137:${env!!.getProperty("server.port")}${env!!.getProperty("server.servlet.context-path")}"

    init {
        println("reportServer: $reportServer")
    }

    val driverSettings get() = DriverSettings(
        scrapeServer,
        authToken,
        scrapeServerPort,
        scrapeServerContextPath
    )

    val outPageScraper = OutPageScraper(driverSettings, reportServer, mongoTemplate, simpMessagingTemplate, ohObjectMapper)

    val driver get() = outPageScraper.taskSubmitter.driver

    val pendingPortalTasks: Deque<ListenablePortalTask> = ConcurrentLinkedDeque()

    val pendingItems = ConcurrentLinkedQueue<ItemDetail>()

    var maxPendingTaskCount = if (IS_DEVELOPMENT) 10 else 50

    init {
        Params.of(
            "scrapeServer", scrapeServer,
            "scrapeServerPort", scrapeServerPort,
            "scrapeServerContextPath", scrapeServerContextPath
        ).withLogger(logger).debug()
    }

    fun crawl() {
        val taskSubmitter = outPageScraper.taskSubmitter
        val submittedTaskCount = taskSubmitter.pendingTaskCount

        if (submittedTaskCount >= maxPendingTaskCount) {
            return
        }

        val n = (maxPendingTaskCount - submittedTaskCount).coerceAtMost(10)
        if (pendingPortalTasks.isNotEmpty()) {
            scrapeFromQueue(pendingPortalTasks, n)
        }
    }

    @Throws(Exception::class)
    fun scrape(task: ListenableScrapeTask) {
        try {
//            task.onItemSuccess = {
//                createPendingItems(it)
//            }
            outPageScraper.scrape(task)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    @Throws(Exception::class)
    fun scrapeOutPages(task: ListenablePortalTask) {
        try {
//            task.onItemSuccess = {
//                createPendingItems(it)
//            }
            if (task.task.rule!!.type == RuleType.Entity.toString()) {
                outPageScraper.scrapeEntity(task)
            } else {
                outPageScraper.scrape(task)
            }
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    override fun close() {
        outPageScraper.close()
    }

    private fun scrapeFromQueue(queue: Queue<ListenablePortalTask>, n: Int) {
        var n0 = n
        while (n0-- > 0) {
            val task = queue.poll()
            if (task != null) {
                scrapeOutPages(task)
            }
        }
    }

    private fun createPendingItems(task: ScrapeTask) {
        val allowDuplicate = task.companionPortalTask?.rule != null
        task.response.resultSet
            ?.filter { it.isNotEmpty() }
            ?.map { ItemDetail.create(it["uri"].toString(), it, allowDuplicate) }
            ?.toCollection(pendingItems)
    }
}

fun main() {
    lateinit var mongoTemplate : MongoTemplate

    val scraper = ExoticCrawler(
        mongoTemplate = mongoTemplate,
    )
    scraper.crawl()

    readLine()
}
