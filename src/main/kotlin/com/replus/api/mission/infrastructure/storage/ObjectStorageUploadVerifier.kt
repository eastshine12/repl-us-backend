package com.replus.api.mission.infrastructure.storage

interface ObjectStorageUploadVerifier {
    fun verifyUploadedObject(command: VerifyUploadedObjectCommand): VerifiedUploadedObject
}

data class VerifyUploadedObjectCommand(
    val bucket: String,
    val objectKey: String,
    val expectedContentType: String,
    val expectedFileSizeBytes: Long,
)

data class VerifiedUploadedObject(
    val exists: Boolean,
    val contentType: String?,
    val fileSizeBytes: Long?,
)
