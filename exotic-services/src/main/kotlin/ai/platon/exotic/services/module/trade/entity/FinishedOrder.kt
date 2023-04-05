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


@Table(name = "trade_finished_orders")
@Entity
@EntityListeners(AuditingEntityListener::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class FinishedOrder(
    var orderCreatedTime: Instant,
    var orderFinishedTime: Instant
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    var accountId: Long = 0
    var sym4s: String = ""
    var symbol: String = "" // 交易所symbol
    var orderId: String = ""
    var side: String = ""
    var price: Double = 0.0
    var amount: Double = 0.0
    var filledAmount: Double = 0.0

    @CreatedDate
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    var lastModifiedDate: Instant = Instant.now()

}
