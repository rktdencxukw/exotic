package ai.platon.exotic.driver.crawl.entity

import java.time.Instant


data class ResultItem(
    var ruleId: Long,
    var portalTaskId: Long,
    var resultId: String,
    var title: String,
    var href: String,
    var content: String
) {
    var createdTime: Instant = Instant.now()
}
