package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.apache.commons.lang3.StringUtils
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.repository.query.Param
import java.time.Instant
import javax.persistence.*

/**
 * A portal task is a task start with a portal url
 * */


@Table(name = "portal_tasks")
@Entity
@EntityListeners(AuditingEntityListener::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class PortalTask(
    var url: String,

    var args: String = "",

    var priority: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @JsonIdentityReference(alwaysAsId = true)
//    @ManyToOne(fetch = FetchType.LAZY)
    @ManyToOne
    var rule: CrawlRule? = null

    /**
     * The server side id
     * */
    var serverTaskId: String = ""

    var submittedCount: Int = 0

    var successCount: Int = 0

    var resultCount: Int = 0

    var retryCount: Int = 0

    var failedCount: Int = 0

    var finishedCount: Int = 0

    var startTime: Instant = Instant.now()

    var status: TaskStatus = TaskStatus.CREATED

    @CreatedDate
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedDate: Instant = Instant.now()

    val abbreviatedUrl get() = StringUtils.abbreviateMiddle(url, "...", 35)

}
