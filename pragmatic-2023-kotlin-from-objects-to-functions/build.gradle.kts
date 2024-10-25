plugins {
    kotlin("jvm") version "1.9.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val http4kVersion="5.12.1.0"
val junitVersion="5.10.1"
val junitLauncherVersion="1.10.1"
val pesticideCoreVersion="1.6.6"
val striktVersion="0.34.1"
val slf4jVersion="2.1.0-alpha1"

dependencies {
    // Default assert
//    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-server-jetty:$http4kVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    // Http client for testing
    testImplementation("org.http4k:http4k-client-jetty:$http4kVersion")
    testImplementation("com.ubertob.pesticide:pesticide-core:$pesticideCoreVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitLauncherVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
