package ai.platon.exotic.services.module.trade.persist

import ai.platon.exotic.services.module.trade.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable
import java.time.Instant
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, Serializable> {
    fun findAllByAccountId(accountId: Long, pageable: Pageable): Page<Account>
    fun findAllByEnable(enable: Int=1): List<Account>
    fun findByAccountIdAndEnable(accountId: Long, enable: Int=1): Account?
}

@Repository
interface BalanceRepository : JpaRepository<Balance, Serializable> {
    fun findAllByAccountId(accountId: Long, pageable: Pageable): Page<Balance>
    fun findByAccountIdAndAsset(accountId: Long, asset: String): Balance?
    fun findAllByAsset(asset: String): List<Balance>

    @Transactional
    @Modifying
    @Query("update Balance b set b.total=0,b.free=0,b.frozen=0,last_modified_date=:now where b.accountId=:accountId and b.asset not in :assets")
    fun updateBalancesByAccountIdAndAssetNotIn(@Param(value = "accountId") accountId: Long, @Param(value = "assets") assets: Set<String>, @Param(value = "now") now: Instant = Instant.now())
}
@Repository
interface PendingOrderRepository : JpaRepository<PendingOrder, Serializable> {
    fun findAllByAccountId(accountId: Long, pageable: Pageable): Page<PendingOrder>
}
@Repository
interface PositionRepository : JpaRepository<Position, Serializable> {
    fun findAllByAccountId(accountId: Long, pageable: Pageable): Page<Position>
}
@Repository
interface FinishedOrderRepository : JpaRepository<FinishedOrder, Serializable> {
    fun findAllByAccountId(accountId: Long, pageable: Pageable): Page<FinishedOrder>
    fun findAllByAccountIdAndSym4sIn(accountId: Long, sym4sSet: Set<String>, pageable: Pageable): Page<FinishedOrder>
    fun findTopByAccountIdAndSym4sOrderByOrderIdDesc(accountId: Long, sym4s: String): FinishedOrder?
}
