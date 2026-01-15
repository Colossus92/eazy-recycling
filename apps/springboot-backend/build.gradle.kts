plugins {
  kotlin("jvm") version "2.1.20"
  kotlin("plugin.spring") version "2.1.20"
  id("org.springframework.boot") version "3.5.7"
  id("io.spring.dependency-management") version "1.1.7"
  kotlin("plugin.jpa") version "2.1.20"
  kotlin("kapt") version "2.1.20"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
  id("dev.nx.gradle.project-graph") version ("0.1.4")
  id("org.springdoc.openapi-gradle-plugin") version ("1.9.0")
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
  implementation("org.springframework.boot:spring-boot-starter-cache:$springVersion")
  implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
  implementation("org.springframework.retry:spring-retry:2.0.11")
  implementation("org.springframework:spring-aspects:6.2.2")

  implementation("org.springframework.boot:spring-boot-starter-web-services:$springVersion")
  implementation("org.springframework.boot:spring-boot-starter-security:$springVersion")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:$springVersion")
  implementation("org.jobrunr:jobrunr-spring-boot-3-starter:8.3.1")
  implementation("org.springframework.ws:spring-ws-core:4.0.11")

  // Apache HttpClient for WebServiceTemplate with SSL support
  implementation("org.apache.httpcomponents:httpclient:4.5.14")
  implementation("org.springframework.ws:spring-ws-support:4.0.11")

  // JAXB for SOAP client
  implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.4")
  implementation("org.glassfish.jaxb:jaxb-runtime:4.0.5")
  implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
  implementation("com.sun.xml.ws:jaxws-tools:4.0.3")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.4")
  implementation("jakarta.activation:jakarta.activation-api:2.1.4")
  implementation("com.sun.xml.ws:jaxws-rt:4.0.3")

  // Database
  runtimeOnly("org.postgresql:postgresql:42.7.8")
  runtimeOnly("com.h2database:h2:2.3.232")
  implementation(platform("io.github.jan-tennert.supabase:bom:3.2.6"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:auth-kt")
  implementation("io.github.jan-tennert.supabase:functions-kt")
  implementation("io.github.jan-tennert.supabase:storage-kt")
  // Web
  implementation("wsdl4j:wsdl4j:1.6.3")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate6:2.19.0")
  implementation("io.ktor:ktor-client-cio:3.1.2")
  implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.9.0"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-io-jvm:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  implementation("org.jobrunr:jobrunr-kotlin-2.2-support:8.3.1")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")

  // Logging
  implementation("com.logtail:logback-logtail:0.3.5")

  // Code
  implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.21")

  // Environment variables
  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")

  // CSV parsing for LMA import
  implementation("org.apache.commons:commons-csv:1.14.1")

  // String similarity for fuzzy matching
  implementation("org.apache.commons:commons-text:1.15.0")

  // Mapstruct
  implementation("org.mapstruct:mapstruct:1.6.3")
  kapt("org.mapstruct:mapstruct-processor:1.6.3")

  // Documentation
  @Suppress("GradleDependency")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

  // Testing
  testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  @Suppress("GradleDependency")
  testImplementation("org.springframework.boot:spring-boot-testcontainers:$springVersion")
  testImplementation("org.springframework.security:spring-security-test:6.5.6")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("io.rest-assured:rest-assured")
  testImplementation("org.testcontainers:junit-jupiter:1.21.3")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.10")
  testImplementation("org.junit.platform:junit-platform-launcher")
}

allprojects {
  apply {
    plugin("dev.nx.gradle.project-graph")
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xjsr305=strict",
                  "-opt-in=kotlin.time.ExperimentalTime"
    )
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
    // Use our custom bootRunOpenApi task
    mainClass.set("nl.eazysoftware.eazyrecyclingservice.OpenApiApplicationKt")
    classpath.setFrom(sourceSets["test"].runtimeClasspath, sourceSets["main"].runtimeClasspath)
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

// Create a custom task to run the OpenAPI application with testcontainers (for manual use)
tasks.register<JavaExec>("bootRunOpenApi") {
  group = "application"
  description = "Runs the Spring Boot application with OpenAPI profile and testcontainers"
  classpath = sourceSets["test"].runtimeClasspath + sourceSets["main"].runtimeClasspath
  mainClass.set("nl.eazysoftware.eazyrecyclingservice.OpenApiApplicationKt")
  args("--spring.profiles.active=openapi")

  // Ensure classes are compiled before running
  dependsOn("testClasses")
}

// Ensure test classes and resources are processed before forkedSpringBootRun
tasks.named("forkedSpringBootRun") {
  dependsOn("testClasses", "processTestResources")
}

// Ensure test classes are compiled before generateOpenApiDocs runs
tasks.named("generateOpenApiDocs") {
  dependsOn("testClasses")
}

// Configure WSDL2Java for SOAP client generation
wsdl2java {
  groups {
    register("MeldingService") {
      generatedSourceDir = layout.buildDirectory.dir("generated/sources/wsdl2java-melding")
      wsdlDir = layout.projectDirectory.dir("src/main/resources/amice")
      includes.set(listOf("MeldingService.wsdl"))
      packageName.set("nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding")
    }
    register("ToetsenAfvalstroomnummerService") {
      generatedSourceDir = layout.buildDirectory.dir("generated/sources/wsdl2java-toetsen")
      wsdlDir = layout.projectDirectory.dir("src/main/resources/amice")
      includes.set(listOf("ToetsenAfvalstroomnummerService.wsdl"))
      packageName.set("nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen")
    }
  }
}
