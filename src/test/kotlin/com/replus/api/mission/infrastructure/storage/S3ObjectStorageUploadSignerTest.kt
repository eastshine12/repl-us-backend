package com.replus.api.mission.infrastructure.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class S3ObjectStorageUploadSignerTest {
    private val putObjectPresigner = RecordingS3PutObjectPresigner()
    private val signer = S3ObjectStorageUploadSigner(putObjectPresigner)

    @Test
    fun `put object presign result is returned as upload signer result`() {
        // given
        putObjectPresigner.nextResult = S3PresignedPutObject(
            uploadUrl = "https://object-storage.example.dev/signed-upload",
            requiredHeaders = mapOf(
                "content-type" to "video/webm",
                "x-amz-checksum-crc32" to "checksum",
            ),
        )

        // when
        val presigned = signer.presignPutObject(
            PresignPutObjectCommand(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                contentType = "video/webm",
                expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
                maxFileSizeBytes = 15_000_000,
            ),
        )

        // then
        assertThat(putObjectPresigner.recordedRequest).isEqualTo(
            S3PresignPutObjectRequest(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                contentType = "video/webm",
                expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
                maxFileSizeBytes = 15_000_000,
            ),
        )
        assertThat(presigned.uploadUrl).isEqualTo("https://object-storage.example.dev/signed-upload")
        assertThat(presigned.requiredHeaders).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "content-type" to "video/webm",
                "x-amz-checksum-crc32" to "checksum",
            ),
        )
    }

    private class RecordingS3PutObjectPresigner : S3PutObjectPresigner {
        var recordedRequest: S3PresignPutObjectRequest? = null
        var nextResult: S3PresignedPutObject? = null

        override fun presignPutObject(request: S3PresignPutObjectRequest): S3PresignedPutObject {
            recordedRequest = request
            return requireNotNull(nextResult)
        }
    }
}
