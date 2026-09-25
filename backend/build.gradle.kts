import java.io.File
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

fun loadDotEnv(file: File): Map<String, String> {
    if (!file.isFile) {
        return emptyMap()
    }
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                val key = line.substring(0, separator).trim().removePrefix("\uFEFF")
                val value = line.substring(separator + 1).trim().removeSurrounding("\"")
                key to value
            }
        }
        .toMap()
}

val repoDotEnv = loadDotEnv(File(rootDir, "../.env"))

group = "com.syncticket"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    exclude("**/*IT.class", "**/*IT$*.class")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    include("**/*IT.class", "**/*IT$*.class")
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn("integrationTest")
}

tasks.named<BootRun>("bootRun") {
    repoDotEnv.forEach { (key, value) -> environment(key, value) }
}
