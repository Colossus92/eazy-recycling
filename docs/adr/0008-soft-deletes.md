# Soft Deletion Strategy with `deleted_at` and Minimal Snapshots

## Context and Problem Statement

We need to remove master data (e.g., vehicles, carriers, customers, waste types) from day-to-day usage without breaking historical truth or referential integrity for documents like transports, weight tickets, waybills, and invoices. We also want a single, consistent approach that works well with Supabase (Postgres + RLS) and our modular monolith.

Hard deletes create dangling references or force cascading deletes that we do not want. We therefore adopt **soft deletion** using a single nullable `deleted_at` column and, where needed, **small snapshots** on referencing documents to preserve labels that may change over time.

## Considered Options

* **Hard delete** — Physically remove rows and cascade or restrict
* **Soft delete with `is_active` boolean** — Hide via boolean flag
* **Soft delete with `deleted_at` timestamp (chosen)** — Use a single timestamp for lifecycle and RLS, with optional snapshots on documents

## Decision Outcome

Chosen option: **Soft delete with `deleted_at` timestamp and minimal snapshots**.

This decision allows us to:
- Preserve historical truth for documents while letting users retire master data
- Keep foreign keys intact inside the owning context where appropriate (`ON DELETE RESTRICT`)
- Use clean RLS filters and partial unique indexes
- Optionally snapshot a small set of display fields on documents (e.g., vehicle code, carrier name) so PDFs and exports remain stable after data changes

### Related reference
- Supabase guidance on soft deletes (client perspective and RLS): <https://supabase.com/docs/guides/troubleshooting/soft-deletes-with-supabase-js>

## Pros and Cons of the Options

### Hard delete

**Pros:**
- Minimal storage
- Simplest queries when no history is required

**Cons:**
- Breaks historical referential integrity or requires complex archival
- Dangerous under concurrency; accidental data loss is hard to recover
- Incompatible with long-lived documents that reference retired master data

### Soft delete with `is_active` boolean

**Pros:**
- Easy to understand and query

**Cons:**
- Lacks deletion timestamp and audit trail
- Encourages multiple flags (`active`, `archived`, etc.) that drift
- Harder to build partial unique indexes with clear semantics

### Soft delete with `deleted_at` timestamp (chosen)

**Pros:**
- Precise audit: when an entity was retired
- Simple RLS filters and partial indexes (`where deleted_at is null`)
- Supports undelete by clearing the timestamp
- Plays well with snapshots on documents for immutable history

**Cons:**
- Requires discipline in queries and RLS to exclude deleted rows
- Some maintenance for indexes and validations

## How We Apply Soft Deletion

### 1) Column and constraints (pattern)

```sql
-- Example: ops.vehicle
alter table ops.vehicle
  add column if not exists deleted_at timestamptz;

-- Prevent duplicate codes among non-deleted rows
create unique index if not exists ux_vehicle_code_active
  on ops.vehicle(code)
  where deleted_at is null;
```

### 2) Foreign keys and referential integrity

- Use `ON DELETE RESTRICT` for document references (e.g., `ops.transport.vehicle_id → ops.vehicle.id`). This prevents removing a vehicle that is still referenced by a live document.
- Because we **soft delete**, the FK remains valid: we do not delete the row, we only set `deleted_at`.
- For cross-context references (e.g., ops → billing), continue to use **identity values** and **snapshots** rather than cross-schema FKs.

### 3) RLS policies (hide deleted rows by default)

```sql
-- Enable RLS on master data tables
alter table ops.vehicle enable row level security;

-- Typical read policy: non-admin roles only see non-deleted rows
create policy vehicle_read_active_only on ops.vehicle
  for select using (deleted_at is null);

-- Write policies must ensure you cannot update a deleted row (except undelete)
create policy vehicle_update_not_deleted on ops.vehicle
  for update using (deleted_at is null);

-- Admin roles can have broader policies or bypass RLS
```

> Note: Client SDK filters (e.g., Supabase JS) should not be the only guard. Keep the rule in RLS so all access paths respect soft deletion. See the Supabase doc linked above for client examples.

### 4) Application semantics

- **Retire** = `update ... set deleted_at = now()`
- **Undelete** (when allowed) = `update ... set deleted_at = null`
- **Lists/lookup UIs** must filter `deleted_at is null`

### 5) Snapshots on documents (when labels must not drift)

For documents that must render the original labels (PDFs, exports): keep a small JSON or scalar snapshot in addition to the FK/identity.

```sql
-- Example: ops.transport keeps a vehicle code snapshot
alter table ops.transport
  add column if not exists vehicle_snapshot jsonb;

create or replace function ops.set_vehicle_snapshot()
returns trigger as $$
begin
  if new.vehicle_id is null then
    new.vehicle_snapshot := null;
  else
    select jsonb_build_object('id', v.id, 'code', v.code, 'name', v.name)
      into new.vehicle_snapshot
    from ops.vehicle v where v.id = new.vehicle_id;
  end if;
  return new;
end;$$ language plpgsql;

create trigger trg_transport_vehicle_snapshot
before insert or update of vehicle_id on ops.transport
for each row execute function ops.set_vehicle_snapshot();
```

### 6) Unique constraints among active rows

Use **partial unique indexes** so users can reuse codes after soft deletion:

```sql
create unique index if not exists ux_customer_number_active
  on crm.customer(customer_number)
  where deleted_at is null;
```

### 7) Guard updates after deletion

Optionally prevent any updates to deleted rows except an explicit undelete:

```sql
create or replace function guard_updates_on_deleted()
returns trigger as $$
begin
  if old.deleted_at is not null and (new.deleted_at is not null) and new is distinct from old then
    raise exception 'Cannot update a soft-deleted row (undelete first)';
  end if;
  return new;
end;$$ language plpgsql;

create trigger t_guard_updates_on_deleted
before update on ops.vehicle
for each row execute function guard_updates_on_deleted();
```

### 8) Testing (pgTAP)

Add tests to ensure policies and indexes behave:

```sql
select plan(3);

select lives_ok($$
  insert into ops.vehicle(id, code, name) values (gen_random_uuid(),'TR-001','Truck A');
$$, 'insert active vehicle');

select throws_ok($$
  insert into ops.vehicle(id, code, name) values (gen_random_uuid(),'TR-001','Truck B');
$$, '23505', 'duplicate active code rejected');

-- retire and reuse code
update ops.vehicle set deleted_at = now() where code = 'TR-001';
select lives_ok($$
  insert into ops.vehicle(id, code, name) values (gen_random_uuid(),'TR-001','Truck C');
$$, 'reuse code after soft delete');

select * from finish();
```

## Migration Guidance

1. Add `deleted_at` to all master data tables (vehicles, carriers, customers, locations, materials, containers, etc.).
2. Create partial unique indexes for human-readable codes that must be unique among active rows.
3. Add RLS policies to hide deleted rows for non-admins.
4. Update repositories/queries to always filter `deleted_at is null` for active lists.
5. Add snapshot columns/triggers only on documents that require immutable labels (transport, waybill, invoice).
6. Document UI semantics (retire/restore) and ensure audit logging as needed.

## Consequences

- We keep strong integrity and history without complex archival flows.
- Deleting master data no longer breaks documents.
- There is minor overhead in policies and indexes, and developers must remember to filter by `deleted_at is null` where appropriate.

## More Information

- Supabase doc on soft deletes (client examples and caveats): <https://supabase.com/docs/guides/troubleshooting/soft-deletes-with-supabase-js+>
- Our RLS policy patterns live with migrations in each context schema (`ops`, `billing`, `compliance`).
