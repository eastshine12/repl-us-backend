package com.replus.api.common.operations

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path

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
        assertThat(result.output).contains("--with-guest-auth", "/actuator/health/readiness")
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

    private fun runSmokeScript(vararg arguments: String): ScriptResult {
        val process = ProcessBuilder(listOf("bash", "scripts/smoke-api.sh") + arguments)
            .directory(Path.of(".").toFile())
            .redirectErrorStream(true)
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
}
