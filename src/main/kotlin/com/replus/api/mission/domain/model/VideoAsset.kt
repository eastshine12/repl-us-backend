package com.replus.api.mission.domain.model

import java.time.Instant
import java.util.UUID

data class VideoAsset(
    val id: UUID,
    val objectKey: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val hasAudio: Boolean,
    val width: Int?,
    val height: Int?,
    val thumbnailObjectKey: String?,
    val createdAt: Instant,
)
