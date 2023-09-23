package com.sunnychung.lib.server.springfeigncoroutine.httpclient.webclient

import feign.AsyncClient
import feign.Request
import feign.Response
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClientRequest
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

class WebClientExecutor(webClientCustomizer: (WebClient.Builder.() -> Unit)? = null) : AsyncClient<Unit> {
    val client = WebClient.builder()
        .apply { webClientCustomizer?.invoke(it) }
        .build()

    override fun execute(
        request: Request,
        options: Request.Options,
        requestContext: Optional<Unit>
    ): CompletableFuture<Response> {
        return client.method(HttpMethod.valueOf(request.httpMethod().name))
            .uri(request.url())
            .headers {
                request.headers().forEach { key, values ->
                    it.addAll(key, values.toList())
                }
            }
            .bodyValue(request.body())
            .httpRequest {
                val nativeRequest = it.getNativeRequest<HttpClientRequest>() // coupled with Netty
                nativeRequest.responseTimeout(Duration.ofMillis(options.readTimeoutMillis().toLong()))
            }
            .exchangeToMono { response ->
                println("exchangeToMono")
                Mono.just(
                    Response.builder()
                        .status(response.statusCode().value())
                        .headers(response.headers().asHttpHeaders().toMap())
                        .request(request)
                ).zipWith(response.bodyToMono(ByteArrayResource::class.java))
            }
            .map { it ->
                val builder = it.t1
                val bodyBytes = it.t2
                builder.body(bodyBytes.byteArray).build()
            }
            .toFuture()
    }
}
