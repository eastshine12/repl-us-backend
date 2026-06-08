package com.replus.api.mission.infrastructure.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ObjectStorageVideoStorageAdapterTest {
    private val signer = RecordingObjectStorageUploadSigner()
    private val verifier = RecordingObjectStorageUploadVerifier()
    private val storage = ObjectStorageVideoStorageAdapter(
        properties = ObjectStorageProperties(
            bucket = "replus-dev-videos",
            publicBaseUrl = "https://cdn.example.dev/videos/",
        ),
        uploadSigner = signer,
        uploadVerifier = verifier,
    )

    @Test
    fun `upload target is delegated to object storage signer`() {
        // given
        val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"
        val expiresAt = Instant.parse("2026-05-24T09:25:00Z")

        // when
        val target = storage.createUploadTarget(
            objectKey = objectKey,
            contentType = "video/webm",
            expiresAt = expiresAt,
            maxFileSizeBytes = 15_000_000,
        )

        // then
        assertThat(signer.recordedCommand).isEqualTo(
            PresignPutObjectCommand(
                bucket = "replus-dev-videos",
                objectKey = objectKey,
                contentType = "video/webm",
                expiresAt = expiresAt,
                maxFileSizeBytes = 15_000_000,
            ),
        )
        assertThat(target.uploadUrl).isEqualTo("https://object-storage.example.dev/signed-upload")
        assertThat(target.method).isEqualTo("PUT")
        assertThat(target.objectKey).isEqualTo(objectKey)
        assertThat(target.requiredHeaders).containsEntry("Content-Type", "video/webm")
        assertThat(target.expiresAt).isEqualTo(expiresAt)
        assertThat(target.maxFileSizeBytes).isEqualTo(15_000_000)
    }

    @Test
    fun `upload target canonicalizes signed content type header`() {
        // given
        signer.nextHeaders = mapOf(
            "content-type" to "video/webm",
            "x-amz-checksum-crc32" to "checksum",
        )

        // when
        val target = storage.createUploadTarget(
            objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
            contentType = "video/webm",
            expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
            maxFileSizeBytes = 15_000_000,
        )

        // then
        assertThat(target.requiredHeaders).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "Content-Type" to "video/webm",
                "x-amz-checksum-crc32" to "checksum",
            ),
        )
    }

    @Test
    fun `playback and thumbnail urls use public base url`() {
        // given
        val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"

        // when
        val playbackUrl = storage.playbackUrl(objectKey)
        val thumbnailUrl = storage.thumbnailUrl(objectKey)

        // then
        assertThat(playbackUrl).isEqualTo("https://cdn.example.dev/videos/$objectKey")
        assertThat(thumbnailUrl).isEqualTo("https://cdn.example.dev/videos/$objectKey")
    }

    @Test
    fun `upload verification is delegated to object storage verifier`() {
        // given
        val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"

        // when
        val verification = storage.verifyUploadedObject(
            objectKey = objectKey,
            expectedContentType = "video/webm",
            expectedFileSizeBytes = 842120,
        )

        // then
        assertThat(verifier.recordedCommand).isEqualTo(
            VerifyUploadedObjectCommand(
                bucket = "replus-dev-videos",
                objectKey = objectKey,
                expectedContentType = "video/webm",
                expectedFileSizeBytes = 842120,
            ),
        )
        assertThat(verification.exists).isTrue
        assertThat(verification.contentType).isEqualTo("video/webm")
        assertThat(verification.fileSizeBytes).isEqualTo(842120)
    }

    private class RecordingObjectStorageUploadSigner : ObjectStorageUploadSigner {
        var recordedCommand: PresignPutObjectCommand? = null
        var nextHeaders: Map<String, String>? = null

        override fun presignPutObject(command: PresignPutObjectCommand): PresignedPutObject {
            recordedCommand = command
            return PresignedPutObject(
                uploadUrl = "https://object-storage.example.dev/signed-upload",
                requiredHeaders = nextHeaders ?: mapOf(
                    "Content-Type" to command.contentType,
                    "x-amz-meta-max-file-size" to command.maxFileSizeBytes.toString(),
                ),
            )
        }
    }

    private class RecordingObjectStorageUploadVerifier : ObjectStorageUploadVerifier {
        var recordedCommand: VerifyUploadedObjectCommand? = null

        override fun verifyUploadedObject(command: VerifyUploadedObjectCommand): VerifiedUploadedObject {
            recordedCommand = command
            return VerifiedUploadedObject(
                exists = true,
                contentType = command.expectedContentType,
                fileSizeBytes = command.expectedFileSizeBytes,
            )
        }
    }
}
