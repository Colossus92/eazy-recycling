# JobRunr Dashboard

A standalone Spring Boot application for monitoring and administering JobRunr jobs via the JobRunr dashboard.

## Purpose

This application provides a web-based dashboard to:
- Monitor job execution status
- View job history and statistics
- Manage recurring jobs
- Debug failed jobs
- Access job logs and details

## Configuration

### Database Connection

Configure the PostgreSQL database connection using environment variables or a `.env` file:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

### Setup

1. Copy `.env.example` to `.env` in the project root:
   ```bash
   cp .env.example .env
   ```

2. Update the database credentials in `.env`

### Application Settings

The application is configured with:
- **Workers disabled**: This app only provides the dashboard, no job processing
- **Dashboard enabled**: Accessible on port 8000
- **Database type**: SQL (PostgreSQL)
- **Table prefix**: `jobrunr.` (matches the main application schema)

## Running the Application

### Using Gradle

```bash
./gradlew bootRun
```

### Using the JAR

```bash
./gradlew bootJar
java -jar build/libs/jobrunr-dashboard.jar
```

## Accessing the Dashboard

Once the application is running, access the dashboard at:

```
http://localhost:8000
```

## Requirements

- Java 21
- PostgreSQL database with JobRunr tables
- The database should be the same one used by your main application

## Notes

- This application does **not** process jobs - it only provides monitoring capabilities
- Ensure the database connection points to the same database as your job-processing application
- The dashboard will show all jobs from any application using the same JobRunr database
