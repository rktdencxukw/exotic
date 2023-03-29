package ai.platon.exotic.services.api.controller.api

//import ai.platon.exotic.services.common.jackson.scentObjectMapper
import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.api.entity.api.ExpandedScrapeResponse
import ai.platon.pulsar.driver.ScrapeResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("api/crawl/remote/tasks",
//    consumes = [MediaType.TEXT_PLAIN_VALUE, "${MediaType.TEXT_PLAIN_VALUE};charset=UTF-8", MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
    )
class RemoteTaskController(
    private val exoticCrawler: ExoticCrawler,
) {
    private val driver get() = exoticCrawler.driver

    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "500") pageSize: Int = 500,
        @RequestParam(defaultValue = "desc") direction: String = "desc",
    ): ResponseEntity<List<ExpandedScrapeResponse>> {
        throw UnsupportedOperationException("Not implemented yet")
        val ascPageNumber = if (direction == "desc") {
            val count = driver.count()
            val totalPageNumber = 1 + count / pageSize
            totalPageNumber - pageNumber - 1
        } else pageNumber

        val pageable = PageRequest.of(ascPageNumber.toInt(), pageSize)
        val tasks: List<ExpandedScrapeResponse> = driver.fetch(pageable.offset, pageable.pageSize)
            .map { ExpandedScrapeResponse(it) }
            .sortedByDescending { it.timestamp }
        return ResponseEntity.ok().body(tasks)
    }

//    @GetMapping(
//        "/download",
//        produces = [MediaType.APPLICATION_JSON_VALUE]
//    )
//    fun download(
//        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
//        @RequestParam(defaultValue = "500") pageSize: Int = 500,
//        @RequestParam(defaultValue = "desc") direction: String = "desc",
//    ): String {
//        val ascPageNumber = if (direction == "desc") {
//            val count = driver.count()
//            val totalPageNumber = 1 + count / pageSize
//            totalPageNumber - pageNumber - 1
//        } else pageNumber

//        val pageable = PageRequest.of(ascPageNumber.toInt(), pageSize)
//        val tasks: List<ExpandedScrapeResponse> = driver.fetch(pageable.offset, pageable.pageSize)
//            .map { ExpandedScrapeResponse(it) }
//            .sortedByDescending { it.timestamp }

//        return scentObjectMapper().writeValueAsString(tasks)
//    }

    @GetMapping("/view/{id}")
    fun view(@PathVariable id: String): ResponseEntity<OhJsonRespBody<ScrapeResponse>> {
        val task = driver.findById(id)
        return if (task != null){
            ResponseEntity.ok(OhJsonRespBody(task))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
