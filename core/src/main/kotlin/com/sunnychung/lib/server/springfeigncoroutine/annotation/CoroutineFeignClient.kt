package com.sunnychung.lib.server.springfeigncoroutine.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CoroutineFeignClient(val name: String)
