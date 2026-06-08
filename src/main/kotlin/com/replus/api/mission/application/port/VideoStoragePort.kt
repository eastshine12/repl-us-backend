package com.replus.api.mission.application.port

import java.time.Instant

interface VideoStoragePort {
    fun createUploadTarget(
        objectKey: String,
        contentType: String,
        expiresAt: Instant,
        maxFileSizeBytes: Long,
    ): VideoUploadTarget

    fun playbackUrl(objectKey: String): String

    fun thumbnailUrl(objectKey: String): String

    fun verifyUploadedObject(
        objectKey: String,
        expectedContentType: String,
        expectedFileSizeBytes: Long,
    ): VideoUploadVerification
}

data class VideoUploadTarget(
    val uploadUrl: String,
    val method: String,
    val objectKey: String,
    val requiredHeaders: Map<String, String>,
    val expiresAt: Instant,
    val maxFileSizeBytes: Long,
)

data class VideoUploadVerification(
    val exists: Boolean,
    val contentType: String?,
    val fileSizeBytes: Long?,
)
