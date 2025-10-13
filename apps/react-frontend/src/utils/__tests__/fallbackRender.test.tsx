import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fallbackRender } from '../fallbackRender';

describe('fallbackRender', () => {
  const mockResetErrorBoundary = vi.fn();
  const mockError = new Error('Test error message');

  beforeEach(() => {
    mockResetErrorBoundary.mockClear();
  });

  it('should render error alert with proper role', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const alertElement = screen.getByRole('alert');
    expect(alertElement).toBeInTheDocument();
  });

  it('should display the error message', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    expect(screen.getByText('Er is iets fout gegaan:')).toBeInTheDocument();
    expect(screen.getByText('Test error message')).toBeInTheDocument();
  });

  it('should render error message in a pre element with red color', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const preElement = screen.getByText('Test error message');
    expect(preElement.tagName).toBe('PRE');
    expect(preElement).toHaveStyle({ color: 'rgb(255, 0, 0)' });
  });

  it('should render retry button with correct label', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const retryButton = screen.getByRole('button', { name: 'Probeer opnieuw' });
    expect(retryButton).toBeInTheDocument();
  });

  it('should call resetErrorBoundary when retry button is clicked', async () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const retryButton = screen.getByRole('button', { name: 'Probeer opnieuw' });
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(mockResetErrorBoundary).toHaveBeenCalledTimes(1);
    });
  });

  it('should render button with primary variant', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const retryButton = screen.getByRole('button', { name: 'Probeer opnieuw' });
    expect(retryButton).toHaveClass('bg-color-brand-primary');
  });

  it('should handle different error messages', () => {
    const customError = new Error('Custom error message');
    render(
      fallbackRender({
        error: customError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    expect(screen.getByText('Custom error message')).toBeInTheDocument();
  });

  it('should handle empty error message', () => {
    const emptyError = new Error('Test');
    render(
      fallbackRender({
        error: emptyError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const preElement = screen.getByText('Test');
    expect(preElement.tagName).toBe('PRE');
  });

  it('should have proper layout structure', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const container = screen.getByRole('alert');
    expect(container).toHaveClass('flex', 'flex-col', 'items-center');
  });

  it('should maintain accessibility with proper ARIA attributes', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const alertElement = screen.getByRole('alert');
    expect(alertElement).toBeInTheDocument();

    const button = screen.getByRole('button');
    expect(button).toBeInTheDocument();
  });

  it('should handle multiple clicks on retry button', async () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const retryButton = screen.getByRole('button', { name: 'Probeer opnieuw' });

    fireEvent.click(retryButton);
    fireEvent.click(retryButton);
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(mockResetErrorBoundary).toHaveBeenCalledTimes(3);
    });
  });

  it('should render with correct text content structure', () => {
    render(
      fallbackRender({
        error: mockError,
        resetErrorBoundary: mockResetErrorBoundary,
      })
    );

    const container = screen.getByRole('alert');
    expect(container).toHaveTextContent('Er is iets fout gegaan:');
    expect(container).toHaveTextContent('Test error message');
    expect(container).toHaveTextContent('Probeer opnieuw');
  });
});
