---
description: Generate OpenAPI client code for React frontend from Spring Boot backend
---

# Generate React Client from OpenAPI

Follow these steps **exactly in order**:

// turbo
1. Kill any existing process on port 8080:
   ```bash
   npx kill-port 8080
   ```

// turbo
2. Generate OpenAPI docs from Spring Boot backend:
   ```bash
   cd apps/springboot-backend && ./gradlew generateOpenApiDocs --info
   ```

3. Verify the required changes are applied to `apps/react-frontend/src/api/spec.yaml`

// turbo
4. Generate the React client:
   ```bash
   npx nx run react-frontend:generate-client
   ```

5. When new endpoints are added or removed, update the applicable service in the frontend.

**IMPORTANT**: Step 1 (kill port 8080) MUST always be executed first, even if you think the server is not running. This ensures fresh OpenAPI docs are generated.
