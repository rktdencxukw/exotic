package ai.platon.exotic.services.module.market.entity

import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.apache.commons.lang3.StringUtils
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.*

/**
 * A portal task is a task start with a portal url
 * */


@Table(name = "market_spots")
@Entity
@EntityListeners(AuditingEntityListener::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class Spot(
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    var base: String = ""
    var target: String = ""
    var volume: Double = 0.0
    var exchangeCgId: String = ""
    var link: String = ""

    @CreatedDate
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedDate: Instant = Instant.now()

}
