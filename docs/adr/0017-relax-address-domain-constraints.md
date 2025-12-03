# Relax Address Domain Constraints for Exact Online Sync

## Context and Problem Statement

Data imported from Exact Online does not always meet the strict validation rules defined in our `Address` domain model. For example:

- Street names may be missing or exceed the 43-character limit
- Cities may be blank or exceed the 24-character limit
- Postal codes may not match the Dutch format (e.g., foreign addresses, incomplete data)
- Building numbers may be blank

Currently, when an account in Exact Online has invalid address data, the sync fails with a domain validation error. This creates a chicken-and-egg problem: we cannot sync the company to fix its address because the address is invalid.

The business requirement is to sync all accounts from Exact Online, even those with incomplete or incorrect address data. Address quality can be improved later when the data is actually used (e.g., when creating a waste stream declaration).

## Considered Options

- **Option 1: Fix all addresses in Exact Online first**
  - Requires manual cleanup of potentially hundreds of records
  - Blocks sync until all data is corrected
  - Not practical given time constraints

- **Option 2: Relax domain constraints, keep controller validation**
  - Remove `require` statements from domain value objects (`StreetName`, `City`, `DutchPostalCode`, `Address`)
  - Keep Jakarta validation annotations in `AddressRequest` for user-created data
  - LMA validation provides a final safety net before waste stream declarations

- **Option 3: Create separate "unvalidated" address type for imports**
  - Adds complexity with two address types
  - Requires mapping between types
  - Overengineered for this use case

## Decision Outcome

Chosen option: **Option 2 - Relax domain constraints, keep controller validation**

This approach:

1. Allows all Exact Online accounts to sync regardless of address quality
2. Maintains data quality for user-created data via controller validation
3. Uses LMA validation as the final safety net before official declarations
4. Follows the principle of "be liberal in what you accept, conservative in what you send"

## Implementation

### Domain Changes

Remove `require` statements from:

- `Address.kt`: Remove building number blank check
- `StreetName.kt`: Remove `isNotBlank` and length checks
- `City.kt`: Remove `isNotBlank` and length checks
- `DutchPostalCode.kt`: Remove regex validation

### Controller Validation (Unchanged)

`AddressRequest.kt` retains Jakarta validation annotations:

- `@NotBlank` on street, buildingNumber, city, country
- `@Pattern` for Dutch postal code format

This ensures user-created data through the API maintains quality standards.

### Validation Flow

1. **Exact Online Sync**: No domain validation → all accounts sync
2. **User creates company via API**: Controller validation → rejects invalid data
3. **User creates waste stream**: LMA validation → ensures declaration-ready data

## Pros and Cons of the Options

### Option 1: Fix all addresses in Exact Online first

- ✅ Data is clean from the start
- ❌ Blocks sync indefinitely
- ❌ Requires significant manual effort
- ❌ Not practical for initial go-live

### Option 2: Relax domain constraints, keep controller validation (Chosen)

- ✅ Enables immediate sync of all accounts
- ✅ Maintains quality for user-created data
- ✅ LMA validation provides safety net
- ✅ Users can fix addresses when needed
- ❌ Domain model is less strict
- ❌ Invalid addresses may exist in database

### Option 3: Create separate "unvalidated" address type

- ✅ Keeps domain model strict
- ❌ Adds complexity
- ❌ Requires type conversions
- ❌ Overengineered for this use case

## More Information

- The LMA (Landelijk Meldpunt Afvalstoffen) validation ensures all waste stream declarations have valid addresses before submission
- Users will see validation errors when attempting to create declarations with invalid addresses, prompting them to fix the data
- This approach prioritizes data availability over data purity, with validation deferred to the point of use
