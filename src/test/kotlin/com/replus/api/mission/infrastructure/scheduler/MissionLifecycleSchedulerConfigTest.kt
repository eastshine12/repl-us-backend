package com.replus.api.mission.infrastructure.scheduler

import com.replus.api.mission.application.MissionLifecycleFailureResult
import com.replus.api.mission.application.MissionLifecycleWorker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class MissionLifecycleSchedulerConfigTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(MissionLifecycleSchedulerConfig::class.java, WorkerConfig::class.java)

    @Test
    fun `scheduler는 기본 설정에서 등록되지 않는다`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(MissionLifecycleScheduler::class.java)
        }
    }

    @Test
    fun `scheduler는 enabled 설정에서 등록된다`() {
        contextRunner
            .withPropertyValues("replus.mission.lifecycle.scheduler.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(MissionLifecycleScheduler::class.java)
            }
    }

    @Configuration
    private class WorkerConfig {
        @Bean
        fun missionLifecycleWorker(): MissionLifecycleWorker =
            MissionLifecycleWorker(
                missionLifecycleFailureUseCase = { _ -> MissionLifecycleFailureResult(failedMissionIds = emptyList()) },
                clock = java.time.Clock.systemUTC(),
                zoneIdValue = "Asia/Seoul",
            )
    }
}
