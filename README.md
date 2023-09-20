# Spring Feign Coroutine Starter

This starter module serves as a temporary solution of using Spring Boot with Coroutine Feign,
until Spring Cloud OpenFeign officially supports it. This module provides Spring Boot integration
with the [Feign 12 Kotlin module](https://github.com/OpenFeign/feign/tree/master/kotlin).

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
# TODO provide configurations
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

## Current Status

This module is still under development and has not been officially released. However, interested parties may
download this as a maven dependency via JitPack.
