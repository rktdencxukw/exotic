package ai.platon.exotic.services.module.market.persist

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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Repository
interface DerivateRepository : JpaRepository<Derivate, Serializable> {

//    @Query("select p from derivates p where p.base like :prefix% limit :pageable.pageNumber*:pageable.pageSize, :pageable.pageSize")
//    fun findAllByBaseLike(@Param("prefix") prefix: String, pageable: Pageable): Page<Derivate>
    fun findAllByBaseLike(keyword: String, pageable: Pageable): Page<Derivate>
}

@Repository
interface SpotRepository : JpaRepository<Spot, Serializable> {
    fun findAllByBaseLike(keyword: String, pageable: Pageable): Page<Spot>
}

@Repository
interface SpotExchangeRepository : JpaRepository<SpotExchange, Serializable> {
    // keyword surround % must be added by caller
    fun findAllByNameLike(keyword: String, pageable: Pageable): Page<SpotExchange>
}

@Repository
interface DerivateExchangeRepository : JpaRepository<DerivateExchange, Serializable> {
    // keyword surround % must be added by caller
    fun findAllByNameLike(keyword: String, pageable: Pageable): Page<DerivateExchange>
}
