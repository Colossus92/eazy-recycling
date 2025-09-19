rootProject.name = "springboot-backend"

// Include the eba-schema library as a composite build so Gradle can resolve
// the dependency directly from source during CI (no reliance on mavenLocal).
// This also prevents issues where remote caching skips publishToMavenLocal.
includeBuild("../../libs/eba-schema") {
  dependencySubstitution {
    substitute(module("nl.eazysoftware:eba-schema")).using(project(":"))
  }
}
