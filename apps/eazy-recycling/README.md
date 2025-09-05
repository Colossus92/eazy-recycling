# React + TypeScript + Vite Template

A highly opinionated project template for efficient and consistent development. 
This template is designed to help you kickstart your projects with best practices and a clean, organized setup.

---

## Frameworks & Tools

- **Vite**: Lightning-fast build tool and development server.
- **React**: A powerful library for building user interfaces.
- **TypeScript**: Type-safe development for scalable, maintainable projects.
- **Tailwind CSS**: Utility-first CSS framework for rapid UI development.
- **Tailwind UI**: Pre-built, accessible components for beautiful UI designs.
- **ESLint**: Enforces code quality and consistency.
- **Prettier**: Opinionated code formatter for consistent styling.
- **Husky**: Git hooks for pre-commit linting and formatting.
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
### Git Hooks (Husky)

Pre-commit hooks ensure code quality:
•	Linting: Automatically checks for linting issues.
•	Formatting: Automatically formats code before committing.

To install Husky hooks:
```bash
npx husky install
```

## Tailwind CSS & Tailwind UI
* Modify tailwind.config.js to customize the theme or extend default configurations.
* Use pre-built components from [Tailwind UI](https://tailwindui.com/components), subscription required.

## Supabase integration
Supabase is an open-source backend-as-a-service that provides a suite of tools to build scalable and secure applications. It is often referred to as an open-source alternative to Firebase. Supabase offers the following key features:
* Database: A PostgreSQL database with instant APIs. 
* Authentication: User management and authentication using email, social logins, and more. 
* Storage: Scalable file storage for images, videos, and other assets. 
* Edge Functions: Serverless functions for custom backend logic. 
* Real-time Updates: Live data synchronization for apps needing real-time functionality.

### Supabase in this template
This React template comes pre-integrated with Supabase for user authentication and session management. It uses an AuthProvider to manage authentication throughout the app, making it easy to:
* Log users in and out.
* Access the current user session anywhere in the application.
* Display a login screen for unauthenticated users.


### Configuration

To set up Supabase for this template, follow these steps:

### 1. Create a Supabase Project
1.	Go to Supabase.
2.	Create a new project and take note of the API URL and Anon Key from your project settings.

### 2. Add Environment Variables
Create a .env.local file in the root of your project with the following contents:
```
VITE_SUPABASE_URL=<your-supabase-url>
VITE_SUPABASE_ANON_KEY=<your-supabase-anon-key>
```
Replace <your-supabase-url> and <your-supabase-anon-key> with the values from your Supabase project settings.
## Contributing

Contributions are welcome! Feel free to submit a pull request or open an issue for suggestions or bug reports.