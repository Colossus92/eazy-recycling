---
trigger: always_on
description: When updating the spec.yaml and react client is required to update requests between react-frontend and springboot-backend
---

---
trigger: model_decision
description: When updating the spec.yaml and react client is required to update requests between react-frontend and springboot-backend
---

**CRITICAL: ALWAYS kill port 8080 FIRST before generating OpenAPI docs. Never skip this step.**

1. FIRST: `npx kill-port 8080` (MANDATORY - do this even if you think the server is not running)
2. Run `./gradlew generateOpenApiDocs` from apps/springboot-backend
3. Verify changes in spec.yaml
4. Run `nx run react-frontend:generate-client`
5. Update applicable frontend services if endpoints changed