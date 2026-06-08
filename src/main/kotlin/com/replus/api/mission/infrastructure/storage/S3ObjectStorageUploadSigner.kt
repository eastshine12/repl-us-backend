package com.replus.api.mission.infrastructure.storage

class S3ObjectStorageUploadSigner(
    private val putObjectPresigner: S3PutObjectPresigner,
) : ObjectStorageUploadSigner {
    override fun presignPutObject(command: PresignPutObjectCommand): PresignedPutObject {
        val presigned = putObjectPresigner.presignPutObject(
            S3PresignPutObjectRequest(
                bucket = command.bucket,
                objectKey = command.objectKey,
                contentType = command.contentType,
                expiresAt = command.expiresAt,
                maxFileSizeBytes = command.maxFileSizeBytes,
            ),
        )

        return PresignedPutObject(
            uploadUrl = presigned.uploadUrl,
            requiredHeaders = presigned.requiredHeaders,
        )
    }
}
