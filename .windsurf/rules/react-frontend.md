---
trigger: always_on
---

---
description: React standards and API integration
globs: "**/*.{ts,tsx}"
---
# Frontend Standards

## Component Design
- Reuse existing components from `src/components/ui` before creating new ones.
- Style: Match existing Tailwind/CSS modules patterns.
- Logic: Use custom hooks for complex logic.

## API Integration
- **Client Generation**: Never manually write fetch calls. Use the generated Axios client from OpenAPI.
- **Data Fetching**: Wrap generated clients in React Query hooks (if applicable) or standard service layers.