package com.sunnychung.lib.server.springfeigncoroutine.autoconfiguration

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata
import org.springframework.cloud.openfeign.FeignAutoConfiguration

class ExcludeFeignImportFilter : AutoConfigurationImportFilter {
    val EXCLUDE_AUTOCONFIGURATIONS = listOf(FeignAutoConfiguration::class).map {
        it.qualifiedName
    }
        .toSet()

    override fun match(
        autoConfigurationClasses: Array<out String>,
        autoConfigurationMetadata: AutoConfigurationMetadata
    ): BooleanArray {
        return autoConfigurationClasses.map {
            !EXCLUDE_AUTOCONFIGURATIONS.contains(it)
        }.toBooleanArray()
    }
}
