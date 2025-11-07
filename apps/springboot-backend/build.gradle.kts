plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.spring") version "2.1.20"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.1.20"
    kotlin("kapt") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
    id("dev.nx.gradle.project-graph") version("0.1.4")
    id("org.springdoc.openapi-gradle-plugin") version("1.9.0")
    id("com.github.bjornvester.wsdl2java") version "2.0.2"
}
group = "nl.eazysoftware"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

val springVersion: String by project

dependencies {
    // Spring Boot Core Dependencies
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springVersion")

    implementation("org.springframework.boot:spring-boot-starter-web-services:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-security:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:$springVersion")
    implementation("org.springframework.ws:spring-ws-core:4.0.11")

    // JAXB for SOAP client
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.4")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.5")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
    implementation("com.sun.xml.ws:jaxws-rt:4.0.3")
    // Database
    runtimeOnly("org.postgresql:postgresql:42.7.5")
    runtimeOnly("com.h2database:h2:2.3.232")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4")) {
        (this as ExternalModuleDependency).exclude("org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm")
    }
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    // Web
    implementation("wsdl4j:wsdl4j:1.6.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate6:2.19.0")
    implementation("io.ktor:ktor-client-cio:3.1.2")
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-io-jvm:1.8.0")
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    implementation("org.apache.pdfbox:pdfbox:2.0.30")
    // Code
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

    // EBA Schema Library
    implementation("nl.eazysoftware:eba-schema:0.0.1")

    // Amice SOAP Client Libraries
    implementation("nl.eazysoftware:amice-toetsen-client:0.0.1")

    // Environment variables
  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

    // Mapstruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-testcontainers:$springVersion")
    testImplementation("org.springframework.security:spring-security-test:6.5.0")
    testImplementation("org.testcontainers:postgresql:1.20.5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.testcontainers:junit-jupiter:1.20.5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.10")
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
}

allprojects {
  apply {
    plugin("dev.nx.gradle.project-graph")
  }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

kapt {
    correctErrorTypes = true
}

openApi {
  apiDocsUrl.set("http://localhost:8080/v3/api-docs.yaml")
  outputDir.set(file("../../docs/openapi"))
  outputFileName.set("spec.yaml")
  customBootRun {
    args.set(listOf("--spring.profiles.active=openapi"))
  }
}
tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
    enabled = true
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Configure bootRun to use local profile by default
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    args("--spring.profiles.active=local")
}

// Configure WSDL2Java for SOAP client generation
wsdl2java {
    wsdlDir = layout.projectDirectory.dir("src/main/resources/amice")
    includes = listOf("ToetsenAfvalstroomnummerService.wsdl", "MeldingService.wsdl")
    generatedSourceDir = layout.buildDirectory.dir("generated/sources/wsdl2java")
    packageName = "nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated"
}
