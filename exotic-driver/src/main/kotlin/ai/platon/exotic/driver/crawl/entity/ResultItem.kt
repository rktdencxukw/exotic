package ai.platon.exotic.driver.crawl.entity

import java.time.Instant


open class ResultItem(
    var ruleId: Long,
    var portalTaskId: Long,
    var resultId: String,
    var title: String,
    var href: String?,
    var content: String
) {
    var createdTime: Instant = Instant.now()
}

class ResultItemDto: ResultItem {
    constructor(resultItem: ResultItem): super(
        resultItem.ruleId,
        resultItem.portalTaskId,
        resultItem.resultId,
        resultItem.title,
        resultItem.href,
        resultItem.content
    ) {
        this.createdTime = resultItem.createdTime
    }
    var ruleName: String = ""
}