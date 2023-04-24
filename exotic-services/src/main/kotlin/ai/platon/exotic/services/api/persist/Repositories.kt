package ai.platon.exotic.services.api.persist

import ai.platon.exotic.driver.crawl.entity.CrawlRule
import ai.platon.exotic.driver.crawl.entity.PortalTask
import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import ai.platon.exotic.services.api.entity.SysProp
import ai.platon.exotic.services.api.entity.generated.FullFieldProduct
import ai.platon.exotic.services.api.entity.generated.IntegratedProduct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Repository
interface CrawlRuleRepository : JpaRepository<CrawlRule, Serializable> {
    fun findAllByStatusIn(status: List<String>, pageable: Pageable): Page<CrawlRule>
    fun findAllByStatusNot(status: String, pageable: Pageable): Page<CrawlRule>

    // if multi fields, maybe need to custom a dto object
    @Query(
        value = "select u.tags from CrawlRule u",
        countQuery = "select count(u.tags) from CrawlRule u",
    )
    fun findAllTags(pageable: Pageable): Page<String>
}

@Repository
interface PortalTaskRepository : JpaRepository<PortalTask, Serializable> {
    fun findAllByStatusInAndCreatedDateGreaterThan(
        status: List<String>, createdDate: LocalDateTime
    ): List<PortalTask>

    fun findAllByStatus(status: TaskStatus, pageable: Pageable): Page<PortalTask>

    fun findAllByRule(rule: CrawlRule, page: Pageable): Page<PortalTask>

}

@Repository
interface FullFieldProductRepository : JpaRepository<FullFieldProduct, Serializable> {
    fun findAllByIdGreaterThan(id: Long): List<FullFieldProduct>
}

@Repository
interface IntegratedProductRepository : JpaRepository<IntegratedProduct, Serializable> {
    fun findTopByOrderByIdDesc(): Optional<IntegratedProduct>
}

@Repository
interface SysPropRepository : JpaRepository<SysProp, Serializable>
