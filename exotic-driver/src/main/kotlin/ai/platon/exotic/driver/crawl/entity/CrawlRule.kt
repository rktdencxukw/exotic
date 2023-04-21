package ai.platon.exotic.driver.crawl.entity

import ai.platon.exotic.driver.common.ExoticUtils
import ai.platon.exotic.driver.common.NameGenerator
import ai.platon.exotic.driver.crawl.scraper.RenderType
import ai.platon.exotic.driver.crawl.scraper.RuleStatus
import ai.platon.exotic.driver.crawl.scraper.RuleType
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.IpType
import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.format.annotation.DateTimeFormat
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import javax.persistence.*

@Table(name = "crawl_rules")
@Entity
@EntityListeners(AuditingEntityListener::class)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class CrawlRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null

    @Column(name = "name", length = 32)
    var name: String = randomName()

    @Column(name = "label", length = 64)
    var label: String? = null

    @Lob
    @Column(name = "portal_urls")
    var portalUrls: String = ""

    @Column(name = "out_link_selector", length = 64)
    var outLinkSelector: String? = null

    @Lob
    @Column(name = "sql_template")
    var sqlTemplate: String? = null

    @Column(name = "description", length = 128)
    var description: String? = null

    @Column(name = "next_page_selector", length = 64)
    var nextPageSelector: String? = null

    @Column(name = "max_pages")
    var maxPages: Int = 30

    @Column(name = "start_time")
    @DateTimeFormat(pattern = "yyyy-MM-ddTHH:mm:ssZ")
    var startTime: Instant = Instant.EPOCH

    @Column(name = "dead_time")
    @DateTimeFormat(pattern = "yyyy-MM-ddTHH:mm:ssZ")
    var deadTime: Instant = DateTimes.doomsday

    @Column(name = "last_crawl_time")
    var lastCrawlTime: Instant = Instant.EPOCH

    @Column(name = "start_count")
    var crawlCount: Int? = 0

    @Column(name = "crawl_history", length = 1024)
    var crawlHistory: String = ""

    @Column(name = "period")
    var period: Duration = Duration.ofDays(3650)

    @Column(name = "cron_expression")
    var cronExpression: String? = null

    /**
     * Enum: Created, Running, Paused
     * */
    @Column(name = "status", length = 8)
    var status: String = RuleStatus.Created.toString()

    @Column(name = "type", length = 16)
    var type: String? = RuleType.Portal.toString()

    @Column(name = "render_type", length = 16)
    var renderType: String? = RenderType.Browser.toString()

    var scrapeServer: String? = "127.0.0.1:8182"
    var proxyServer: String? = ""

    var fetchMode: String? = FetchMode.BROWSER.toString()

    /**
     * ids of last page in EntityListRule or last item in EntityItemRule
     */
    @Column(name = "ids_of_last", length = 1024)
    var idsOfLast: String = ""

    var waitForSelector: String? = null
    var waitForTimeoutMillis: Long? = 30000L

    var ipTypeWant: String = IpType.SERVER.name
    var fetchModeWant: String = FetchMode.BROWSER.name

    /**
     * The time difference, in minutes, between UTC time and local time.
     * */
    @Column(name = "timezone_offset_minutes")
    var timezoneOffsetMinutes: Int? = -480

    @CreatedDate
    @Column(name = "created_date")
    var createdDate: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "last_modified_date")
    var lastModifiedDate: Instant = Instant.now()

    // TODO 延迟加载，分页
    //    @OneToMany(fetch = FetchType.LAZY)
//    @OneToMany(mappedBy = "rule", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
//    @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
//    val portalTasks: MutableList<PortalTask> = mutableListOf()

    val zoneOffset: ZoneOffset
        get() {
            val minutes = timezoneOffsetMinutes ?: -480
            return ZoneOffset.ofHoursMinutes(minutes / 60, minutes % 60)
        }

    val portalUrlList
        get() = portalUrls.split("\n")
            .filter { it.isNotBlank() }
            .filter { UrlUtils.isValidUrl(it) }

    val descriptivePeriod: String
        get() {
            val expression = cronExpression
            return when {
                period.isNegative && expression != null -> describeCron(expression)
                period.toDays() > 360 -> "once"
                else -> "every " + ExoticUtils.formatDuration(period.seconds)
            }
        }

    val deducedDomain: String
        get() {
            val host = portalUrlList.firstOrNull()?.let { UrlUtils.getURLOrNull(it) }?.host
            if (host != null) {
                // TODO: use URLUtil.getDomainName
                val parts = host.split(".")
                return if (parts[0] == "www") {
                    parts.drop(1).joinToString(".")
                } else {
                    parts.takeLast(2).joinToString(".")
                }
            }
            return "-"
        }
    companion object {
        val FAR_TIME: Instant = Instant.parse("2300-12-31T23:59:59.999999999Z")
    }
    val nextCrawlTime: Instant
        get() {
            if (period.seconds > 0) {
                return lastCrawlTime +period
            }

            if (cronExpression.isNullOrBlank()) {
                return FAR_TIME
            }

            val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
            val parser = CronParser(cronDefinition)
            val quartzCron: Cron = parser.parse(cronExpression)
            quartzCron.validate()
            val executionTime = ExecutionTime.forCron(quartzCron)

            val zonedLastCrawlTime = lastCrawlTime.atZone(DateTimes.zoneId)
            val nextExecution = executionTime.nextExecution(zonedLastCrawlTime)
            return if (nextExecution.isPresent) {
                nextExecution.get().toInstant()
            } else {
                lastCrawlTime;
            }
        }

    val localCreatedDateTime: LocalDateTime
        get() = createdDate.atOffset(zoneOffset).toLocalDateTime()

    val localLastModifiedDateTime: LocalDateTime
        get() = lastModifiedDate.atOffset(zoneOffset).toLocalDateTime()

    // kcread 按 LoadOptions 格式组装参数，稍后会加入到 sql 向pulsar提交。会被更具体类型任务调用这里，所以这里只处理公共部分
    fun buildArgs(): String {
        val taskTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val formattedTime = DateTimes.format(taskTime, "YYMMddHH")
        val taskIdSuffix = id ?: formattedTime
        val taskId = "r$taskIdSuffix"
        var args = "-taskId $taskId -taskTime $taskTime"
        if (deadTime != DateTimes.doomsday) {
            args += " -deadTime $deadTime"
        }
        if (waitForSelector != null) {
            args += " -waitForSelector $waitForSelector"
        }
        if (waitForTimeoutMillis != null) {
            args += " -waitForTimeoutMillis $waitForTimeoutMillis"
        }
        if (proxyServer.isNullOrBlank().not()) {
            args += " -proxyServer $proxyServer"
        }
        if (fetchMode.isNullOrBlank().not()) {
            args += " -fetchMode $fetchMode"
        }
        return args
    }

    final fun randomName(): String {
        return NameGenerator.gen()
    }

    private fun describeCron(expression: String): String {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
        val parser = CronParser(cronDefinition)
        val quartzCron: Cron = parser.parse(expression)
        val descriptor = CronDescriptor.instance(Locale.getDefault())
        return descriptor.describe(quartzCron)
    }

    @PrePersist
    @PreUpdate
    @PostLoad
    final fun adjustFields() {
        val count = cronExpression?.split(" ") ?: 0
        if (count == 5) {
            cronExpression = "0 $cronExpression"
        }

        name = name.takeIf { it.isNotBlank() } ?: randomName()
    }
}
