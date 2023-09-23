# Spring Feign Coroutine Starter

[![JitPack Release](https://jitpack.io/v/sunny-chung/spring-starter-feign-coroutine.svg)](https://jitpack.io/#sunny-chung/spring-starter-feign-coroutine)

This starter module serves as a temporary solution of using Spring Boot with Coroutine Feign,
until Spring Cloud OpenFeign officially supports it. This module provides Spring Boot integration with the
[Feign 12 Kotlin module](https://github.com/OpenFeign/feign/tree/master/kotlin) and the reactive Spring WebClient.

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
- Client-scoped configuration beans
- Blocking HTTP clients (users can use asynchronous HTTP clients)

Except above, users are expected to be able to migrate their existing Spring Cloud OpenFeign integrations to
this module directly and painlessly (hopefully).

## Performance

A load test was performed using JMeter against the example project in this repository.

Setting:
- 1000 concurrent threads
- Infinite loops of calls within 200 seconds
- Each request requires 3s non-blocking processing time
- Call path: Spring Cloud Gateway --> Example Application --(feign)--> Example Application
- All services are run inside docker containers
- Each docker container has only 200 MB memory limit

|            | Direct call without feign | **Spring Feign Coroutine Starter** | feign-kotlin          |
|------------|---------------------------|------------------------------------|-----------------------|
| Median     | 3025 ms                   | **3017 ms**                        | *Out-of-memory Crash* |
| 95%        | 3149 ms                   | **3095 ms**                        | *Out-of-memory Crash* |
| 99%        | 3322 ms                   | **3137 ms**                        | *Out-of-memory Crash* |
| Maximum    | 3583 ms                   | **3633 ms**                        | *Out-of-memory Crash* |
| Error      | 0.0 %                     | **0.0 %**                          | *Out-of-memory Crash* |
| Throughput | 311.1 / s                 | **314.0 / s**                      | *Out-of-memory Crash* |

It proved that Spring Feign Coroutine Starter takes full advantages of non-blocking coroutines to achieve a performance
similar to direct calling the underlying endpoint. In the test, the median latency added by this module is about 17ms.

Note there is environment noise in the load test. The result does not mean using feign has lower latencies than direct call.

Interested individuals may carry out tests by making use of the [JMeter script included](load-test.jmx),
the [starter script](run-local.sh) and
the example projects ([API Gateway](apigateway), [Example Application](example)).


## Getting Started

In build.gradle.kts (or equivalent), add:

```kotlin
repositories {
    // ...
    maven(url = "https://jitpack.io")
}
```

```kotlin
dependencies {
    // ...
    implementation("com.github.sunny-chung:spring-starter-feign-coroutine:<version>")
}
```

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
Entire response body is read into memory before next step, this means it would perform not as optimized as
reactive clients for large response body or streaming.

`Flow<T>` is not yet supported.
