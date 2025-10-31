# Global Error Handling

This folder contains components for handling errors globally in the application.

## Components

### GlobalErrorHandler

A global error handler that catches **uncaught errors** and **unhandled promise rejections** across the entire application and displays them as toast notifications.

**Features:**

- Catches all unhandled promise rejections (e.g., failed API calls without `.catch()`)
- Catches all uncaught JavaScript errors
- Extracts meaningful error messages from Axios errors (status codes, response messages)
- Displays errors using the existing `toastService`
- Logs errors to the console for debugging

**Usage:**

Already integrated at the top level in both `desktop/main.tsx` and `mobile/main.tsx`.

### ErrorBoundary

A React Error Boundary that catches errors in the React component tree during rendering, lifecycle methods, or constructors.

**Features:**

- Catches errors that occur during React rendering
- Displays a fallback UI when an error occurs
- Shows toast notifications for caught errors
- Can be customized with a custom fallback component

**Usage:**

Already integrated at the top level wrapping the `<App />` component.

You can also wrap specific components for more granular error handling:

```tsx
<ErrorBoundary fallback={<div>Custom error UI</div>}>
  <YourComponent />
</ErrorBoundary>
```

## Error Message Extraction

The `GlobalErrorHandler` intelligently extracts error messages from different sources:

1. **Axios errors**: Extracts from `response.data.message`, `response.data.error`, or status text
2. **JavaScript Error objects**: Uses the `message` property
3. **String errors**: Uses the string directly
4. **Unknown errors**: Shows a generic error message in Dutch

## Example Scenarios

### Uncaught Promise Rejection (Your Case)

When you double-click a row and get:

```text
AxiosError {message: 'Request failed with status code 400', ...}
```

The `GlobalErrorHandler` will:

1. Catch the unhandled promise rejection
2. Extract the error message from the response data or status
3. Display it as a toast notification
4. Log the full error to the console

### React Component Error

If a component throws an error during render:

```tsx
const BuggyComponent = () => {
  throw new Error('Something went wrong!');
  return <div>Hello</div>;
};
```

The `ErrorBoundary` will:

1. Catch the error
2. Show a fallback UI
3. Display a toast notification
4. Log the error to the console

## Debugging

All errors are still logged to the console with full details, so you can:

- View the stack trace
- Inspect the error object
- Debug the root cause

The user just sees a user-friendly toast message.
