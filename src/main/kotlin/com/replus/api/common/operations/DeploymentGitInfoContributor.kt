package com.replus.api.common.operations

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.info.GitProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class DeploymentGitInfoContributor(
    private val environment: Environment,
    private val gitPropertiesProvider: ObjectProvider<GitProperties>,
) : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        val gitProperties = gitPropertiesProvider.ifAvailable
        val commit = linkedMapOf(
            "id" to (
                firstNonUnknownProperty("RENDER_GIT_COMMIT", "REPLUS_BUILD_GIT_COMMIT")
                    ?: gitProperties?.commitId
                    ?: "unknown"
                ),
        )

        val commitTime = firstNonUnknownProperty("REPLUS_BUILD_GIT_COMMIT_TIME")
            ?: gitProperties?.get("commit.time")
        if (commitTime != null) {
            commit["time"] = commitTime
        }

        builder.withDetail(
            "git",
            linkedMapOf(
                "branch" to (
                    firstNonUnknownProperty("RENDER_GIT_BRANCH", "REPLUS_BUILD_GIT_BRANCH")
                        ?: gitProperties?.branch
                        ?: "unknown"
                    ),
                "commit" to commit,
            ),
        )
    }

    private fun firstNonUnknownProperty(vararg names: String): String? =
        names.firstNotNullOfOrNull { name ->
            environment.getProperty(name)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.takeUnless { it == "unknown" }
        }
}
