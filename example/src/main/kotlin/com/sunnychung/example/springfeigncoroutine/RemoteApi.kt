package com.sunnychung.example.springfeigncoroutine

import com.sunnychung.lib.server.springfeigncoroutine.annotation.CoroutineFeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@CoroutineFeignClient(name = "svc-a")
interface RemoteApi {

    @PostMapping("a")
    suspend fun a(@RequestBody req: ApiData): ApiData
}
