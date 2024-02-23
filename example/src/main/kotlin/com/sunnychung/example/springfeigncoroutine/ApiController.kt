package com.sunnychung.example.springfeigncoroutine

import kotlinx.coroutines.delay
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api")
class ApiController {
    val log = LogFactory.getLog(javaClass)

    @Autowired
    lateinit var remoteApi: RemoteApi

    @PostMapping("a")
    suspend fun a(@RequestBody req: ApiData): ApiData {
        log.info("API a -- ${req.x}")
        delay(3000)

        return ApiData("${req.x} a")
    }

    @PostMapping("b")
    suspend fun b(): ApiData {
        log.info("API b")
        return remoteApi.a(ApiData("b"))
    }
}
