package ai.platon.exotic.services.module.trade.service

import org.springframework.stereotype.Service

import com.ohquant.client.api.DefaultApi;
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.ResultDto
import javax.annotation.PostConstruct

@Service
class TradeService {
    lateinit var apiInstance: DefaultApi

    @PostConstruct
    fun init() {
        apiInstance = DefaultApi(basePath = "http://124.71.112.150:8081")
    }

    fun getBalance(accountId: Long): ResultDto {

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