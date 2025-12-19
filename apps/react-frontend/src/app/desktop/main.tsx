import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@/index.css';
import { App } from './App.tsx';
import 'react-toastify/dist/ReactToastify.css';
import { GlobalErrorHandler } from '@/components/error/GlobalErrorHandler';
import { ErrorBoundary } from '@/components/error/ErrorBoundary';
import { ToastPortal } from '@/components/ui/toast/ToastPortal';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GlobalErrorHandler />
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
    <ToastPortal />
  </StrictMode>
);
