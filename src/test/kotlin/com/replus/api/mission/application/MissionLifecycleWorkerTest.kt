package com.replus.api.mission.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MissionLifecycleWorkerTest {
    @Test
    fun `worker는 서비스에 오늘 이전 mission 실패 처리를 요청한다`() {
        val useCase = RecordingMissionLifecycleFailureUseCase()
        val worker = MissionLifecycleWorker(
            missionLifecycleFailureUseCase = useCase,
            missionLifecycleReleaseUseCase = RecordingMissionLifecycleReleaseUseCase(),
            clock = Clock.fixed(Instant.parse("2026-05-24T15:30:00Z"), ZoneOffset.UTC),
            zoneIdValue = "Asia/Seoul",
        )

        worker.failIncompleteMissionsBeforeToday()

        assertThat(useCase.requestedCutoffDates).containsExactly(LocalDate.parse("2026-05-25"))
    }

    @Test
    fun `worker는 서비스에 release 예정 시간이 지난 mission 공개 처리를 요청한다`() {
        val useCase = RecordingMissionLifecycleReleaseUseCase()
        val worker = MissionLifecycleWorker(
            missionLifecycleFailureUseCase = RecordingMissionLifecycleFailureUseCase(),
            missionLifecycleReleaseUseCase = useCase,
            clock = Clock.fixed(Instant.parse("2026-05-24T09:20:00Z"), ZoneOffset.UTC),
            zoneIdValue = "Asia/Seoul",
        )

        worker.releaseDueMissions()

        assertThat(useCase.requestedDueInstants).containsExactly(Instant.parse("2026-05-24T09:20:00Z"))
    }

    private class RecordingMissionLifecycleFailureUseCase : MissionLifecycleFailureUseCase {
        val requestedCutoffDates = mutableListOf<LocalDate>()

        override fun failIncompleteMissionsBefore(cutoffDate: LocalDate): MissionLifecycleFailureResult {
            requestedCutoffDates += cutoffDate
            return MissionLifecycleFailureResult(failedMissionIds = emptyList())
        }
    }

    private class RecordingMissionLifecycleReleaseUseCase : MissionLifecycleReleaseUseCase {
        val requestedDueInstants = mutableListOf<Instant>()

        override fun releaseDueMissions(dueAt: Instant): MissionLifecycleReleaseResult {
            requestedDueInstants += dueAt
            return MissionLifecycleReleaseResult(releasedMissionIds = emptyList())
        }
    }
}
