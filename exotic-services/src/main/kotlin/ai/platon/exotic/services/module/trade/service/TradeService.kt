package ai.platon.exotic.services.module.trade.service

import com.ohquant.client.api.DefaultApi
import org.springframework.stereotype.Service

import org.openapitools.client.models.ResultDto
import org.openapitools.client.models.VectorAssetDto
import org.openapitools.client.models.VectorOrderDto
import org.springframework.core.env.Environment
import java.time.Instant
import javax.annotation.PostConstruct

@Service
class TradeService(
    val env: Environment,
) {
    lateinit var apiInstance: DefaultApi

    @PostConstruct
    fun init() {
//        apiInstance = DefaultApi(basePath = "http://124.71.112.150:8081")
        val executorServer = env.getProperty("oh_executor.server", "124.71.112.150:8081") // hw-gz-1
        println("executorServer: $executorServer")
        apiInstance = DefaultApi("http://$executorServer")
    }

    fun getFinishedOrder(accountId: Long, sym4s: String, limit: Int, orderIdStartExclusive: String): VectorOrderDto {
        return if (orderIdStartExclusive != "0") {
            apiInstance.fetchFinishedOrder(
                accountId.toString(),
                sym4s,
                limit = limit,
                orderIdStartExclusive = orderIdStartExclusive
            )
        } else {
            val startTime = Instant.now().minusSeconds(60 * 60 * 24 * 15).epochSecond
            val endTime = Instant.now().epochSecond
            apiInstance.fetchFinishedOrder(
                accountId.toString(),
                sym4s,
                limit = 1000,
                timeSecsStart = startTime,
                timeSecsEnd = endTime
            )
        }
    }

    fun getBalance(accountId: Long): VectorAssetDto {
        return apiInstance.fetchBalance(accountId.toString())

//        try {
//            val result : ResultDto = apiInstance.fetchBalance(accountId)
//            println(result)
//        } catch (e: ClientException) {
//            println("4xx response calling DefaultApi#fetchBalance")
//            e.printStackTrace()
//        } catch (e: ServerException) {
//            println("5xx response calling DefaultApi#fetchBalance")
//            e.printStackTrace()
//        }
//
//
//        val defaultClient = Configuration.getDefaultApiClient();
//        defaultClient.basePath = "http://192.168.0.183:8081";
//
//        val apiInstance = DefaultApi(defaultClient);
//        try {
//            val result = apiInstance.fetchBalance(accountId.toString())
//            println(result);
//        } catch (e: ApiException) {
//            System.err.println("Exception when calling DefaultApi#root");
//            System.err.println("Status code: " + e.code);
//            System.err.println("Reason: " + e.responseBody);
//            System.err.println("Response headers: " + e.responseHeaders);
//            e.printStackTrace();
//        }
    }
}