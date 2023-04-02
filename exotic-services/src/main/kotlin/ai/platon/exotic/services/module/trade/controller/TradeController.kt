package ai.platon.exotic.services.module.trade.controller


import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.module.trade.entity.Account
import ai.platon.exotic.services.module.trade.entity.Balance
import ai.platon.exotic.services.module.trade.entity.PendingOrder
import ai.platon.exotic.services.module.trade.entity.Position
import ai.platon.exotic.services.module.trade.persist.AccountRepository
import ai.platon.exotic.services.module.trade.persist.BalanceRepository
import ai.platon.exotic.services.module.trade.persist.PendingOrderRepository
import ai.platon.exotic.services.module.trade.persist.PositionRepository
import ai.platon.exotic.services.module.trade.service.TradeService
import org.openapitools.client.models.AssetDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import kotlin.streams.toList


@CrossOrigin
@RestController
@RequestMapping(
    "api/trade",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class TradeController(
    private val accountRepository: AccountRepository,
    private val balanceRepository: BalanceRepository,
    private val pendingOrderRepository: PendingOrderRepository,
    private val positionRepository: PositionRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
    @Autowired
    private val env: Environment,
    private val tradeService: TradeService
) {

    @PostConstruct
    fun init() {
    }

    @GetMapping("/accounts")
    fun listAccounts(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
    ): ResponseEntity<OhJsonRespBody<Page<Account>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = accountRepository.findAll(pageable)
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/balances")
    fun listBalances(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "0") accountId: Int = 0,
    ): ResponseEntity<OhJsonRespBody<Page<Balance>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "lastModifiedDate"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (accountId == 0) {
            balanceRepository.findAll(pageable)
        } else {
            balanceRepository.findAllByAccountId(accountId, pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/pending_orders")
    fun listPendingOrders(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "0") accountId: Int = 0,
    ): ResponseEntity<OhJsonRespBody<Page<PendingOrder>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (accountId == 0) {
            pendingOrderRepository.findAll(pageable)
        } else {
            pendingOrderRepository.findAllByAccountId(accountId, pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @GetMapping("/positions")
    fun listPositions(
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
        @RequestParam(defaultValue = "0") accountId: Int = 0,
    ): ResponseEntity<OhJsonRespBody<Page<Position>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (accountId == 0) {
            positionRepository.findAll(pageable)
        } else {
            positionRepository.findAllByAccountId(accountId, pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @PostMapping("/fetch_balances")
    fun fetchBalances(
        @RequestParam(defaultValue = "all") accounts: String = "all", // ,分割的账户
    ): ResponseEntity<OhJsonRespBody<List<Pair<String, String>>>> {
        var accountList: List<Long> = if (accounts == "all") {
            accountRepository.findAllByEnable(1).stream().map { it.accountId }.toList()
        } else {
            accounts.split(",").map { it.toLong() }
        }
        var errMsgList = mutableListOf<Pair<String, String>>()
        for (accountId in accountList) {
            val res = tradeService.getBalance(accountId)
            if (res.ec != 0) {
                errMsgList.add(Pair(accountId.toString(), res.errmsg))
            } else {
                val balanceDto = res.v!!
                for (b in balanceDto) {
                    var balance: Balance = convertDtoToEntity(b)
                    balance.accountId = accountId.toInt()
                    balanceRepository.save(balance)
                }
            }
        }
        return if (errMsgList.isEmpty()) {
            ResponseEntity.ok(OhJsonRespBody())
        } else {
            return ResponseEntity.badRequest()
                .body(OhJsonRespBody<List<Pair<String, String>>>().error("failed", errMsgList))
        }
    }

    private fun convertDtoToEntity(dto: AssetDto): Balance {
        var b = Balance()
        b.asset = dto.asset!!
        b.free = dto.free!!
        b.frozen = dto.frozen!!
        b.total = dto.total!!
        return b
    }

}
