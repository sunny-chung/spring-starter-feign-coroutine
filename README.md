# Spring Feign Coroutine Starter

This starter module serves as a temporary solution of using Spring Boot with Coroutine Feign,
until Spring Cloud OpenFeign officially supports it. This module provides Spring Boot integration
with the [Feign 12 Kotlin module](https://github.com/OpenFeign/feign/tree/master/kotlin) and Spring WebClient.

A limited set of Spring Cloud OpenFeign features is supported. *Unsupported* features include:
- Default Encoder and Decoder beans (users must provide these beans)
- Load balancer (URL starts with `lb://`)
- Refresh context
- Lazy attributes
- AOT
- Circuit breaker
- PageJacksonModule and SortJacksonModule beans (users may still customize the encoders and decoders to support)
- OAuth 2 interceptors
- Caching
- Blocking HTTP clients (users can use asynchronous HTTP clients)

Except above, users are expected to be able to migrate their existing Spring Cloud OpenFeign integrations to
this module directly and painlessly (hopefully).

## Performance

A load test was performed using JMeter against the example project in this repository.

Setting:
- 1000 concurrent threads
- Infinite loops of calls within 200 seconds
- Each request requires 3s non-blocking processing time
- Call path: Spring Cloud Gateway -> Example Application --(feign)--> Example Application

|            | Direct call without feign | **Spring Feign Coroutine Starter** | feign-kotlin |
|------------|---------------------------|------------------------------------|--------------|
| Median     | 3025 ms                   | 3017 ms                            | *OOM Crash*  |
| 95%        | 3149 ms                   | 3095 ms                            | *OOM Crash*  |
| 99%        | 3322 ms                   | 3137 ms                            | *OOM Crash*  |
| Maximum    | 3583 ms                   | 3633 ms                            | *OOM Crash*  |
| Error      | 0.0 %                     | 0.0 %                              | *OOM Crash*  |
| Throughput | 311.1 / s                 | 314.0 / s                          | *OOM Crash*  |

It proved that Spring Feign Coroutine Starter taking full advantages of non-blocking coroutines to achieve
a performance similar to direct calling the underlying endpoint.

Interested individuals may carry out tests by making use of the [JMeter script included](load-test.jmx) and
the example projects ([API Gateway](apigateway), [Example Application](example)).

## Example

```kotlin
@Configuration
@EnableCoroutineFeignClients(basePackages = ["com.sunnychung.example.springfeigncoroutine"])
class FeignConfig {
    @Bean
    fun decoder() = JacksonDecoder(jacksonObjectMapper())

    @Bean
    fun encoder() = JacksonEncoder()
}
```

```kotlin
@CoroutineFeignClient(name = "svc-a")
interface RemoteApi {

    @PostMapping("a")
    suspend fun a(@RequestBody req: ApiData): ApiData
}
```

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            default-request-headers:
              content-type: application/json
          svc-a:
            logger-level: full
            url: http://service-a:8080/api/
```

```kotlin
@RequestMapping("api")
class ApiController {

    @Autowired
    lateinit var remoteApi: RemoteApi

    @PostMapping("b")
    suspend fun b(): ApiData {
        return remoteApi.a(ApiData("b"))
    }
}
```

## Limitation
Entire response body is read into memory before next step, this means it would perform poorly for
large response body or streaming.

`Flow<T>` is not yet supported.

## Current Status

This module is still under development and has not been officially released. However, interested parties may
download this as a maven dependency via JitPack.
