package com.replus.api.common.operations

import com.replus.api.mission.infrastructure.storage.StorageMode
import com.replus.api.mission.infrastructure.storage.StorageProperties
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("storageHealthIndicator")
class StorageHealthIndicator(
    private val storageProperties: StorageProperties,
) : HealthIndicator {
    override fun health(): Health {
        val details = when (storageProperties.mode) {
            StorageMode.LOCAL -> localDetails()
            StorageMode.OBJECT_STORAGE -> objectStorageDetails()
        }

        return Health.up()
            .withDetails(details)
            .build()
    }

    private fun localDetails(): Map<String, Any> {
        val local = storageProperties.local
        return mapOf(
            "mode" to StorageMode.LOCAL.name,
            "uploadBaseUrlConfigured" to local.uploadBaseUrl.isConfigured(),
            "playbackBaseUrlConfigured" to local.playbackBaseUrl.isConfigured(),
        )
    }

    private fun objectStorageDetails(): Map<String, Any> {
        val objectStorage = storageProperties.objectStorage
        return mapOf(
            "mode" to StorageMode.OBJECT_STORAGE.name,
            "bucketConfigured" to objectStorage.bucket.isConfigured(),
            "publicBaseUrlConfigured" to objectStorage.publicBaseUrl.isConfigured(),
            "regionConfigured" to objectStorage.region.isConfigured(),
            "endpointConfigured" to objectStorage.endpoint.isConfigured(),
            "pathStyleAccessEnabled" to objectStorage.pathStyleAccessEnabled,
        )
    }

    private fun String.isConfigured(): Boolean =
        trim().isNotBlank()
}
