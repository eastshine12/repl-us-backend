import java.time.Instant
import java.util.Properties
import kotlin.io.path.readLines
import kotlin.io.path.readText

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.replus"
version = "0.1.0-SNAPSHOT"
description = "Private daily 3-second video room backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.nimbusds:nimbus-jose-jwt:10.3")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(platform("software.amazon.awssdk:bom:2.45.1"))
    implementation("software.amazon.awssdk:s3")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

springBoot {
    buildInfo()
}

val generatedGitPropertiesDir = layout.buildDirectory.dir("generated/resources/git")

val generateGitProperties by tasks.registering {
    val outputFile = generatedGitPropertiesDir.map { it.file("git.properties") }

    outputs.file(outputFile)
    outputs.upToDateWhen { false }

    doLast {
        fun environment(name: String): String? =
            providers.environmentVariable(name).orNull
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.takeUnless { it == "unknown" }

        fun git(vararg arguments: String): String? =
            try {
                val result = providers.exec {
                    commandLine("git", *arguments)
                    isIgnoreExitValue = true
                }
                result.standardOutput.asText.get().trim().takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }

        fun gitReference(name: String): String? {
            val referenceFile = layout.projectDirectory.file(".git/$name").asFile
            if (referenceFile.isFile) {
                return referenceFile.readText().trim().takeIf { it.isNotBlank() }
            }

            val packedRefsFile = layout.projectDirectory.file(".git/packed-refs").asFile
            if (!packedRefsFile.isFile) {
                return null
            }

            return packedRefsFile.toPath().readLines()
                .asSequence()
                .filterNot { it.startsWith("#") || it.startsWith("^") }
                .mapNotNull { line ->
                    val parts = line.split(" ", limit = 2)
                    if (parts.size == 2 && parts[1] == name) parts[0] else null
                }
                .firstOrNull()
        }

        fun gitHead(): Pair<String?, String?> {
            val headFile = layout.projectDirectory.file(".git/HEAD").asFile
            if (!headFile.isFile) {
                return null to null
            }

            val head = headFile.readText().trim()
            if (!head.startsWith("ref: ")) {
                return null to head.takeIf { it.isNotBlank() }
            }

            val reference = head.removePrefix("ref: ").trim()
            val branchName = reference.removePrefix("refs/heads/").takeIf { it != reference }
            return branchName to gitReference(reference)
        }

        val (fileBranch, fileCommitId) = gitHead()
        val commitId = environment("REPLUS_BUILD_GIT_COMMIT")
            ?: environment("RENDER_GIT_COMMIT")
            ?: environment("GITHUB_SHA")
            ?: git("rev-parse", "HEAD")
            ?: fileCommitId
            ?: "unknown"
        val branch = environment("REPLUS_BUILD_GIT_BRANCH")
            ?: environment("RENDER_GIT_BRANCH")
            ?: environment("GITHUB_REF_NAME")
            ?: git("rev-parse", "--abbrev-ref", "HEAD")
            ?: fileBranch
            ?: "unknown"
        val commitTime = environment("REPLUS_BUILD_GIT_COMMIT_TIME")
            ?: git("show", "-s", "--format=%cI", "HEAD")
            ?: Instant.now().toString()
        val abbreviatedCommitId = commitId
            .takeUnless { it == "unknown" }
            ?.take(7)
            ?: "unknown"

        val properties = Properties().apply {
            setProperty("git.branch", branch)
            setProperty("git.commit.id", commitId)
            setProperty("git.commit.id.abbrev", abbreviatedCommitId)
            setProperty("git.commit.time", commitTime)
        }

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.outputStream().use { properties.store(it, "Generated by Gradle") }
    }
}

sourceSets {
    main {
        resources.srcDir(generatedGitPropertiesDir)
    }
}

tasks.named("processResources") {
    dependsOn(generateGitProperties)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
