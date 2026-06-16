package com.replus.api.common.operations

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SmokeApiScriptTest {
    private var server: HttpServer? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    @Test
    fun `smoke script checks health endpoints and optional guest auth flow`() {
        val receivedAuthorizationHeaders = mutableListOf<String?>()
        val fakeApi = startFakeApi(receivedAuthorizationHeaders)

        val result = runSmokeScript("--with-guest-auth", fakeApi)

        assertThat(result.exitCode)
            .withFailMessage(result.output)
            .isEqualTo(0)
        assertThat(result.output).contains(
            "liveness: ok",
            "readiness: ok",
            "info: ok",
            "guest auth: ok",
            "current user: ok",
        )
        assertThat(receivedAuthorizationHeaders).containsExactly("Bearer smoke-token")
    }

    @Test
    fun `smoke script checks social auth endpoint rejects invalid tokens when requested`() {
        val receivedAuthorizationHeaders = mutableListOf<String?>()
        val receivedSocialLoginBodies = mutableListOf<String>()
        val fakeApi = startFakeApi(
            receivedAuthorizationHeaders = receivedAuthorizationHeaders,
            receivedSocialLoginBodies = receivedSocialLoginBodies,
        )

        val result = runSmokeScript("--with-social-auth-failure", fakeApi)

        assertThat(result.exitCode)
            .withFailMessage(result.output)
            .isEqualTo(0)
        assertThat(result.output).contains(
            "liveness: ok",
            "readiness: ok",
            "info: ok",
            "social auth rejection: ok",
        )
        assertThat(receivedSocialLoginBodies).containsExactly(
            """{"provider":"GOOGLE","providerToken":"smoke-invalid-token"}""",
        )
    }

    @Test
    fun `smoke script checks social auth success flow when provider token is configured`() {
        val receivedAuthorizationHeaders = mutableListOf<String?>()
        val receivedSocialLoginBodies = mutableListOf<String>()
        val fakeApi = startFakeApi(
            receivedAuthorizationHeaders = receivedAuthorizationHeaders,
            receivedSocialLoginBodies = receivedSocialLoginBodies,
        )

        val result = runSmokeScript(
            "--with-social-auth-success",
            fakeApi,
            environment = mapOf(
                "SMOKE_SOCIAL_AUTH_PROVIDER" to "apple",
                "SMOKE_SOCIAL_AUTH_TOKEN" to "real-social-token",
            ),
        )

        assertThat(result.exitCode)
            .withFailMessage(result.output)
            .isEqualTo(0)
        assertThat(result.output).contains(
            "liveness: ok",
            "readiness: ok",
            "info: ok",
            "social auth: ok",
            "social current user: ok",
        )
        assertThat(result.output).doesNotContain("real-social-token")
        assertThat(receivedSocialLoginBodies).containsExactly(
            """{"provider":"APPLE","providerToken":"real-social-token"}""",
        )
        assertThat(receivedAuthorizationHeaders).containsExactly("Bearer smoke-social-token")
    }

    @Test
    fun `smoke script requires a social auth token for success check`() {
        val receivedAuthorizationHeaders = mutableListOf<String?>()
        val fakeApi = startFakeApi(receivedAuthorizationHeaders)

        val result = runSmokeScript("--with-social-auth-success", fakeApi)

        assertThat(result.exitCode).isEqualTo(64)
        assertThat(result.output).contains("SMOKE_SOCIAL_AUTH_TOKEN")
    }

    @Test
    fun `smoke script checks room invite and today mission flow when requested`() {
        val receivedAuthorizationHeaders = mutableListOf<String?>()
        val receivedDisplayNames = mutableListOf<String>()
        val fakeApi = startFakeApi(receivedAuthorizationHeaders, receivedDisplayNames)

        val result = runSmokeScript("--with-room-flow", fakeApi)

        assertThat(result.exitCode)
            .withFailMessage(result.output)
            .isEqualTo(0)
        assertThat(result.output).contains(
            "liveness: ok",
            "readiness: ok",
            "info: ok",
            "guest auth: ok",
            "current user: ok",
            "room create: ok",
            "room detail: ok",
            "invite link: ok",
            "invite join: ok",
            "today mission: ok",
            "mission update: ok",
            "member cleanup: ok",
        )
        assertThat(receivedAuthorizationHeaders).contains(
            "Bearer smoke-token",
            "Bearer smoke-member-token",
        )
        assertThat(receivedDisplayNames).allSatisfy { displayName ->
            assertThat(displayName).hasSizeLessThanOrEqualTo(24)
        }
    }

    @Test
    fun `smoke script cleans up smoke room when cleanup token is configured`() {
        val receivedAuthorizationHeaders = mutableListOf<String?>()
        val receivedDisplayNames = mutableListOf<String>()
        val receivedOperationsTokens = mutableListOf<String?>()
        val fakeApi = startFakeApi(
            receivedAuthorizationHeaders = receivedAuthorizationHeaders,
            receivedDisplayNames = receivedDisplayNames,
            receivedOperationsTokens = receivedOperationsTokens,
        )

        val result = runSmokeScript(
            "--with-room-flow",
            fakeApi,
            environment = mapOf("SMOKE_CLEANUP_TOKEN" to "cleanup-secret"),
        )

        assertThat(result.exitCode)
            .withFailMessage(result.output)
            .isEqualTo(0)
        assertThat(result.output).contains("room cleanup: ok")
        assertThat(receivedOperationsTokens).containsExactly("cleanup-secret")
    }

    @Test
    fun `smoke script prints usage when base url is missing`() {
        val result = runSmokeScript()

        assertThat(result.exitCode).isEqualTo(64)
        assertThat(result.output).contains("Usage:", "API_BASE_URL")
    }

    @Test
    fun `smoke script exposes help`() {
        val result = runSmokeScript("--help")

        assertThat(result.exitCode).isEqualTo(0)
        assertThat(result.output).contains(
            "--with-guest-auth",
            "--with-room-flow",
            "--with-social-auth-failure",
            "--with-social-auth-success",
            "/actuator/health/readiness",
            "/api/auth/social",
            "/api/rooms/{roomId}/today",
            "SMOKE_CLEANUP_TOKEN",
            "SMOKE_SOCIAL_AUTH_TOKEN",
            "SMOKE_CURL_TIMEOUT_SECONDS",
            "SMOKE_RETRY_ATTEMPTS",
        )
    }

    @Test
    fun `smoke script allows curl timeout to be configured for cold starts`() {
        val fakeCurl = createFakeCurl()

        val result = runSmokeScript(
            "https://api.example.test",
            environment = mapOf(
                "PATH" to "${fakeCurl.binDir.absolutePathString()}:${System.getenv("PATH")}",
                "SMOKE_CURL_TIMEOUT_SECONDS" to "180",
            ),
        )

        assertThat(result.exitCode).isEqualTo(0)
        assertThat(fakeCurl.callsLog.readText()).contains("--max-time 180")
        assertThat(fakeCurl.callsLog.readText()).doesNotContain("--max-time 20")
    }

    @Test
    fun `smoke script retries transient request failures`() {
        val fakeCurl = createFakeCurl(readinessFailuresBeforeSuccess = 1)

        val result = runSmokeScript(
            "https://api.example.test",
            environment = mapOf(
                "PATH" to "${fakeCurl.binDir.absolutePathString()}:${System.getenv("PATH")}",
                "SMOKE_RETRY_ATTEMPTS" to "2",
                "SMOKE_RETRY_DELAY_SECONDS" to "0",
            ),
        )

        assertThat(result.exitCode).isEqualTo(0)
        assertThat(result.output).contains("liveness: ok", "readiness: ok", "info: ok")
        assertThat(fakeCurl.readinessCount.readText().trim()).isEqualTo("2")
    }

    private fun startFakeApi(
        receivedAuthorizationHeaders: MutableList<String?>,
        receivedDisplayNames: MutableList<String> = mutableListOf(),
        receivedOperationsTokens: MutableList<String?> = mutableListOf(),
        receivedSocialLoginBodies: MutableList<String> = mutableListOf(),
    ): String {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server = httpServer
        var guestSessionCount = 0
        val roomId = "11111111-1111-4111-8111-111111111111"
        val memberId = "22222222-2222-4222-8222-222222222222"
        val missionId = "33333333-3333-4333-8333-333333333333"
        val inviteCode = "SMOKE2026"

        httpServer.createContext("/actuator/health/liveness") { exchange ->
            exchange.respond("""{"status":"UP"}""")
        }
        httpServer.createContext("/actuator/health/readiness") { exchange ->
            exchange.respond("""{"status":"UP"}""")
        }
        httpServer.createContext("/actuator/info") { exchange ->
            exchange.respond("""{"app":{"name":"repl.us backend","version":"0.1.0-SNAPSHOT"}}""")
        }
        httpServer.createContext("/api/auth/guest") { exchange ->
            assertThat(exchange.requestMethod).isEqualTo("POST")
            val requestBody = exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)
            Regex(""""displayName"\s*:\s*"([^"]+)"""")
                .find(requestBody)
                ?.groupValues
                ?.get(1)
                ?.let { receivedDisplayNames += it }
            guestSessionCount += 1
            val token = if (guestSessionCount == 1) "smoke-token" else "smoke-member-token"
            exchange.respond("""{"accessToken":"$token","tokenType":"Bearer","user":{"id":"smoke-user-$guestSessionCount"}}""")
        }
        httpServer.createContext("/api/auth/social") { exchange ->
            assertThat(exchange.requestMethod).isEqualTo("POST")
            val requestBody = exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)
            receivedSocialLoginBodies += requestBody
            if (requestBody.contains("real-social-token")) {
                exchange.respond(
                    """{"accessToken":"smoke-social-token","tokenType":"Bearer","user":{"id":"social-user"}}""",
                    status = 201,
                )
            } else {
                exchange.respond(
                    """{"title":"Unauthorized","status":401,"code":"UNAUTHENTICATED"}""",
                    status = 401,
                )
            }
        }
        httpServer.createContext("/api/me") { exchange ->
            receivedAuthorizationHeaders += exchange.requestHeaders.getFirst("Authorization")
            exchange.respond("""{"user":{"id":"smoke-user"},"rooms":[]}""")
        }
        httpServer.createContext("/api/rooms") { exchange ->
            val path = exchange.requestURI.path
            receivedAuthorizationHeaders += exchange.requestHeaders.getFirst("Authorization")
            when {
                exchange.requestMethod == "POST" && path == "/api/rooms" -> exchange.respond(
                    """
                    {
                      "id":"$roomId",
                      "name":"Smoke Room",
                      "memberCount":1,
                      "maxMembers":6,
                      "ownerMemberId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                      "currentUserMemberId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                      "currentUserRole":"OWNER",
                      "members":[],
                      "createdAt":"2026-06-12T17:00:00Z",
                      "today":null
                    }
                    """.trimIndent(),
                )
                exchange.requestMethod == "GET" && path == "/api/rooms/$roomId" -> exchange.respond(
                    """
                    {
                      "id":"$roomId",
                      "name":"Smoke Room",
                      "memberCount":1,
                      "maxMembers":6,
                      "currentUserRole":"OWNER",
                      "members":[],
                      "createdAt":"2026-06-12T17:00:00Z",
                      "today":null
                    }
                    """.trimIndent(),
                )
                exchange.requestMethod == "POST" && path == "/api/rooms/$roomId/invite-links" -> exchange.respond(
                    """
                    {
                      "code":"$inviteCode",
                      "roomId":"$roomId",
                      "url":"https://example.test/invite/$inviteCode",
                      "expiresAt":"2026-06-12T18:00:00Z",
                      "maxUses":1,
                      "uses":0
                    }
                    """.trimIndent(),
                )
                exchange.requestMethod == "GET" && path == "/api/rooms/$roomId/today" -> exchange.respond(
                    """
                    {
                      "serverDate":"2026-06-12",
                      "room":{"id":"$roomId","name":"Smoke Room","memberCount":2,"maxMembers":6,"currentUserRole":"OWNER"},
                      "mission":{"id":"$missionId","roomId":"$roomId","missionDate":"2026-06-12","prompt":"Look around","category":"OBSERVATION","editCount":0,"canEdit":true,"createdAt":"2026-06-12T17:00:00Z"},
                      "viewer":{"memberId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"OWNER","hasSubmittedToday":false},
                      "participation":{"totalActiveMembers":2,"submittedCount":0,"viewerHasSubmitted":false,"canViewFriendResponses":false,"allSubmitted":false},
                      "responses":[],
                      "todayFrames":[],
                      "growthPreview":[]
                    }
                    """.trimIndent(),
                )
                exchange.requestMethod == "PATCH" && path == "/api/rooms/$roomId/missions/$missionId" -> exchange.respond(
                    """
                    {
                      "id":"$missionId",
                      "roomId":"$roomId",
                      "missionDate":"2026-06-12",
                      "prompt":"Smoke prompt",
                      "category":"OBSERVATION",
                      "editCount":1,
                      "canEdit":false,
                      "createdAt":"2026-06-12T17:00:00Z"
                    }
                    """.trimIndent(),
                )
                exchange.requestMethod == "DELETE" && path == "/api/rooms/$roomId/members/$memberId" -> exchange.respond(
                    """{"memberId":"$memberId","status":"REMOVED","removedAt":"2026-06-12T17:05:00Z"}""",
                )
                else -> exchange.respond("unexpected room request: ${exchange.requestMethod} $path", status = 404)
            }
        }
        httpServer.createContext("/api/invite-links/$inviteCode/join") { exchange ->
            receivedAuthorizationHeaders += exchange.requestHeaders.getFirst("Authorization")
            exchange.respond(
                """
                {
                  "id":"$roomId",
                  "name":"Smoke Room",
                  "memberCount":2,
                  "maxMembers":6,
                  "ownerMemberId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                  "currentUserMemberId":"$memberId",
                  "currentUserRole":"MEMBER",
                  "members":[],
                  "createdAt":"2026-06-12T17:00:00Z",
                  "today":null
                }
                """.trimIndent(),
            )
        }
        httpServer.createContext("/internal/operations/smoke-rooms/$roomId") { exchange ->
            receivedOperationsTokens += exchange.requestHeaders.getFirst("X-Replus-Operations-Token")
            exchange.respond("""{"roomId":"$roomId","deleted":true}""")
        }
        httpServer.start()

        return "http://127.0.0.1:${httpServer.address.port}"
    }

    private fun createFakeCurl(readinessFailuresBeforeSuccess: Int = 0): FakeCurl {
        val directory = Files.createTempDirectory("smoke-api-test")
        val binDir = Files.createDirectory(directory.resolve("bin"))
        val callsLog = directory.resolve("curl-calls.log")
        val readinessCount = directory.resolve("readiness-count")
        callsLog.writeText("")
        readinessCount.writeText("0")

        val curlPath = binDir.resolve("curl")
        curlPath.writeText(
            """
            #!/usr/bin/env bash
            set -euo pipefail

            printf '%s\n' "$*" >> "$callsLog"

            url="${'$'}{@: -1}"
            case "${'$'}url" in
              */actuator/health/liveness)
                printf '{"status":"UP"}'
                ;;
              */actuator/health/readiness)
                count="$(cat "$readinessCount")"
                count="${'$'}((count + 1))"
                printf '%s\n' "${'$'}count" > "$readinessCount"
                if [[ "${'$'}count" -le "$readinessFailuresBeforeSuccess" ]]; then
                  echo "simulated cold start" >&2
                  exit 7
                fi
                printf '{"status":"UP"}'
                ;;
              */actuator/info)
                printf '{"app":{"name":"repl.us backend","version":"0.1.0-SNAPSHOT"}}'
                ;;
              */api/auth/guest)
                printf '{"accessToken":"smoke-token","tokenType":"Bearer","user":{"id":"smoke-user"}}'
                ;;
              */api/me)
                printf '{"user":{"id":"smoke-user"},"rooms":[]}'
                ;;
              *)
                echo "unexpected URL: ${'$'}url" >&2
                exit 22
                ;;
            esac
            """.trimIndent(),
        )
        curlPath.toFile().setExecutable(true)

        return FakeCurl(
            binDir = binDir,
            callsLog = callsLog,
            readinessCount = readinessCount,
        )
    }

    private fun runSmokeScript(
        vararg arguments: String,
        environment: Map<String, String> = emptyMap(),
    ): ScriptResult {
        val process = ProcessBuilder(listOf("bash", "scripts/smoke-api.sh") + arguments)
            .directory(Path.of(".").toFile())
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()
        val output = process.inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
        val exitCode = process.waitFor()
        return ScriptResult(exitCode, output)
    }

    private fun HttpExchange.respond(body: String, status: Int = 200) {
        responseHeaders.add("Content-Type", "application/json")
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    private data class ScriptResult(
        val exitCode: Int,
        val output: String,
    )

    private data class FakeCurl(
        val binDir: Path,
        val callsLog: Path,
        val readinessCount: Path,
    )
}
