plugins {
    java
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "org.demo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.projectlombok:lombok")
    testImplementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // devtools
    implementation("org.springframework.boot:spring-boot-devtools")
    // actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // h2
    runtimeOnly("com.h2database:h2")
    // mariadb
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    // datasource proxy starter
    implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:1.9.2")
    // spring data rest
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
