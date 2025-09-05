# Kotlin Spring Boot Project Template

This is a reusable template for Kotlin-based Spring Boot projects using Gradle. It includes features like request logging, CORS configuration, and a structured setup to help you get started quickly.

<hr />

## Features
*	Kotlin with Spring Boot for building modern, scalable web applications.
*	Gradle for dependency management and build automation.
*	CORS configuration to manage cross-origin requests.
*	Request and response logging for monitoring API usage.
*	Pre-configured folder structure for clean and maintainable code.
*	Basic setup for global exception handling.
*	Ready-to-use Gradle tasks for development, testing, and production.

<hr />

## Getting Started

Prerequisites
•	Java 21 or higher
•	Kotlin 1.9 or higher
•	Gradle 8.x
•	IDE support for Kotlin (e.g., IntelliJ IDEA or VS Code with Kotlin plugin)

## Installation
1.	Clone the repository:
2.	Navigate to the project directory:
3.	Build the project:
```bash
./gradlew build 
```
4.	Run the application:
```bash
./gradlew bootRun
```
<hr />

## Development
1.	**Start the Development Server:** 
Use this command to start the server. The application will run on http://localhost:8080 by default.
```bash 
./gradlew bootRun
```
2.	**Run Tests:**
Use this command to run all unit tests.
```bash
./gradlew test
```

3.	**Build for Production:**
Use this command to build a production-ready JAR file. The JAR will be located in the build/libs directory.
```bash
./gradlew build 
```
<hr />

## Using Docker

This project includes a Dockerfile to containerize the application, making it easy to deploy and run in any environment.

Prerequisites
* Docker installed on your system. Download it from Docker’s official website.

<hr />

### Build the Docker Image

To build the Docker image for the application, run:

```bash
docker build -t your-image-name .
```

* -t your-image-name: Tags the image with a name for easy reference.
* .: Refers to the current directory where the Dockerfile is located.

### Run the Docker Container

To start the application as a container, use:

```bash
docker run -p 8080:8080 your-image-name
```

* -p 8080:8080: Maps port 8080 on the container to port 8080 on your host machine.
* your-image-name: Name of the image built in the previous step.

The application will be accessible at http://localhost:8080.

### Pushing Docker images to a container registry
To share or deploy your Docker image, you can push it to a container registry like Docker Hub, Amazon ECR, or Google Artifact Registry.

#### 1. Log in to GitHub Container Registry

Authenticate with GitHub Container Registry using your GitHub credentials. Run:

```bash
docker login ghcr.io
```

*	Use your GitHub username as the username.
*	Use a personal access token (PAT) as the password. Ensure the PAT has the write:packages and read:packages scopes.

#### 2. Tag your image
Docker images need to be tagged with the registry URL, your GitHub username (or organization), and repository name before pushing.

For example:
```bash
docker tag your-image-name ghcr.io/your-github-username/your-repository-name:tag
```

* Replace your-github-username with your GitHub username or organization name.
* Replace your-repository-name with the name of the repository.

#### 3. Push your image
Push the tagged image to Github container registry:
```bash
docker push ghcr.io/your-github-username/your-repository-name:tag
``` 


