package ai.platon.exotic.services.api.controller.api

import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.api.persist.PortalTaskRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(
    "api/crawl/portal-tasks",
//    consumes = [MediaType.TEXT_PLAIN_VALUE, "${MediaType.TEXT_PLAIN_VALUE};charset=UTF-8", MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class PortalTaskController(
    private val crawlRuleRepository: CrawlRuleRepository,
    private val repository: PortalTaskRepository
) {
    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        @RequestParam(defaultValue = "0") ruleId: Long = 0,
    ): ResponseEntity<OhJsonRespBody<Page<PortalTask>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val rsp: Page<PortalTask> = if (ruleId > 0) {
            val rule = crawlRuleRepository.getById(ruleId) ?: return ResponseEntity.badRequest()
                .body(OhJsonRespBody<Page<PortalTask>>().error("Rule not found: $ruleId"))
            repository.findAllByRule(rule, pageable)
        } else {
            repository.findAll(pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(rsp))
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: Long): ResponseEntity<OhJsonRespBody<PortalTask>> {
        val rsp = repository.getById(id)
        return ResponseEntity.ok(OhJsonRespBody(rsp))
    }
}
