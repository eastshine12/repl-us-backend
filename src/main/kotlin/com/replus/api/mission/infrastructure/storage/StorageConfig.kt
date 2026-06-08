package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig {
    @Bean
    fun videoStoragePort(
        properties: StorageProperties,
        uploadSigners: ObjectProvider<ObjectStorageUploadSigner>,
        uploadVerifiers: ObjectProvider<ObjectStorageUploadVerifier>,
    ): VideoStoragePort =
        when (properties.mode) {
            StorageMode.LOCAL -> LocalVideoStorageAdapter(properties.local)
            StorageMode.OBJECT_STORAGE -> ObjectStorageVideoStorageAdapter(
                properties = properties.objectStorage,
                uploadSigner = uploadSigners.getIfAvailable()
                    ?: throw IllegalStateException("Object storage upload signer is not configured"),
                uploadVerifier = uploadVerifiers.getIfAvailable()
                    ?: throw IllegalStateException("Object storage upload verifier is not configured"),
            )
        }
}
