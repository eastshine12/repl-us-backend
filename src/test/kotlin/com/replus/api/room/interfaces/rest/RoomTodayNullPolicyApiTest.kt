package com.replus.api.room.interfaces.rest

import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.mission.domain.repository.MissionRepository
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
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
class RoomTodayNullPolicyApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var missionRepository: MissionRepository

    @Test
    fun `내 방 목록은 오늘 미션이 없으면 과거 미션 날짜와 today null을 구분한다`() {
        val pastMissionDate = savePastMission()

        mockMvc.perform(
            get("/api/me")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms[0].id").value(ROOM_ID))
            .andExpect(jsonPath("$.rooms[0].lastMissionDate").value(pastMissionDate.toString()))
            .andExpect(jsonPath("$.rooms[0].today").value(nullValue()))
    }

    @Test
    fun `방 상세는 오늘 미션이 없으면 today null을 내려준다`() {
        savePastMission()

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(ROOM_ID))
            .andExpect(jsonPath("$.today").value(nullValue()))
    }

    private fun savePastMission(): LocalDate {
        val missionDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1)
        missionRepository.save(
            Mission(
                id = UUID.randomUUID(),
                roomId = UUID.fromString(ROOM_ID),
                missionDate = missionDate,
                prompt = "어제의 작은 발견은?",
                category = MissionCategory.OBSERVATION,
                editCount = 0,
                editedByMemberId = null,
                editedAt = null,
                createdAt = Instant.parse("2026-05-24T09:15:00Z"),
            ),
        )
        return missionDate
    }

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    }
}
