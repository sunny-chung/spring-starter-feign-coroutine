package com.sunnychung.lib.server.springfeigncoroutine.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy
import org.mapstruct.factory.Mappers
import org.springframework.cloud.openfeign.FeignClientProperties

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
internal interface ConfigMapper {
    companion object {
        val INSTANCE: ConfigMapper = Mappers.getMapper(ConfigMapper::class.java)
    }


    fun copy(
        from: FeignClientProperties.FeignClientConfiguration?,
        @MappingTarget to: FeignClientProperties.FeignClientConfiguration
    )
}
