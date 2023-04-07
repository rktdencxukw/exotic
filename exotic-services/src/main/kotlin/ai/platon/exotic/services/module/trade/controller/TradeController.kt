package ai.platon.exotic.services.module.trade.controller


import ai.platon.exotic.driver.crawl.ExoticCrawler
import ai.platon.exotic.driver.crawl.scraper.*
import ai.platon.exotic.services.api.component.CrawlTaskRunner
import ai.platon.exotic.services.api.controller.response.OhJsonRespBody
import ai.platon.exotic.services.module.market.persist.DerivateRepository
import ai.platon.exotic.services.module.market.persist.SpotRepository
import ai.platon.exotic.services.module.trade.entity.*
import ai.platon.exotic.services.module.trade.persist.*
import ai.platon.exotic.services.module.trade.service.TradeService
import org.openapitools.client.models.AssetDto
import org.openapitools.client.models.OrderDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
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
    private val finishedOrderRepository: FinishedOrderRepository,
    private val positionRepository: PositionRepository,
    private val spotRepository: SpotRepository,
    private val derivateRepository: DerivateRepository,
    private val crawlTaskRunner: CrawlTaskRunner,
    private val exoticCrawler: ExoticCrawler,
    @Autowired
    private val env: Environment,
    private val tradeService: TradeService
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

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
        @RequestParam(defaultValue = "0") accountId: Long = 0,
    ): ResponseEntity<OhJsonRespBody<Page<Balance>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "lastModifiedDate"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (accountId == 0L) {
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
        @RequestParam(defaultValue = "0") accountId: Long = 0,
    ): ResponseEntity<OhJsonRespBody<Page<PendingOrder>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (accountId == 0L) {
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
        @RequestParam(defaultValue = "0") accountId: Long = 0,
    ): ResponseEntity<OhJsonRespBody<Page<Position>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "id"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        val results = if (accountId == 0L) {
            positionRepository.findAll(pageable)
        } else {
            positionRepository.findAllByAccountId(accountId, pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    enum class Direction(
        val value: String
    ) {
        Long("long"),
        Short("short");
    }

    data class Tradeable(
        var accountId: Long,
        var accountName: String,
        var exchangeName: String,
        var sym4s: String,
        var balanceFree: Double,
    )

    @GetMapping("/get_tradeables")
    fun getTradeables(
        @RequestParam() product: String,
        @RequestParam() direction: Direction,
    ): ResponseEntity<OhJsonRespBody<MutableList<Tradeable>>> {
        var tradeableList = mutableListOf<Tradeable>()
        if (direction == Direction.Long) {
            val balanceItemsHasFree = balanceRepository.findAllByAsset("USDT").filter { it.free > 0 }
            for (item in balanceItemsHasFree) {
                val account = accountRepository.findByAccountIdAndEnable(item.accountId, 1) ?: continue
                if (account.type == "spot") {
                    val pairList =
                        spotRepository.findAllByBaseAndTargetAndExchangeCgId(product, "USDT", account.exchangeCgId)
                    for (pair in pairList) {
                        tradeableList.add(
                            Tradeable(
                                account.accountId,
                                account.account,
                                account.exchangeName,
                                "${pair.base}_USDT",
                                item.free
                            )
                        )
                    }
                } else {
                    val pairList =
                        derivateRepository.findAllByBaseAndTargetAndExchangeCgId(product, "USDT", account.exchangeCgId)
                            .filter { it.contractType == "perpetual" }
                    for (pair in pairList) {
                        tradeableList.add(
                            Tradeable(
                                account.accountId,
                                account.account,
                                account.exchangeName,
                                "${pair.base}_USDT",
                                item.free
                            )
                        )
                    }
                }
            }
        } else {
            var balanceItemsHasFree = balanceRepository.findAllByAsset(product).filter { it.free > 0 }
            for (item in balanceItemsHasFree) {
                val account = accountRepository.findByAccountIdAndEnable(item.accountId, 1) ?: continue
                if (account.type == "spot") {
                    val pairList =
                        spotRepository.findAllByBaseAndTargetAndExchangeCgId(product, "USDT", account.exchangeCgId)
                    for (pair in pairList) {
                        tradeableList.add(
                            Tradeable(
                                account.accountId,
                                account.account,
                                account.exchangeName,
                                "${pair.base}_USDT",
                                item.free
                            )
                        )
                    }
                } else {
                    val pairList =
                        derivateRepository.findAllByBaseAndTargetAndExchangeCgId(product, "USDT", account.exchangeCgId)
                            .filter { it.contractType == "perpetual" }
                    for (pair in pairList) {
                        tradeableList.add(
                            Tradeable(
                                account.accountId,
                                account.account,
                                account.exchangeName,
                                "${pair.base}_USDT",
                                item.free
                            )
                        )
                    }
                }
            }
            balanceItemsHasFree = balanceRepository.findAllByAsset("USDT").filter { it.free > 0 }
            for (item in balanceItemsHasFree) {
                val account = accountRepository.findByAccountIdAndEnable(item.accountId, 1) ?: continue
                if (account.type == "derivate") {
                    val pairList =
                        derivateRepository.findAllByBaseAndTargetAndExchangeCgId(product, "USDT", account.exchangeCgId)
                            .filter { it.contractType == "perpetual" }
                    for (pair in pairList) {
                        tradeableList.add(
                            Tradeable(
                                account.accountId,
                                account.account,
                                account.exchangeName,
                                "${pair.base}_USDT",
                                item.free
                            )
                        )
                    }
                }
            }

        }
        return ResponseEntity.ok(OhJsonRespBody(tradeableList))
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
            try {
                val res = tradeService.getBalance(accountId)
                logger.debug("fetch balance for account {} {}", accountId, res)
                val balanceDto = res.v!!
                var updatedAssetSet = mutableSetOf<String>()
                for (b in balanceDto) {
                    var balance: Balance = convertDtoToEntity(accountId, b)
                    balance.accountId = accountId
                    balanceRepository.save(balance)
                    updatedAssetSet.add(b.asset!!)
                }
                balanceRepository.updateBalancesByAccountIdAndAssetNotIn(accountId, updatedAssetSet)
            } catch (e: Exception) {
                errMsgList.add(Pair(accountId.toString(), e.message ?: "unknown error"))
            }
        }
        return if (errMsgList.isEmpty()) {
            ResponseEntity.ok(OhJsonRespBody())
        } else {
            return ResponseEntity.ok(OhJsonRespBody<List<Pair<String, String>>>().error("failed", errMsgList))
        }
    }

    private fun convertDtoToEntity(accountId: Long, dto: AssetDto): Balance {
        var b = balanceRepository.findByAccountIdAndAsset(accountId, dto.asset!!) ?: Balance()
        b.asset = dto.asset!!
        b.free = dto.free!!
        b.frozen = dto.frozen!!
        b.total = dto.total!!
        b.lastModifiedDate = Instant.now()
        return b
    }

    private fun convertDtoToEntity(accountId: Long, dto: OrderDto): FinishedOrder {
        var b =
            FinishedOrder(Instant.ofEpochMilli(dto.createdTimestampMs), Instant.ofEpochMilli(dto.updatedTimestampMs))
        b.sym4s = dto.symbol
        b.orderId = dto.orderId
        b.side = dto.orderSide
        b.price = dto.price
        b.amount = dto.amount
        b.filledAmount = dto.filledAmount
        b.avgPrice = dto.avgPrice
        b.lastModifiedDate = Instant.now()
        return b
    }

    @GetMapping("/finished_orders")
    fun listFinishedOrders(
        @RequestParam accountId: Long,
        @RequestParam sym4sList: String = "",
        @RequestParam(defaultValue = "0") pageNumber: Int = 0,
        @RequestParam(defaultValue = "20") pageSize: Int = 20,
    ): ResponseEntity<OhJsonRespBody<Page<FinishedOrder>>> {
        val sort = Sort.Direction.DESC
        val sortProperty = "orderCreatedTime"
        val pageable = PageRequest.of(pageNumber, pageSize, sort, sortProperty)
        var results = if (sym4sList.isNullOrEmpty()) {
            finishedOrderRepository.findAllByAccountId(accountId, pageable)
        } else {
            val sym4sSet = sym4sList.split(",").map { it.trim() }.toSet()
            finishedOrderRepository.findAllByAccountIdAndSym4sIn(accountId, sym4sSet, pageable)
        }
        return ResponseEntity.ok(OhJsonRespBody(results))
    }

    @PostMapping("/fetch_finished_orders")
    fun fetchFinishedOrders(
        @RequestParam(defaultValue = "all") accounts: String = "all", // ,分割的账户
        @RequestParam sym4sList: String,
    ): ResponseEntity<OhJsonRespBody<List<Pair<String, String>>>> {
        var accountList: List<Long> = if (accounts == "all") {
            accountRepository.findAllByEnable(1).stream().map { it.accountId }.toList()
        } else {
            accounts.split(",").map { it.toLong() }
        }
        var sym4sList2 = sym4sList.split(",").map { it.trim() }
        var errMsgList = mutableListOf<Pair<String, String>>()
        for (accountId in accountList) {
            for (sym4s in sym4sList2) {
                try {
                    var orderIdStartExclusive =
                        finishedOrderRepository.findTopByAccountIdAndSym4sOrderByOrderIdDesc(accountId, sym4s)?.orderId
                            ?: 0
                    val res = tradeService.getFinishedOrder(
                        accountId,
                        sym4s,
                        limit = 1000,
                        orderIdStartExclusive = orderIdStartExclusive.toString()
                    )
                    val balanceDto = res.v!!
                    for (b in balanceDto) {
                        var order: FinishedOrder = convertDtoToEntity(accountId, b)
                        order.accountId = accountId
                        finishedOrderRepository.save(order)
                    }
                } catch (e: Exception) {
                    errMsgList.add(Pair(accountId.toString(), e.message ?: "unknown error"))
                }
            }
        }
        return if (errMsgList.isEmpty()) {
            ResponseEntity.ok(OhJsonRespBody())
        } else {
            return ResponseEntity.ok(OhJsonRespBody<List<Pair<String, String>>>().error("failed", errMsgList))
        }
    }

}
