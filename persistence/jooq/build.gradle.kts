import org.gradle.kotlin.dsl.execution.templateIdFor

plugins {
    java
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    // Flyway
    id("org.flywaydb.flyway") version "10.17.0"
    // Lombok
    id("io.freefair.lombok") version "8.6"
}

group = "org.demo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jooq")
//    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // PostgreSQL runtime
    runtimeOnly("org.postgresql:postgresql")
    // Testcontainers for Spring Boot and PostgreSQL
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postrgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    // Flyway
    implementation("org.flywaydb:flyway-database-postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
