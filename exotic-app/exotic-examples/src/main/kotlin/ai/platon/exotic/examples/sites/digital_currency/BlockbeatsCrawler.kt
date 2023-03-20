package ai.platon.exotic.examples.sites.digital_currency

import ai.platon.pulsar.ql.context.SQLContexts

fun main() {
    val portalUrl = "https://www.theblockbeats.info/newsflash"
    val args = "a[href^='/flash'] -ignoreFailure"

    val fields = SQLContexts.createSession().scrapeOutPages(portalUrl, args, "body", listOf("div.flash-title", "div.flash-content"))
    println(fields.joinToString("\n"))
}
