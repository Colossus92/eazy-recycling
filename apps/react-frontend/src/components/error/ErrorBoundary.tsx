import { Component, ErrorInfo, ReactNode } from 'react';
import { toastService } from '@/components/ui/toast/toastService';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  showToast?: boolean;
}

interface State {
  hasError: boolean;
  error?: Error;
}

/**
 * Error Boundary component that catches errors in the React component tree
 * and optionally displays them as toast notifications.
 * 
 * Usage:
 * ```tsx
 * <ErrorBoundary>
 *   <YourComponent />
 * </ErrorBoundary>
 * ```
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error boundary caught an error:', error, errorInfo);
    
    if (this.props.showToast !== false) {
      toastService.error(
        error.message || 'Er is een onverwachte fout opgetreden'
      );
    }
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      
      return (
        <div className="flex items-center justify-center p-4">
          <div className="text-center">
            <h2 className="text-xl font-semibold text-red-600 mb-2">
              Er is iets misgegaan
            </h2>
            <p className="text-gray-600">
              {this.state.error?.message || 'Een onverwachte fout is opgetreden'}
            </p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
