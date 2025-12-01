---
trigger: model_decision
description: When updating the spec.yaml and react client is required to update requests between react-frontend and springboot-backend
---

To update the client code in the frontend and the spec.yaml from the backend, follow these steps:

- make sure springboot-backend is not running by running npx kill-port 8080
- run ./gradlew generateOpenApiDocs from apps/springboot-backend-folder
- verify the required changes are applied to the spec.yaml
- run nx run react-frontend:generate-client from any folder in workspace
- when new endpoints are added or endpoints are removed, update the applicable service in the frontend.