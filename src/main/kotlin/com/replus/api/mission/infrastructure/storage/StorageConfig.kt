package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig {
    @Bean
    fun videoStoragePort(properties: StorageProperties): VideoStoragePort =
        when (properties.mode) {
            StorageMode.LOCAL -> LocalVideoStorageAdapter(properties.local)
            StorageMode.OBJECT_STORAGE -> throw IllegalStateException(
                "Object storage adapter is not implemented yet",
            )
        }
}
