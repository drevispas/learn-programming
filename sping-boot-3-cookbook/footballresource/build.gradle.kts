plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("io.freefair.lombok") version "8.6"
    //
    id("org.flywaydb.flyway") version "10.17.0"
}

group = "org.demo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.postgresql:postgresql")
    // Bridge between Micrometer and OpenTelemetry
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    // Exporter from OpenTelemetry to Zipkin
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")
    // Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Eureka client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // Spring data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Testcontainers for PostgreSQL
    testImplementation("org.testcontainers:postgresql")
    // Junit Jupiter for Testcontainers
    testImplementation("org.testcontainers:junit-jupiter")
    // Below dependency doesn't work with Spring Boot 3.3.2
//    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
