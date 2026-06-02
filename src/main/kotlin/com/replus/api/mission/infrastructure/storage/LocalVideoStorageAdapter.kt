package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.application.port.VideoUploadTarget
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LocalVideoStorageAdapter : VideoStoragePort {
    override fun createUploadTarget(
        objectKey: String,
        contentType: String,
        expiresAt: Instant,
        maxFileSizeBytes: Long,
    ): VideoUploadTarget =
        VideoUploadTarget(
            uploadUrl = "http://localhost:8080/mock-upload/$objectKey",
            method = "PUT",
            objectKey = objectKey,
            requiredHeaders = mapOf("Content-Type" to contentType),
            expiresAt = expiresAt,
            maxFileSizeBytes = maxFileSizeBytes,
        )

    override fun playbackUrl(objectKey: String): String =
        "http://localhost:8080/mock-playback/$objectKey"

    override fun thumbnailUrl(objectKey: String): String =
        "http://localhost:8080/mock-playback/$objectKey"
}
