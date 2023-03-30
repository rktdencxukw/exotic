package ai.platon.exotic.services.module.trade.entity

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.*

/**
 * A portal task is a task start with a portal url
 * */


@Table(name = "trade_accounts")
@Entity
@EntityListeners(AuditingEntityListener::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class Account(
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    var accountId: Long = 0
    var availableBalance: Double? = null
    var totalBalance: Double? = null
    var settleCurrency: String = ""
    var exchangeCgId: String = ""
    var exchangeName: String = ""
    var type: String = "spot"
    var account: String = ""
    var enable: Int = 1

    @CreatedDate
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedDate: Instant = Instant.now()

}
