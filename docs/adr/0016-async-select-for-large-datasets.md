# Async Select Component for Large Datasets

## Context and Problem Statement

The `CompanySelectFormField` component currently loads all companies on mount using `companyService.getAllAsList()`, which fetches up to 1000 records in a single request. As the number of companies grows, this approach becomes problematic:

1. **Performance**: Loading 1000+ records on every form mount is slow and memory-intensive
2. **UX**: Users must wait for all data to load before interacting with the dropdown
3. **Scalability**: The current approach will not scale beyond ~1000 companies

We need a pattern that supports:

- Initial load of a small subset of options
- Server-side search filtering as the user types
- Optional infinite scroll for browsing without typing

## Considered Options

### Option 1: react-select AsyncSelect with debounced search

Use `react-select`'s built-in `AsyncSelect` component which supports:

- `loadOptions` callback triggered on user input
- `defaultOptions` for initial load (can be `true` to load on mount, or an array)
- `cacheOptions` to avoid redundant API calls

**Frontend changes:**

- Replace `Select` with `AsyncSelect` from `react-select/async`
- Implement `loadOptions` function that calls the paginated API with the search query
- Use debouncing (300-500ms) to avoid excessive API calls while typing

**Backend changes:**

- None required - existing `GET /companies?query=...&page=0&size=20` endpoint already supports this

### Option 2: react-select AsyncPaginate (infinite scroll)

Use `react-select-async-paginate` library which extends `AsyncSelect` with:

- Automatic pagination when scrolling to bottom of dropdown
- `loadOptions` receives `page` parameter for fetching next page
- Built-in "load more" detection

**Frontend changes:**

- Add `react-select-async-paginate` dependency
- Implement `loadOptions(inputValue, loadedOptions, { page })` function
- Handle pagination state

**Backend changes:**

- None required - existing paginated endpoint works

### Option 3: Virtualized list with react-window

Use `react-window` for virtualized rendering of large lists:

- Only renders visible items in the DOM
- Requires loading all data upfront (or complex windowed fetching)

**Frontend changes:**

- Add `react-window` dependency
- Custom integration with react-select using `MenuList` component override

**Backend changes:**

- None required

## Decision Outcome

**Chosen option: Option 1 - react-select AsyncSelect with debounced search**

Rationale:

- **No new dependencies**: `react-select` is already installed (v5.10.1) and includes `AsyncSelect`
- **Minimal backend changes**: Existing paginated search endpoint already supports this pattern
- **Proven pattern**: This is the standard approach for async select fields in React applications
- **Good UX**: Users get immediate feedback as they type, with results filtered server-side

Infinite scroll (Option 2) is not recommended because:

- Adds a new dependency
- Users rarely scroll through hundreds of options - they search instead
- More complex implementation for marginal UX benefit

Virtualization (Option 3) is not recommended because:

- Still requires loading all data upfront
- Complex integration with react-select
- Overkill for this use case

## Implementation Details

### Frontend Component Behavior

```typescript
// New AsyncSelectFormField component
import AsyncSelect from 'react-select/async';

const loadOptions = async (inputValue: string) => {
  const response = await companyService.getAll({
    query: inputValue,
    page: 0,
    size: 20,
    role: role,
  });
  return response.content.map(company => ({
    value: company.id,
    label: company.name,
  }));
};

<AsyncSelect
  loadOptions={loadOptions}
  defaultOptions={true}  // Load initial options on mount
  cacheOptions={true}    // Cache results for same queries
  // ... other props
/>
```

### Debouncing

React-select's `AsyncSelect` does not include built-in debouncing. Options:

1. Use `lodash.debounce` or a custom debounce wrapper
2. Use a library like `react-select-async-paginate` which includes debouncing
3. Implement a simple debounce in the `loadOptions` function

Recommended: Use a simple debounce wrapper (no new dependencies):

```typescript
const debouncedLoadOptions = useMemo(
  () => debounce((inputValue: string, callback: (options: Option[]) => void) => {
    loadOptions(inputValue).then(callback);
  }, 300),
  [role]
);
```

### Backend Considerations

The existing `GET /companies` endpoint already supports:

- `query` parameter for search (searches name, city, KVK, VIHB, external code)
- `page` and `size` for pagination
- `role` for filtering by company role

**No backend changes required** for basic async select functionality.

#### Optional Enhancement: Optimized Name-Only Search

If performance becomes an issue with the current search (which searches multiple fields and joins with sync records), consider adding a lightweight endpoint:

```kotlin
@GetMapping("/search")
fun searchCompanies(
  @RequestParam query: String,
  @RequestParam(defaultValue = "20") limit: Int,
  @RequestParam(required = false) role: CompanyRole?
): List<CompanyOption>  // Lightweight DTO with just id and name
```

This would:

- Search only the `name` column with a simple `ILIKE` query
- Return a minimal DTO (id, name) instead of full company details
- Skip the external code join for faster response

### Component API

The new component should maintain backward compatibility:

```typescript
interface AsyncCompanySelectFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  name: Path<TFieldValues>;
  rules: any;
  disabled?: boolean;
  role?: string;
  // New optional props
  debounceMs?: number;        // Default: 300
  pageSize?: number;          // Default: 20
  minInputLength?: number;    // Default: 0 (load on mount)
}
```

### Migration Path

1. Create new `AsyncSelectFormField` component alongside existing `SelectFormField`
2. Update `CompanySelectFormField` to use `AsyncSelectFormField`
3. Test thoroughly with various company counts
4. Consider applying same pattern to other large-dataset selects (e.g., goods, waste streams)

## Pros and Cons of the Options

### Option 1: AsyncSelect with debounced search

**Pros:**

- No new dependencies
- Simple implementation
- Backend already supports it
- Standard pattern, well-documented

**Cons:**

- No infinite scroll (users must type to find options beyond initial load)
- Requires manual debounce implementation

### Option 2: AsyncPaginate (infinite scroll)

**Pros:**

- Infinite scroll for browsing
- Built-in debouncing
- Good for very large datasets

**Cons:**

- New dependency
- More complex implementation
- Rarely needed - users typically search rather than scroll

### Option 3: Virtualized list

**Pros:**

- Handles very large lists efficiently
- Smooth scrolling

**Cons:**

- Still loads all data upfront
- Complex integration
- Overkill for this use case

## More Information

- [react-select AsyncSelect documentation](https://react-select.com/async)
- [react-select-async-paginate](https://github.com/vtaits/react-select-async-paginate)
- Current implementation: `apps/react-frontend/src/components/ui/form/CompanySelectFormField.tsx`
- Backend endpoint: `apps/springboot-backend/src/main/kotlin/.../controller/company/CompanyController.kt`
