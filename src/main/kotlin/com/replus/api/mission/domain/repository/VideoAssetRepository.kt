package com.replus.api.mission.domain.repository

import com.replus.api.mission.domain.model.VideoAsset

interface VideoAssetRepository {
    fun save(videoAsset: VideoAsset): VideoAsset
}
