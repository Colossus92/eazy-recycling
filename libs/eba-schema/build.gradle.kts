plugins {
  kotlin("jvm") version "2.1.20"
  id("org.unbroken-dome.xjc") version "2.0.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
  id("dev.nx.gradle.project-graph") version("0.1.4")
  `maven-publish`
}

group = "nl.eazysoftware"
version = "0.0.1"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Only dependencies needed for the generated schema classes
  implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
  implementation("org.glassfish.jaxb:jaxb-runtime:4.0.3")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.10")
  testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

xjc {
  xjcVersion.set("3.0")
  srcDirName.set("${projectDir}/src/main/resources/schema/")
  strictCheck.set(false)
  extension.set(true)
}

// Ensure generated sources are included in the jar
tasks.withType<Jar> {
  from(sourceSets.main.get().output)
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Configure the compilation order properly
tasks.named("compileJava") {
  dependsOn("xjcGenerate")
}

// Configure Maven publishing
publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])

      groupId = "nl.eazysoftware"
      artifactId = "eba-schema"
      version = version
    }
  }
}

// Ensure build task publishes to local repository
tasks.named("build") {
  finalizedBy("publishToMavenLocal")
}

