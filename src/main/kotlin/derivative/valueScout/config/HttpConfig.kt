package derivative.valueScout.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class HttpConfig {

    @Bean
    fun restClient(): RestClient =
        RestClient.builder()
            .requestFactory(SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(5000)
                setReadTimeout(5000)
            })
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .build()
}