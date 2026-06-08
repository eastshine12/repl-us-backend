package com.replus.api.mission.application

import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.mission.domain.model.MissionReleaseState
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionResponseStatus
import com.replus.api.mission.domain.model.VideoAsset
import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.MissionRepository
import com.replus.api.mission.domain.repository.MissionResponseRepository
import com.replus.api.mission.domain.repository.VideoAssetRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Sql(
    statements = [
        "delete from response_comments",
        "delete from response_reactions",
        "delete from mission_release_states",
        "delete from mission_responses",
        "delete from video_assets",
        "delete from missions",
    ],
)
class MissionLifecycleServiceTest {
    @Autowired
    private lateinit var missionLifecycleService: MissionLifecycleService

    @Autowired
    private lateinit var missionRepository: MissionRepository

    @Autowired
    private lateinit var missionResponseRepository: MissionResponseRepository

    @Autowired
    private lateinit var videoAssetRepository: VideoAssetRepository

    @Autowired
    private lateinit var missionReleaseStateRepository: MissionReleaseStateRepository

    @Test
    fun `cutoff 이전의 미완료 mission을 실패 처리한다`() {
        val mission = createMission(LocalDate.parse("2026-05-23"))
        createResponse(mission.id, MINA_MEMBER_ID)

        val result = missionLifecycleService.failIncompleteMissionsBefore(LocalDate.parse("2026-05-24"))

        assertThat(result.failedMissionIds).containsExactly(mission.id)
        val releaseState = missionReleaseStateRepository.findByMissionId(mission.id)
        assertThat(releaseState).isNotNull
        assertThat(releaseState!!.failedAt).isNotNull
        assertThat(releaseState.allSubmittedAt).isNull()
        assertThat(releaseState.releaseScheduledAt).isNull()
        assertThat(releaseState.releasedAt).isNull()
    }

    @Test
    fun `완료된 mission과 cutoff 당일 mission은 실패 처리하지 않는다`() {
        val completedMission = createMission(LocalDate.parse("2026-05-22"))
        createResponse(completedMission.id, MINA_MEMBER_ID)
        createResponse(completedMission.id, JOON_MEMBER_ID)
        createResponse(completedMission.id, ARA_MEMBER_ID)

        val cutoffDayMission = createMission(LocalDate.parse("2026-05-24"))
        createResponse(cutoffDayMission.id, MINA_MEMBER_ID)

        val result = missionLifecycleService.failIncompleteMissionsBefore(LocalDate.parse("2026-05-24"))

        assertThat(result.failedMissionIds).isEmpty()
        assertThat(missionReleaseStateRepository.findByMissionId(completedMission.id)).isNull()
        assertThat(missionReleaseStateRepository.findByMissionId(cutoffDayMission.id)).isNull()
    }

    @Test
    fun `release 예정 시간이 지난 mission을 공개 처리한다`() {
        val dueMission = createMission(LocalDate.parse("2026-05-24"))
        val futureMission = createMission(LocalDate.parse("2026-05-25"))
        val failedMission = createMission(LocalDate.parse("2026-05-26"))
        saveReleaseState(
            missionId = dueMission.id,
            releaseScheduledAt = Instant.parse("2026-05-24T09:15:00Z"),
        )
        saveReleaseState(
            missionId = futureMission.id,
            releaseScheduledAt = Instant.parse("2026-05-24T09:25:00Z"),
        )
        saveReleaseState(
            missionId = failedMission.id,
            releaseScheduledAt = Instant.parse("2026-05-24T09:15:00Z"),
            failedAt = Instant.parse("2026-05-24T09:16:00Z"),
        )

        val result = missionLifecycleService.releaseDueMissions(Instant.parse("2026-05-24T09:20:00Z"))

        assertThat(result.releasedMissionIds).containsExactly(dueMission.id)
        assertThat(missionReleaseStateRepository.findByMissionId(dueMission.id)!!.releasedAt)
            .isEqualTo(Instant.parse("2026-05-24T09:20:00Z"))
        assertThat(missionReleaseStateRepository.findByMissionId(futureMission.id)!!.releasedAt).isNull()
        assertThat(missionReleaseStateRepository.findByMissionId(failedMission.id)!!.releasedAt).isNull()
    }

    private fun createMission(missionDate: LocalDate): Mission =
        missionRepository.save(
            Mission(
                id = UUID.randomUUID(),
                roomId = ROOM_ID,
                missionDate = missionDate,
                prompt = "오늘의 3초를 남겨줘",
                category = MissionCategory.OBSERVATION,
                editCount = 0,
                editedByMemberId = null,
                editedAt = null,
                createdAt = Instant.parse("2026-05-23T09:00:00Z"),
            ),
        )

    private fun createResponse(missionId: UUID, memberId: UUID): MissionResponse {
        val videoAsset = videoAssetRepository.save(
            VideoAsset(
                id = UUID.randomUUID(),
                objectKey = "rooms/$ROOM_ID/missions/$missionId/members/$memberId.webm",
                contentType = "video/webm",
                fileSizeBytes = 842120,
                durationSeconds = 3,
                hasAudio = true,
                width = 720,
                height = 1280,
                thumbnailObjectKey = null,
                createdAt = Instant.parse("2026-05-23T09:15:00Z"),
            ),
        )
        return missionResponseRepository.save(
            MissionResponse(
                id = UUID.randomUUID(),
                roomId = ROOM_ID,
                missionId = missionId,
                memberId = memberId,
                videoAssetId = videoAsset.id,
                status = MissionResponseStatus.ACTIVE,
                createdAt = Instant.parse("2026-05-23T09:15:05Z"),
                deletedAt = null,
            ),
        )
    }

    private fun saveReleaseState(
        missionId: UUID,
        releaseScheduledAt: Instant,
        failedAt: Instant? = null,
    ): MissionReleaseState =
        missionReleaseStateRepository.save(
            MissionReleaseState(
                missionId = missionId,
                allSubmittedAt = Instant.parse("2026-05-24T09:14:00Z"),
                releaseScheduledAt = releaseScheduledAt,
                releasedAt = null,
                failedAt = failedAt,
            ),
        )

    private companion object {
        val ROOM_ID: UUID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
        val MINA_MEMBER_ID: UUID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1")
        val JOON_MEMBER_ID: UUID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2")
        val ARA_MEMBER_ID: UUID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3")
    }
}
