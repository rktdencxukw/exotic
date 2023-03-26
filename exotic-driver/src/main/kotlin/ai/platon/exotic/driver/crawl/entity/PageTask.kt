package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.driver.crawl.scraper.TaskStatus
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.*

@Table(name = "page_tasks")
@Entity
@EntityListeners(AuditingEntityListener::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class PageTask(
    var url: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    var portalTask: PortalTask? = null

    var retryCount: Int = 0

    var startTime: Instant = Instant.now()

    var status: TaskStatus = TaskStatus.CREATED

    @CreatedDate
    var createdTime: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedTime: Instant = Instant.now()
}
