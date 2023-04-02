package ai.platon.exotic.services.api.component

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


// https://www.cnblogs.com/yucongblog/p/14107555.html
/**
 * @Description 解决springboot高版本下日期转json时jackson方式不生效问题
 *
# enable @EnableWebMvc 使用mvc拦截器后这里无效
 *
 */
@Configuration
public class DateFormatForJson : WebMvcConfigurer {
    @Bean
    fun jackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter? {
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = ohObjectMapper()
        return converter
    }

    @Bean
    fun ohObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        val jm = JavaTimeModule()
        jm.addSerializer(Instant::class.java, MyInstantSerializer())
        jm.addDeserializer(Instant::class.java, MyInstantDeserializer())
        mapper.registerModule(jm)
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
        mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        return mapper
    }

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
        converters.add(jackson2HttpMessageConverter())
    }
}

private class MyInstantSerializer
    : InstantSerializer(InstantSerializer.INSTANCE, false, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(TimeZone.getTimeZone("GMT+8:00").toZoneId()))
private class MyInstantDeserializer
    : InstantDeserializer<Instant>(InstantDeserializer.INSTANT, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(TimeZone.getTimeZone("GMT+8:00").toZoneId()))
