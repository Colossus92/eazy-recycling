rootProject.name = "eazy-recycling"

// Include the Spring Boot backend as a subproject
include(":apps:springboot-backend")

// Set the project directory for the Spring Boot backend
project(":apps:springboot-backend").projectDir = file("apps/springboot-backend")
