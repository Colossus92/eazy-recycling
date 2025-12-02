---
description: DB migration
auto_execution_mode: 1
---

description: Updates Supabase schema via MCP
steps:
  - step: "Use the Supabase MCP tool to fetch the current database schema."
  - step: "Compare current schema with the new JPA Entities."
  - step: "Write a SQL migration script to reconcile the differences."
  - step: "Ask user for confirmation to apply the migration script."