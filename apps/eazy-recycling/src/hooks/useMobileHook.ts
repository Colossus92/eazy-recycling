import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { isMobile } from 'react-device-detect';
import { useAuth } from '@/components/auth/useAuthHook';

export const useMobileHook = () => {
  const { hasRole, session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const isMobileSession =
    (!hasRole('planner') && !hasRole('admin')) || isMobile;

  useEffect(() => {
    if (
      session &&
      isMobileSession &&
      !location.pathname.startsWith('/mobile')
    ) {
      window.location.href = '/mobile';
    }
  }, [hasRole, navigate, isMobileSession, location.pathname, session]);

  return { isMobileSession };
};
