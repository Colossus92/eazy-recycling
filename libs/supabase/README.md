# Eazy Recycling Supabase Project

This repository contains the Supabase backend for the Eazy Recycling application. It includes database migrations, configuration, and setup for the Supabase services.

## Prerequisites

- [Node.js](https://nodejs.org/) (v14 or newer)
- [npm](https://www.npmjs.com/) or [yarn](https://yarnpkg.com/)
- [Docker](https://www.docker.com/) (for running Supabase locally)

## Installing Supabase CLI

The Supabase CLI is a tool that helps you develop your Supabase project locally and deploy it to the Supabase platform.

### Installation Options

#### Using npm

```bash
npm install -g supabase
```

#### Using yarn

```bash
yarn global add supabase
```

#### Using Homebrew (macOS)

```bash
brew install supabase/tap/supabase
```

#### Using other methods

For other installation methods, refer to the [official Supabase CLI documentation](https://supabase.com/docs/guides/cli).

## Working with Supabase CLI

### Starting the Local Development Environment

To start all Supabase services locally:

```bash
supabase start
```

This command starts a local Supabase stack with PostgreSQL, API, Studio, and other services as defined in your `config.toml` file.

### Stopping the Local Environment

To stop all Supabase services:

```bash
supabase stop
```

### Accessing Supabase Studio

Once the local environment is running, you can access Supabase Studio at:

```
http://localhost:54323
```

### Database Reset

To reset your local database (this will delete all data and reapply migrations):

```bash
supabase db reset
```

### Generating Database Types

If you're using TypeScript, you can generate types based on your database schema:

```bash
supabase gen types typescript --local > types/supabase.ts
```

## Working with Migrations

Migrations are SQL scripts that modify your database schema in a controlled way. They are stored in the `supabase/migrations` directory.

### Creating a New Migration

To create a new migration:

```bash
supabase migration new <migration_name>
```

This will create a new timestamped SQL file in the `supabase/migrations` directory.

### Writing Migrations

Edit the generated SQL file to include your schema changes. For example:

```sql
-- Create a new table
CREATE TABLE public.my_new_table (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL DEFAULT now(),
  name text NOT NULL
);

-- Add RLS policies
ALTER TABLE public.my_new_table ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow authenticated users to read my_new_table"
  ON public.my_new_table
  FOR SELECT
  TO authenticated
  USING (true);
```

### Applying Migrations Locally

Migrations are automatically applied when you start your local Supabase instance. You can also apply them manually:

```bash
supabase db push
```

### Verifying Migrations

To verify your migrations without applying them:

```bash
supabase db diff
```

This shows the difference between your local database and what would be applied by your migrations.

## Deploying Migrations to Production

When you're ready to deploy your migrations to a production Supabase project:

1. Link your local project to your remote Supabase project:

```bash
supabase link --project-ref <project-ref>
```

You can find your project reference in the Supabase dashboard.

2. Push your migrations to the remote project:

```bash
supabase db push
```

## Project Structure

- `supabase/migrations/` - Contains all database migrations
- `supabase/config.toml` - Configuration for the local Supabase development environment
- `supabase/seed.sql` - Seed data for development (if enabled in config)

## Best Practices for Migrations

1. **One change per migration**: Each migration should focus on a single logical change to make rollbacks easier.
2. **Test migrations locally**: Always test migrations in your local environment before deploying.
3. **Use descriptive names**: Name migrations descriptively, e.g., `create_users_table` or `add_email_to_profiles`.
4. **Include down migrations**: When possible, include code to revert your changes for easier rollbacks.
5. **Be careful with production data**: Migrations that modify or delete data should be carefully reviewed.

## Troubleshooting

- If you encounter issues with migrations, check the Supabase logs:
  ```bash
  supabase logs
  ```

- For more detailed database logs:
  ```bash
  supabase logs db
  ```

- If you need to reset your local environment completely:
  ```bash
  supabase stop
  supabase start --fresh
  ```

## Additional Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase CLI Reference](https://supabase.com/docs/reference/cli)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
