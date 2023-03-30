package ai.platon.exotic.services.module.trade.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.*

/**
 * A portal task is a task start with a portal url
 * */


@Table(name = "trade_balances")
@Entity
@EntityListeners(AuditingEntityListener::class)
class Balance(
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)

    var id: Long? = null
    var accountId: Int = 0
    var asset: String = ""
    var total: Double = 0.0
    var free: Double = 0.0
    var locked: Double = 0.0

    @CreatedDate
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedDate: Instant = Instant.now()

}
