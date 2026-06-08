package com.replus.api.mission.infrastructure.storage

class S3ObjectStorageUploadVerifier(
    private val headObjectClient: S3HeadObjectClient,
) : ObjectStorageUploadVerifier {
    override fun verifyUploadedObject(command: VerifyUploadedObjectCommand): VerifiedUploadedObject =
        try {
            val headObject = headObjectClient.headObject(
                S3HeadObjectRequest(
                    bucket = command.bucket,
                    objectKey = command.objectKey,
                ),
            )
            VerifiedUploadedObject(
                exists = true,
                contentType = headObject.contentType,
                fileSizeBytes = headObject.fileSizeBytes,
            )
        } catch (_: S3ObjectNotFoundException) {
            VerifiedUploadedObject(
                exists = false,
                contentType = null,
                fileSizeBytes = null,
            )
        }
}
