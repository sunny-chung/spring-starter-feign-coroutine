# Spring Feign Coroutine Starter

![GitHub](https://img.shields.io/github/license/sunny-chung/spring-starter-feign-coroutine)
![Maven Central](https://img.shields.io/maven-central/v/io.github.sunny-chung/spring-starter-feign-coroutine)

NOTE: Since v0.4.0, the artifact groupId is changed to 'io.github.sunny-chung'.

This starter module serves as a temporary solution of using Spring Boot with Coroutine Feign,
until Spring Cloud OpenFeign officially supports it. This module provides Spring Boot integration with the
[Feign Kotlin module](https://github.com/OpenFeign/feign/tree/master/kotlin), the reactive Spring WebClient and Micrometer.

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
dependencies {
    // ...
    implementation("io.github.sunny-chung:spring-starter-feign-coroutine:<version>")
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

## Micrometer Integration
Similar to Spring Cloud OpenFeign, just include Micrometer in classpath, then everything is autoconfigured. It can be configured using the same set of application properties.

### Demo

Run the demo servers using `./run-local.sh`, make a cURL request to API gateway which calls service-b which calls service-a.

```
curl --verbose \
  --request "POST" \
  --url "http://localhost:10000/api/b" \
  --header "Content-Type: application/json" \
  --data "{\"x\": 10}"
```

The server log combined in Docker Compose like below is observed:

```
spring-feign-coroutine-apigateway-1  | 2024-02-24T08:33:12.075Z  INFO 1 --- [ctor-http-nio-2] [65d9a9c86af376b32fea95088ceb267b-2fea95088ceb267b] c.s.e.springfeigncoroutine.LogFilter     : Request -- POST /api/b
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.602Z  INFO 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.s.ApiController                    : API b
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.608Z DEBUG 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] ---> POST http://service-a:8080/api/a HTTP/1.1
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.608Z DEBUG 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] Content-Length: 15
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.608Z DEBUG 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] content-type: application/json
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.608Z DEBUG 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] 
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.608Z DEBUG 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] {
spring-feign-coroutine-service-b-1   |   "x" : "b"
spring-feign-coroutine-service-b-1   | }
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:12.608Z DEBUG 1 --- [         task-1] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] ---> END HTTP (15-byte body)
spring-feign-coroutine-service-a-1   | 2024-02-24T08:33:13.211Z  INFO 1 --- [ctor-http-nio-2] [65d9a9c86af376b32fea95088ceb267b-30c56df6c7d7ee63] c.s.e.s.ApiController                    : API a -- b
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:16.333Z DEBUG 1 --- [ctor-http-nio-4] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] <--- HTTP/1.1 200 (3724ms)
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:16.333Z DEBUG 1 --- [ctor-http-nio-4] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] content-length: 11
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:16.333Z DEBUG 1 --- [ctor-http-nio-4] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] content-type: application/json
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:16.333Z DEBUG 1 --- [ctor-http-nio-4] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] 
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:16.333Z DEBUG 1 --- [ctor-http-nio-4] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] {"x":"b a"}
spring-feign-coroutine-service-b-1   | 2024-02-24T08:33:16.333Z DEBUG 1 --- [ctor-http-nio-4] [65d9a9c86af376b32fea95088ceb267b-5273a545473a290f] c.s.e.springfeigncoroutine.RemoteApi     : [RemoteApi#a] <--- END HTTP (11-byte body)
spring-feign-coroutine-apigateway-1  | 2024-02-24T08:33:16.370Z  INFO 1 --- [ctor-http-nio-2] [65d9a9c86af376b32fea95088ceb267b-2fea95088ceb267b] c.s.e.springfeigncoroutine.LogFilter     : Response -- POST /api/b -- 4294ms
```

Note the traceId (65d9a9c86af376b32fea95088ceb267b) is propagated and consistent.
