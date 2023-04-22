package ai.platon.exotic.services.module.market.controller

import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.module.market.entity.Derivate
import ai.platon.exotic.services.module.market.entity.DerivateExchange
import ai.platon.exotic.services.module.market.entity.Spot
import ai.platon.exotic.services.module.market.entity.SpotExchange
import ai.platon.exotic.services.module.market.persist.DerivateExchangeRepository
import ai.platon.exotic.services.module.market.persist.DerivateRepository
import ai.platon.exotic.services.module.market.persist.SpotExchangeRepository
import ai.platon.exotic.services.module.market.persist.SpotRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.domain.Page
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
    "api/market",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class MarketController(
    private val derivateRepository: DerivateRepository,
    private val derivateExchangeRepository: DerivateExchangeRepository,
    private val spotRepository: SpotRepository,
    private val spotExchangeRepository: SpotExchangeRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
    @Autowired
    private val env: Environment
) {

    @PostConstruct
    fun init() {
    }


    @GetMapping("/derivates")
    fun queryDerivates(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "") prefix: String = "",
    ): ResponseEntity<OhJsonRespBody<Page<Derivate>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "h24Volume"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (prefix.isNullOrEmpty()) {derivateRepository.findAll(pageable)} else{ derivateRepository.findAllByBaseLike("$prefix%", pageable)}
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/spots")
    fun querySpots(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "") prefix: String = "",
    ): ResponseEntity<OhJsonRespBody<Page<Spot>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "volume"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = spotRepository.findAllByBaseLike("$prefix%", pageable)
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/spot_exchanges")
    fun listSpotExchanges(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "") kw: String = "",
    ): ResponseEntity<OhJsonRespBody<Page<SpotExchange>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "tradeVolume24hBTC"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (kw.isNullOrEmpty()) {
            spotExchangeRepository.findAll(pageable)
        } else {
            spotExchangeRepository.findAllByNameLike("%$kw%", pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/derivate_exchanges")
    fun listDerivateExchanges(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "") kw: String = "",
    ): ResponseEntity<OhJsonRespBody<Page<DerivateExchange>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "tradeVolume24hBTC"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (kw.isNullOrEmpty()) {
            derivateExchangeRepository.findAll(pageable)
        } else {
            derivateExchangeRepository.findAllByNameLike("%$kw%", pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

}
