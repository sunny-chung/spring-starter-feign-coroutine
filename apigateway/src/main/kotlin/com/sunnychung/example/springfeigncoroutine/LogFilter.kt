package com.sunnychung.example.springfeigncoroutine

import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
class LogFilter : WebFilter {
    val log = LogFactory.getLog(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val requestInstant = Instant.now()
        log.info("Request -- ${exchange.request.method} ${exchange.request.path}")
        return chain.filter(exchange)
            .doOnTerminate {
                log.info("Response -- ${exchange.request.method} ${exchange.request.path} -- ${Duration.between(requestInstant, Instant.now()).toMillis()}ms")
            }
    }
}
