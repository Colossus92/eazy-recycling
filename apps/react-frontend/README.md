# React + TypeScript + Vite Template

A highly opinionated project template for efficient and consistent development. 
This template is designed to help you kickstart your projects with best practices and a clean, organized setup.

---

## Frameworks & Tools

- **Vite**: Lightning-fast build tool and development server.
- **React**: A powerful library for building user interfaces.
- **TypeScript**: Type-safe development for scalable, maintainable projects.
- **Tailwind CSS**: Utility-first CSS framework for rapid UI development.
- **ESLint**: Enforces code quality and consistency.
- **Prettier**: Opinionated code formatter for consistent styling.
- **Supabase**  Postgres database, Authentication, instant APIs, Edge Functions, Realtime subscriptions, Storage

---

## Folder Structure

We follow the folder structure as mentioned in the [bullet-proof-react](https://github.com/alan2207/bulletproof-react/blob/master/docs/project-structure.md) repo

## 🛠️ Getting Started

### Prerequisites

Make sure you have the following installed:
	•	Node.js (>= 22.x)
	•	npm or yarn

### Installation
1.	Clone the repository:
```bash
git clone <repository-url> your-project-name
cd your-project-name
```
2. Install dependencies
```bash
npm install
```
---
### Development
Start the development server:
```bash
npm run dev
```
This will launch the application at http://localhost:5173.

---
### Build for production
Build the project for production:
```bash
npm run build
```

Preview the production build:
```bash
npm run preview
```
---
### Linting and Formatting
Run ESLint:
```bash
npm run lint
```
Run Prettier:
```bash
npm run format
```
---

### Configuration

Create a .env.local file in the root of your project with the following contents:

```env
VITE_SUPABASE_URL=<your-supabase-url>
VITE_SUPABASE_ANON_KEY=<your-supabase-anon-key>
```

Replace `<your-supabase-url>` and `<your-supabase-anon-key>` with the values from your Supabase project settings.

---

## Test Policy

### 1. Static Check

- **TypeScript**: `strict: true` enabled, no `any` unless justified
- **ESLint**: Rules enforced for hooks, exhaustive deps, and no-floating-promises
- **Pre-commit**: `lint-staged` runs `tsc --noEmit`, `eslint`, and `prettier`

### 2. Unit Tests

- **What to test**: Currency/weight math, parsing, mappers, permission checks, complex hook logic that's pure
- **How**: Vitest; no React render if not needed
- **Don't**: Snapshot massive objects; assert behavior and edge cases instead

### 3. Component/Integration Tests

- **What to test**: Components/pages with real providers (router, QueryClient, theme, auth)
- **How**: React Testing Library (RTL) to test user behavior: "fill, click, sees X", not internal state
- **HTTP mocking**: Mock service layer methods (not MSW) since we use OpenAPI-generated clients; return realistic payloads and failure cases
- **Form testing**: Assert validation messages, disabled/enabled buttons, submit payload shape
- **Guideline**: One test per user story (e.g., "User can create invoice"), including happy path + 1–2 failure states

### 4. End-to-End Tests

- **Tool**: Playwright against deployed preview or local full stack
- **Scope**: Keep count small and stable—login, create/edit core entity, checkout/payment (if applicable)
- **Reliability**: Capture trace on failure, retry flaky tests once, mark E2E as required in CI for main