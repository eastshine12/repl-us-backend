package com.replus.api.mission.infrastructure.storage

import java.time.Instant

interface ObjectStorageUploadSigner {
    fun presignPutObject(command: PresignPutObjectCommand): PresignedPutObject
}

data class PresignPutObjectCommand(
    val bucket: String,
    val objectKey: String,
    val contentType: String,
    val expiresAt: Instant,
    val maxFileSizeBytes: Long,
)

data class PresignedPutObject(
    val uploadUrl: String,
    val requiredHeaders: Map<String, String>,
)
