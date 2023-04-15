package ai.platon.exotic.services.api.controller.api

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.entity.ResultItem
import ai.platon.exotic.driver.crawl.entity.ResultItemDto
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.api.persist.CrawlRuleRepository
import ai.platon.exotic.services.api.service.RuleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors
import javax.annotation.PostConstruct
import javax.validation.Valid


@CrossOrigin
@RestController
@RequestMapping(
    "api/crawl/results",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ScrapeResultController(
    private val repository: CrawlRuleRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
    @Autowired
    private val env: Environment,
    private val ruleService: RuleService
) {
    private val mongoTemplate = exoticCrawler.mongoTemplate

    @PostConstruct
    fun init() {
    }


    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
    ): ResponseEntity<OhJsonRespBody<List<ResultItemDto>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "createdTime"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        var query = Query()
//        query.addCriteria(Criteria.where("ruleId").`is`(id))
        query.with(pageable)
        val results = mongoTemplate.find(query, ResultItem::class.java).map { var r = ResultItemDto(it)
            val rule = ruleService.getRule(r.ruleId)
            if (rule != null) {
                r.ruleName = rule.name
            }
            r
        }.toList()
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/of_rule/{ruleId}")
    fun listResultsOfRule(@PathVariable ruleId: Long,
                         @RequestParam(defaultValue = "0") pageNumber: Int = 0,
                         @RequestParam(defaultValue = "20") pageSize: Int = 20,
    ): ResponseEntity<OhJsonRespBody<MutableList<ResultItem>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "createdTime"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        var query = Query()
        query.addCriteria(Criteria.where("ruleId").`is`(ruleId))
        query.with(pageable)
        val results = mongoTemplate.find(query, ResultItem::class.java)
        return ResponseEntity.ok(OhJsonRespBody(results))
    }
}
