package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.VideoAsset
import java.util.UUID

interface VideoAssetRepository {
    fun save(videoAsset: VideoAsset): VideoAsset

    fun findByObjectKey(objectKey: String): VideoAsset?

    fun findAllByIds(ids: Collection<UUID>): List<VideoAsset>
}
