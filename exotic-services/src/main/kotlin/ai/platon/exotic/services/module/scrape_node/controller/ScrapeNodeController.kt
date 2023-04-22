package ai.platon.exotic.services.module.scrape_node.controller

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
import ai.platon.pulsar.driver.scrape_node.entity.ScrapeNode
import ai.platon.pulsar.driver.scrape_node.services.ScrapeNodeService
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
    "api/crawl/scrape_node",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class ScrapeNodeController(
    private val repository: CrawlRuleRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
    @Autowired
    private val env: Environment,
    private val ruleService: RuleService
) {
    private val mongoTemplate = exoticCrawler.mongoTemplate
    private val scrapeNodeService = ScrapeNodeService.instance

    @PostConstruct
    fun init() {
    }


    @GetMapping("/")
    fun list(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
    ): ResponseEntity<OhJsonRespBody<List<ScrapeNode>>> {
        val results = scrapeNodeService.getAll()
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

}
