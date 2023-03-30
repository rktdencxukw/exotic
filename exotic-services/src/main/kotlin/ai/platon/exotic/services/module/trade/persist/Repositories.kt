package ai.platon.exotic.services.module.trade.persist

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.api.entity.SysProp
import ai.platon.exotic.services.api.entity.generated.FullFieldProduct
import ai.platon.exotic.services.api.entity.generated.IntegratedProduct
import ai.platon.exotic.services.module.market.entity.Derivate
import ai.platon.exotic.services.module.market.entity.DerivateExchange
import ai.platon.exotic.services.module.market.entity.Spot
import ai.platon.exotic.services.module.market.entity.SpotExchange
import ai.platon.exotic.services.module.trade.entity.Account
import ai.platon.exotic.services.module.trade.entity.Balance
import ai.platon.exotic.services.module.trade.entity.PendingOrder
import ai.platon.exotic.services.module.trade.entity.Position
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, Serializable> {
    fun findAllByAccountId(accountId: Int, pageable: Pageable): Page<Account>
}

@Repository
interface BalanceRepository : JpaRepository<Balance, Serializable> {
    fun findAllByAccountId(accountId: Int, pageable: Pageable): Page<Balance>
}
@Repository
interface PendingOrderRepository : JpaRepository<PendingOrder, Serializable> {
    fun findAllByAccountId(accountId: Int, pageable: Pageable): Page<PendingOrder>
}
@Repository
interface PositionRepository : JpaRepository<Position, Serializable> {
    fun findAllByAccountId(accountId: Int, pageable: Pageable): Page<Position>
}
