package ai.platon.exotic.services.api.controller.web

import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.services.api.persist.PortalTaskRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@CrossOrigin
@Controller
@RequestMapping("crawl/portal-tasks",
//    consumes = [MediaType.TEXT_PLAIN_VALUE, "${MediaType.TEXT_PLAIN_VALUE};charset=UTF-8", MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
    )
class PortalTaskWebController(
    private val repository: PortalTaskRepository
) {
    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
    ): ResponseEntity<PageRequest> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        return ResponseEntity.ok().body(PageRequest.of(pageNumber, pageSize, sort, sortProperty))
    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: Long): ResponseEntity<PortalTask> {
        val rsp =  repository.getById(id)
        return ResponseEntity.ok().body(rsp)
    }
}
