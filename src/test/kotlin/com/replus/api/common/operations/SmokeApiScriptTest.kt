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

        assertThat(result.exitCode).isEqualTo(0)
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
            "/actuator/health/readiness",
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

    private fun startFakeApi(receivedAuthorizationHeaders: MutableList<String?>): String {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server = httpServer

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
            exchange.respond("""{"accessToken":"smoke-token","tokenType":"Bearer","user":{"id":"smoke-user"}}""")
        }
        httpServer.createContext("/api/me") { exchange ->
            receivedAuthorizationHeaders += exchange.requestHeaders.getFirst("Authorization")
            exchange.respond("""{"user":{"id":"smoke-user"},"rooms":[]}""")
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

    private fun HttpExchange.respond(body: String) {
        responseHeaders.add("Content-Type", "application/json")
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        sendResponseHeaders(200, bytes.size.toLong())
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
