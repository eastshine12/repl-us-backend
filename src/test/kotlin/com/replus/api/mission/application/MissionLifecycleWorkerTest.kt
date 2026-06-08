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
            clock = Clock.fixed(Instant.parse("2026-05-24T15:30:00Z"), ZoneOffset.UTC),
            zoneIdValue = "Asia/Seoul",
        )

        worker.failIncompleteMissionsBeforeToday()

        assertThat(useCase.requestedCutoffDates).containsExactly(LocalDate.parse("2026-05-25"))
    }

    private class RecordingMissionLifecycleFailureUseCase : MissionLifecycleFailureUseCase {
        val requestedCutoffDates = mutableListOf<LocalDate>()

        override fun failIncompleteMissionsBefore(cutoffDate: LocalDate): MissionLifecycleFailureResult {
            requestedCutoffDates += cutoffDate
            return MissionLifecycleFailureResult(failedMissionIds = emptyList())
        }
    }
}
