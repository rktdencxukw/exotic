package ai.platon.exotic.examples.sites.food.dianping

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.common.url.ParsableHyperlink
import ai.platon.pulsar.dom.select.selectHyperlinks
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.session.PulsarSession
import ai.platon.scent.context.ScentContexts
import org.jsoup.nodes.Document
import java.time.Duration

class DianpingCrawler(private val session: PulsarSession = ScentContexts.createSession()) {
    private val context = session.context as AbstractPulsarContext

    private val rpa = RestaurantRPA(session)

    private val retryDelayPolicy = { nextRetryNumber: Int, _: UrlAware? ->
        if (nextRetryNumber <= 2) {
            Duration.ofSeconds(10)
        } else {
            val minutes = nextRetryNumber.coerceAtMost(3).toLong()
            Duration.ofMinutes(minutes)
        }
    }

    private val parseHandler = { _: WebPage, document: Document -> }

    init {
        context.crawlLoops.loops.forEach {
            it.crawler.retryDelayPolicy = retryDelayPolicy
        }
    }

    fun runDefault() {
        val args = "-i 1s -ol \"#shop-all-list .tit a[href~=shop]\" -parse -ignoreFailure"
        val portalUrls = ResourceLoader.readAllLines("portal.urls.txt")
            .filter { UrlUtils.isValidUrl(it) }
            .shuffled()
        crawl(portalUrls, args)
    }

    fun crawl(portalUrls: List<String>, args: String) {
        portalUrls.forEach { portalUrl -> scrapeOutPages(portalUrl, args) }
        context.await()
    }

    fun scrapeOutPages(portalUrl: String, args: String) {
        val options = rpa.options(args)

        val document = session.loadDocument(portalUrl, options)

        val links = document.document.selectHyperlinks(options.outLinkSelector)
            .asSequence()
            .take(10000)
            .distinct()
            .map { ParsableHyperlink("$it -requireSize 300000 -ignoreFailure", parseHandler) }
            .onEach {
                it.referer = portalUrl
                it.event.chain(options.itemEvent)
            }
            .toList()
            .shuffled()

        context.submitAll(links)
    }
}

fun main(args: Array<String>) {
//    BrowserSettings.headless()

    val context = ScentContexts.create()
    val session = context.createSession()

    val loadArgs = "-i 1s -ol \"#shop-all-list .tit a[href~=shop]\" -parse -ignoreFailure"
    val portalUrls = ResourceLoader.readAllLines("portal.urls.txt")
        .filter { UrlUtils.isValidUrl(it) }
        .shuffled()
    DianpingCrawler(session).crawl(portalUrls, loadArgs)
}
