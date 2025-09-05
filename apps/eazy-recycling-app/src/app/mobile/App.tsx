import { lazy } from 'react';
import { Route, Routes, Navigate } from 'react-router-dom';
import { ErrorBoundary } from 'react-error-boundary';
import { MobileSignaturePage } from './pages/MobileSignaturePage.tsx';
import { MobileReportFinishedPage } from './pages/MobileReportFinishedPage.tsx';
import { Provider } from '@/app/Provider.tsx';
import { fallbackRender } from '@/utils/fallbackRender';

const MobilePlanningPage = lazy(() => import('./pages/MobilePlanningPage.tsx'));
const MobileTransportDetailsPage = lazy(
  () => import('./pages/MobileTransportDetailsPage.tsx')
);

export const App = () => {
  const refresh = () => {
    window.location.reload();
  };

  return (
    <Provider>
      <div className="flex flex-col h-screen w-screen overflow-hidden bg-color-surface-backround">
        <div className="flex-1 flex flex-col items-start overflow-y-auto">
          <ErrorBoundary fallbackRender={fallbackRender} onError={refresh}>
            <Routes>
              <Route path="/mobile" element={<MobilePlanningPage />} />
              <Route
                path="/mobile/transport/:id"
                element={<MobileTransportDetailsPage />}
              />
              <Route
                path="/mobile/signature/:id/:type"
                element={<MobileSignaturePage />}
              />
              <Route
                path="/mobile/report-ready/:id"
                element={<MobileReportFinishedPage />}
              />
              <Route
                path="/mobile/*"
                element={<Navigate to="/mobile" replace />}
              />
            </Routes>
          </ErrorBoundary>
        </div>
      </div>
    </Provider>
  );
};
