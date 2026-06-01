package com.replus.api.mission.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.model.RoomRole
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class MissionEditPolicyTest {
    private val policy = MissionEditPolicy()

    @Test
    fun `방장이 아니면 오늘 질문을 수정할 수 없다`() {
        // given
        val editor = roomMember(role = RoomRole.MEMBER)
        val mission = mission(editCount = 0)

        // when
        val action = { policy.validateTodayEdit(editor, mission, activeResponseCount = 0) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ROOM_OWNER_REQUIRED)
    }

    @Test
    fun `첫 active 리플이 있으면 방장도 오늘 질문을 수정할 수 없다`() {
        // given
        val editor = roomMember(role = RoomRole.OWNER)
        val mission = mission(editCount = 0)

        // when
        val action = { policy.validateTodayEdit(editor, mission, activeResponseCount = 1) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.MISSION_ALREADY_HAS_RESPONSE)
    }

    @Test
    fun `오늘 질문을 이미 한 번 수정했으면 다시 수정할 수 없다`() {
        // given
        val editor = roomMember(role = RoomRole.OWNER)
        val mission = mission(editCount = 1)

        // when
        val action = { policy.validateTodayEdit(editor, mission, activeResponseCount = 0) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.MISSION_EDIT_LIMIT_REACHED)
    }

    @Test
    fun `방장이며 첫 리플 전이고 수정 이력이 없으면 오늘 질문을 수정할 수 있다`() {
        // given
        val editor = roomMember(role = RoomRole.OWNER)
        val mission = mission(editCount = 0)

        // when
        val action = { policy.validateTodayEdit(editor, mission, activeResponseCount = 0) }

        // then
        assertThatCode { action() }
            .doesNotThrowAnyException()
    }

    private fun roomMember(role: RoomRole): RoomMember =
        RoomMember(
            id = UUID.randomUUID(),
            roomId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            role = role,
            status = RoomMemberStatus.ACTIVE,
            slotIndex = 0,
            joinedAt = Instant.parse("2026-05-24T00:01:00Z"),
            removedAt = null,
        )

    private fun mission(editCount: Int): Mission =
        Mission(
            id = UUID.randomUUID(),
            roomId = UUID.randomUUID(),
            missionDate = LocalDate.parse("2026-05-24"),
            prompt = "요즘 나만 알고 싶은 음료 조합은?",
            category = MissionCategory.OBSERVATION,
            editCount = editCount,
            editedByMemberId = null,
            editedAt = null,
            createdAt = Instant.parse("2026-05-24T00:00:00Z"),
        )
}
