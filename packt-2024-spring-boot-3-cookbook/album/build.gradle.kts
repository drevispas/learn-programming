plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("io.freefair.lombok") version "8.6"
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // just "wiremock" without "standalone" causes an error "Jetty 11 is not present and no suitable HttpServerFactory extension was found."
    // Spring Boot uses Jetty 12 but just wiremock depends on Jetty 11 so that we use "wiremock-standalone" instead of "wiremock"
//    testImplementation("org.wiremock:wiremock:3.9.0")
    implementation("org.wiremock:wiremock-standalone:3.9.0")
    // Connect to Eureka
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    // Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.112.Final:osx-aarch_64")
    // Spring cloud config starter
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Bridge between Micrometer and OpenTelemetry
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    // Exporter from OpenTelemetry to Zipkin
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    // Micrometer tracing
    implementation("io.micrometer:micrometer-tracing")
    // OpenFeign does not propagate distributed tracing headers by default so that we need to add this dependency
    implementation("io.github.openfeign:feign-micrometer")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
