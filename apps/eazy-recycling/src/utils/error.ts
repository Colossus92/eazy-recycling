// Helper function to extract error message from unknown error
export const getErrorMessage = (
  error: unknown,
  defaultMessage: string
): string => {
  if (error && typeof error === 'object') {
    // Handle axios error structure
    if (
      'response' in error &&
      error.response &&
      typeof error.response === 'object'
    ) {
      const response = error.response as { data?: { message?: string } };
      if (response.data?.message) {
        return response.data.message;
      }
    }
    // Handle standard Error object
    if ('message' in error && typeof error.message === 'string') {
      return error.message;
    }
  }
  return defaultMessage;
};
