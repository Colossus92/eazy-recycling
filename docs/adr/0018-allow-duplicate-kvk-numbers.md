# ADR-0018: Allow Duplicate KVK Numbers

## Status

Accepted

## Date

2025-12-03

## Context

During Exact Online synchronization, we encountered errors where multiple Exact accounts share the same KVK (Chamber of Commerce) number. Our system enforced a unique constraint on `chamber_of_commerce_id`, causing sync failures with the error:

```text
Query did not return a unique result: 2 results were returned
```

### Root Cause

Exact Online allows multiple accounts to have the same KVK number. This happens in legitimate business scenarios:

- **Branches**: Different branches of the same legal entity
- **Historical data**: Merged or renamed companies
- **Data entry variations**: Same company entered multiple times with slight variations

Our previous approach treated KVK as a unique identifier, which is incorrect. KVK is an **attribute** of a company, not its **identity**. The true identity in our system is:

- For companies created locally: the internal `company_id` (UUID)
- For companies synced from Exact: the `exact_guid`

### Previous Behavior

1. **Database**: Unique constraint on `chamber_of_commerce_id`
2. **Create/Update**: Rejected companies with duplicate KVK numbers
3. **Exact Sync**: Attempted to match incoming accounts by KVK, causing conflicts when multiple Exact accounts shared the same KVK

## Decision

We adopt a "soft validation" approach where KVK duplicates are allowed but flagged for review:

### 1. Remove Database Constraint

Drop the unique constraint on `chamber_of_commerce_id`. Add a non-unique index for query performance.

### 2. Remove Application-Level KVK Validation

Remove KVK duplicate checks from `CreateCompany` and `UpdateCompany` services. VIHB number uniqueness is still enforced as it's a regulatory identifier.

### 3. Simplify Exact Sync Matching

Remove KVK-based matching from `ExactAccountProcessor`. The sync now relies solely on `exact_guid` for matching:

- If `exact_guid` matches an existing sync record → update that company
- Otherwise → create a new company

Each Exact account creates its own company record, linked by `exact_guid`.

## Consequences

### Positive

- **Sync reliability**: Exact Online sync no longer fails on KVK collisions
- **Data fidelity**: Our data mirrors Exact Online's structure accurately
- **Simplified logic**: Removed complex KVK collision handling code
- **Flexibility**: Supports legitimate business structures (holdings, branches)

### Negative

- **Potential duplicates**: Users can create multiple companies with the same KVK
- **Data quality**: Requires manual review to identify true duplicates

### Neutral

- **LMA/Amice integration**: Unaffected. These services validate KVK against the official registry, not our database.
- **Search**: Still works; may return multiple results for the same KVK.

## Future Improvements

### 1. Soft Warning on Create/Update (Recommended)

Instead of blocking, show a warning when a user enters a KVK that already exists:

```text
"Dit KVK-nummer is al gekoppeld aan bedrijf 'X'. Weet u zeker dat u wilt doorgaan?"
```

Allow the user to proceed if they confirm.

### 2. Duplicate Detection Dashboard

Create an admin dashboard showing potential duplicates:

- Companies with the same KVK number
- Companies with similar names and addresses
- Sync records flagged as potential duplicates

### 3. Merge Functionality

Implement a "merge companies" feature allowing admins to:

- Select a primary company
- Merge duplicate records into the primary
- Update all references (waste streams, transports, etc.)
- Archive the duplicate

### 4. Exact Online Deduplication Report

Generate a report of potential duplicates in Exact Online itself, allowing the customer to clean up their source data.

## References

- [Exact Online API Documentation](https://start.exactonline.nl/docs/HlpRestAPIResources.aspx)
- ADR-0012: Exact Online Synchronization Strategy
