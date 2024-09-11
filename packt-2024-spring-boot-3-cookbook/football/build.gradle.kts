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

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// Bridge between Micrometer and OpenTelemetry
	implementation("io.micrometer:micrometer-tracing-bridge-otel")
	// Exporter from OpenTelemetry to Zipkin
	implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
	// Connect to Eureka
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
	// Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider
	implementation("io.netty:netty-resolver-dns-native-macos:4.1.112.Final:osx-aarch_64")
	// Spring cloud config starter
	implementation("org.springframework.cloud:spring-cloud-starter-config")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
