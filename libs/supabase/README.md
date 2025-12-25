# Eazy Recycling Supabase Project

This repository contains the Supabase backend for the Eazy Recycling application. It includes database migrations, configuration, and setup for the Supabase services.

## Reminders when rolling out
1. Enable the auth hook under Authentication > Auth Hooks > select custom_access_token_hook

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
supabase db diff --use-migra -f <migration_name>
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

### Exporting Data

To export data from your local Supabase database to an SQL file:

```bash
supabase db dump --local --data-only -f data.sql
```

This command exports only the data (not the schema) from your local database to a file named `data.sql`.

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

# Workflow: Populating Local Supabase with Production Data

This guide documents how to refresh your local Supabase `public` schema with production data. This method is superior to `supabase db reset` because it **preserves** your local Auth users, Storage buckets, and Edge Function configurations.

## Prerequisites
* Supabase CLI installed and logged in.
* IntelliJ IDEA (Ultimate or Community) with the Database plugin enabled.
* Docker running (hosting the local Supabase instance).

---

## Step 1: Dump Remote Data

Use the Supabase CLI to pull **data only** from your production instance. We target only the `public` schema to avoid messing with Auth/Storage system tables.

Run this in your terminal:

```bash
# Replace <db-url> with your production connection string (from Supabase Dashboard > Settings > Database)
# We use -f to save it to a file
supabase db dump \
  --data-only \
  --schema public \
  -f supabase/prod_data.sql
```

## Step 2: Prepare the Import File
To avoid "Foreign Key Constraint" errors during import (because data might be inserted in the wrong order), we must instruct Postgres to ignore constraints temporarily.

Open supabase/prod_data.sql in IntelliJ and wrap the content with the session_replication_role commands.

Add this to the very top of the file:

```SQL
SET session_replication_role = 'replica';
```
Add this to the very bottom of the file:
```SQL
SET session_replication_role = 'origin';
```


## Step 3: Clean Local Public Schema
Before importing, we must remove existing local data to avoid "Duplicate Key" errors. Instead of dropping the entire schema (which kills permissions), we simply truncate all tables.

  1. Open your Database Tool Window.
  2. Connect to your local Supabase, execute `supabase status` to retrieve the credentials 
  3. Open a New Query Console attached to this connection.
  4. Run this script to truncate all tables in public

```SQL
DO $$
DECLARE
r RECORD;
BEGIN
-- Loop over every table in the public schema
FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
-- Truncate the table and cascade to clear dependent rows
EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' CASCADE;';
END LOOP;
END $$;
```

## Step 4: Run import
In case of using intellij: 

1. In the IntelliJ Project View (file explorer), locate supabase/prod_data.sql. 
2. Right-Click the file.
3. Select Run 'prod_data.sql'... 
4. In the popup, select your Local Supabase connection as the target. 
5. IntelliJ will stream the file directly to the database.

6. Done! Your local database now mirrors production data, while your local Auth and Storage remain untouched.


## Additional Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase CLI Reference](https://supabase.com/docs/reference/cli)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
