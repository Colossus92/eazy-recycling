import { render } from '@testing-library/react';
import { GlobalErrorHandler } from './GlobalErrorHandler';
import { toastService } from '@/components/ui/toast/toastService';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

// Mock the toast service
vi.mock('@/components/ui/toast/toastService', () => ({
  toastService: {
    error: vi.fn(),
    success: vi.fn(),
    warn: vi.fn(),
  },
}));

describe('GlobalErrorHandler', () => {
  let unhandledRejectionHandler: ((event: PromiseRejectionEvent) => void) | undefined;
  let errorHandler: ((event: ErrorEvent) => void) | undefined;

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    
    // Capture the event listeners
    const originalAddEventListener = window.addEventListener;
    vi.spyOn(window, 'addEventListener').mockImplementation((type, handler) => {
      if (type === 'unhandledrejection') {
        unhandledRejectionHandler = handler as (event: PromiseRejectionEvent) => void;
      } else if (type === 'error') {
        errorHandler = handler as (event: ErrorEvent) => void;
      }
      return originalAddEventListener.call(window, type, handler as EventListener);
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should render without crashing', () => {
    const { container } = render(<GlobalErrorHandler />);
    expect(container).toBeTruthy();
  });

  it('should handle unhandled promise rejection with Axios error', () => {
    render(<GlobalErrorHandler />);

    const axiosError = {
      isAxiosError: true,
      message: 'Request failed',
      response: {
        data: {
          message: 'Bad request',
        },
        status: 400,
        statusText: 'Bad Request',
      },
    };

    const event = new Event('unhandledrejection') as PromiseRejectionEvent;
    Object.defineProperty(event, 'reason', { value: axiosError });
    Object.defineProperty(event, 'preventDefault', { value: vi.fn() });

    if (unhandledRejectionHandler) {
      unhandledRejectionHandler(event);
    }

    expect(toastService.error).toHaveBeenCalledWith('Bad request');
  });

  it('should handle unhandled promise rejection with Error object', () => {
    render(<GlobalErrorHandler />);

    const error = new Error('Something went wrong');
    const event = new Event('unhandledrejection') as PromiseRejectionEvent;
    Object.defineProperty(event, 'reason', { value: error });
    Object.defineProperty(event, 'preventDefault', { value: vi.fn() });

    if (unhandledRejectionHandler) {
      unhandledRejectionHandler(event);
    }

    expect(toastService.error).toHaveBeenCalledWith('Something went wrong');
  });

  it('should handle uncaught errors', () => {
    render(<GlobalErrorHandler />);

    const error = new Error('Test error');
    const event = new ErrorEvent('error', {
      error,
      message: 'Test error',
    });
    Object.defineProperty(event, 'preventDefault', { value: vi.fn() });

    if (errorHandler) {
      errorHandler(event);
    }

    expect(toastService.error).toHaveBeenCalledWith('Test error');
  });
});
