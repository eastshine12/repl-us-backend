package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

class StorageConfigTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(StorageConfig::class.java)

    private val objectStorageContextRunner = ApplicationContextRunner()
        .withUserConfiguration(StorageConfig::class.java, FakeObjectStorageAccessConfig::class.java)

    private val objectStorageSignerOnlyContextRunner = ApplicationContextRunner()
        .withUserConfiguration(StorageConfig::class.java, FakeObjectStorageSignerConfig::class.java)

    private val objectStorageDefaultVerifierContextRunner = ApplicationContextRunner()
        .withUserConfiguration(
            StorageConfig::class.java,
            ObjectStorageClientConfig::class.java,
            FakeObjectStorageSignerConfig::class.java,
        )

    @Test
    fun `local mode uses configured upload and playback base urls`() {
        contextRunner
            .withPropertyValues(
                "replus.storage.mode=local",
                "replus.storage.local.upload-base-url=https://uploads.example.dev/upload/",
                "replus.storage.local.playback-base-url=https://cdn.example.dev/playback/",
            )
            .run { context ->
                assertThat(context).hasSingleBean(VideoStoragePort::class.java)

                val storage = context.getBean(VideoStoragePort::class.java)
                val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"

                val uploadTarget = storage.createUploadTarget(
                    objectKey = objectKey,
                    contentType = "video/webm",
                    expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
                    maxFileSizeBytes = 15_000_000,
                )

                assertThat(uploadTarget.uploadUrl).isEqualTo("https://uploads.example.dev/upload/$objectKey")
                assertThat(storage.playbackUrl(objectKey)).isEqualTo("https://cdn.example.dev/playback/$objectKey")
            }
    }

    @Test
    fun `object storage mode uses configured signer and public base url`() {
        objectStorageContextRunner
            .withPropertyValues(
                "replus.storage.mode=object-storage",
                "replus.storage.object-storage.bucket=replus-dev-videos",
                "replus.storage.object-storage.public-base-url=https://cdn.example.dev/videos/",
            )
            .run { context ->
                assertThat(context).hasSingleBean(VideoStoragePort::class.java)

                val storage = context.getBean(VideoStoragePort::class.java)
                val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"

                val uploadTarget = storage.createUploadTarget(
                    objectKey = objectKey,
                    contentType = "video/webm",
                    expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
                    maxFileSizeBytes = 15_000_000,
                )

                assertThat(uploadTarget.uploadUrl).isEqualTo(
                    "https://object-storage.example.dev/replus-dev-videos/$objectKey",
                )
                assertThat(storage.playbackUrl(objectKey)).isEqualTo("https://cdn.example.dev/videos/$objectKey")

                val verification = storage.verifyUploadedObject(
                    objectKey = objectKey,
                    expectedContentType = "video/webm",
                    expectedFileSizeBytes = 842120,
                )
                assertThat(verification.exists).isTrue
                assertThat(verification.contentType).isEqualTo("video/webm")
                assertThat(verification.fileSizeBytes).isEqualTo(842120)
            }
    }

    @Test
    fun `object storage mode fails fast when upload signer is missing`() {
        contextRunner
            .withPropertyValues("replus.storage.mode=object-storage")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Object storage upload signer is not configured")
            }
    }

    @Test
    fun `object storage mode fails fast when upload verifier is missing`() {
        objectStorageSignerOnlyContextRunner
            .withPropertyValues(
                "replus.storage.mode=object-storage",
                "replus.storage.object-storage.bucket=replus-dev-videos",
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Object storage upload verifier is not configured")
            }
    }

    @Test
    fun `object storage mode creates default upload verifier from object storage client config`() {
        objectStorageDefaultVerifierContextRunner
            .withPropertyValues(
                "replus.storage.mode=object-storage",
                "replus.storage.object-storage.bucket=replus-dev-videos",
                "replus.storage.object-storage.region=auto",
                "replus.storage.object-storage.endpoint=https://object-storage.example.dev",
            )
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(VideoStoragePort::class.java)
                assertThat(context).hasSingleBean(ObjectStorageUploadVerifier::class.java)
                assertThat(context.getBean(ObjectStorageUploadVerifier::class.java))
                    .isInstanceOf(S3ObjectStorageUploadVerifier::class.java)
            }
    }

    @Test
    fun `object storage mode fails fast when bucket is blank`() {
        objectStorageContextRunner
            .withPropertyValues(
                "replus.storage.mode=object-storage",
                "replus.storage.object-storage.public-base-url=https://cdn.example.dev/videos/",
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Object storage bucket is required")
            }
    }

    @Configuration
    private class FakeObjectStorageAccessConfig {
        @Bean
        fun objectStorageUploadSigner(): ObjectStorageUploadSigner =
            object : ObjectStorageUploadSigner {
                override fun presignPutObject(command: PresignPutObjectCommand): PresignedPutObject =
                    PresignedPutObject(
                        uploadUrl = "https://object-storage.example.dev/${command.bucket}/${command.objectKey}",
                        requiredHeaders = mapOf("Content-Type" to command.contentType),
                    )
            }

        @Bean
        fun objectStorageUploadVerifier(): ObjectStorageUploadVerifier =
            object : ObjectStorageUploadVerifier {
                override fun verifyUploadedObject(command: VerifyUploadedObjectCommand): VerifiedUploadedObject =
                    VerifiedUploadedObject(
                        exists = true,
                        contentType = command.expectedContentType,
                        fileSizeBytes = command.expectedFileSizeBytes,
                    )
            }
    }

    @Configuration
    private class FakeObjectStorageSignerConfig {
        @Bean
        fun objectStorageUploadSigner(): ObjectStorageUploadSigner =
            object : ObjectStorageUploadSigner {
                override fun presignPutObject(command: PresignPutObjectCommand): PresignedPutObject =
                    PresignedPutObject(
                        uploadUrl = "https://object-storage.example.dev/${command.bucket}/${command.objectKey}",
                        requiredHeaders = mapOf("Content-Type" to command.contentType),
                    )
            }
    }
}
