package ai.platon.exotic.services.module.portal_task.dto

import ai.platon.exotic.driver.crawl.entity.PortalTask

class PortalTaskExtDto : PortalTask  {
    var ruleName: String
    var ruleId: Long

    constructor(p:PortalTask) : super(p.url, p.args, p.priority) {
        this.ruleId = p.rule!!.id!!
        this.ruleName = p.rule!!.name

        this.id = p.id
        this.serverTaskId = p.serverTaskId
        this.submittedCount = p.submittedCount
        this.successCount = p.successCount
        this.resultCount = p.resultCount
        this.deltaResultCount = p.deltaResultCount
        this.retryCount = p.retryCount
        this.failedCount = p.failedCount
        this.finishedCount = p.finishedCount
        this.startTime = p.startTime
        this.status = p.status
        this.createdDate = p.createdDate
        this.lastModifiedDate = p.lastModifiedDate
    }
}