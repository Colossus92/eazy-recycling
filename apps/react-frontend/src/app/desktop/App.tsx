import { lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Provider } from '../Provider.tsx';
import WasteStreamManagement from './pages/WasteStreamManagement.tsx';
import { useMobileHook } from '@/hooks/useMobileHook.ts';
import { Sidebar } from '@/components/layouts/sidebar/Sidebar.tsx';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute.tsx';
import WeightTicketManagement from './pages/WeightTicketManagement.tsx';

const PlanningPage = lazy(() => import('./pages/PlanningPage'));
const CompanyManagement = lazy(() => import('./pages/CompanyManagement'));
const UserManagement = lazy(() => import('./pages/UserManagement'));
const MasterdataManagement = lazy(() => import('./pages/MasterdataManagement.tsx'));
const ProfileManagement = lazy(() => import('./pages/ProfileManagement'));

const DesktopAccessCheck = ({ children }: { children: React.ReactNode }) => {
  const { isMobileSession } = useMobileHook();

  if (isMobileSession) {
    // Immediately redirect to mobile app
    window.location.href = '/mobile';

    return (
      <div className="flex items-center justify-center h-screen w-screen">
        <p className="text-lg">
          U wordt doorgestuurd naar de chauffeur pagina...
        </p>
      </div>
    );
  }
  return <>{children}</>;
};

export const App = () => {
  return (
    <Provider>
      <DesktopAccessCheck>
        <div className="flex items-start h-screen w-screen overflow-hidden pt-2 pb-2 pl-2 bg-color-surface-background">
          <Sidebar />
          <Routes>
            <Route path="/" element={<PlanningPage />} />
            <Route path="/crm" element={<CompanyManagement />} />
            <Route path="/waste-streams" element={<WasteStreamManagement />} />
            <Route path="/weight-tickets" element={<WeightTicketManagement />} />
            <Route
              path="/users"
              element={
                <ProtectedRoute requiredRole="admin">
                  <UserManagement />
                </ProtectedRoute>
              }
            />
            <Route
              path="/masterdata"
              element={
                <ProtectedRoute requiredRole="admin">
                  <MasterdataManagement />
                </ProtectedRoute>
              }
            />
            <Route path="/profile" element={<ProfileManagement />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </DesktopAccessCheck>
    </Provider>
  );
};
