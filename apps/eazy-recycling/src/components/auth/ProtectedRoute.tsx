import { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@/components/auth/useAuthHook.ts';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredRole: string;
  redirectPath?: string;
}

export const ProtectedRoute = ({
  children,
  requiredRole,
  redirectPath = '/',
}: ProtectedRouteProps) => {
  const { hasRole } = useAuth();

  if (!hasRole(requiredRole)) {
    return <Navigate to={redirectPath} replace />;
  }

  return <>{children}</>;
};
