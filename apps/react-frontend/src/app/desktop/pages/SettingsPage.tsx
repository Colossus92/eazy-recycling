import type {
  AuthorizationUrlResponse,
  ConnectionStatusResponse,
  RefreshTokenResponse,
} from '@/api/client';
import { exactOnlineService } from '@/api/services/exactOnlineService.ts';
import { SyncFromExactResponse } from '@/api/client/models/sync-from-exact-response';
import { ContentContainer } from '@/components/layouts/ContentContainer.tsx';
import { Button } from '@/components/ui/button/Button.tsx';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { Tab } from '@/components/ui/tab/Tab';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { ExactSyncConflictsTab } from '@/features/exactsync/components/ExactSyncConflictsTab';
import { LmaImportTab } from '@/features/lmaimport';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ReactNode, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

export const SettingsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [authUrl, setAuthUrl] = useState<string | null>(null);
  const [selectedIndex, setSelectedIndex] = useState(0);

  // Query to get connection status
  const {
    data: connectionStatus,
    isLoading: isLoadingStatus,
    refetch: refetchStatus,
  } = useQuery<ConnectionStatusResponse>({
    queryKey: ['exactOnlineStatus'],
    queryFn: async () => {
      const response = await exactOnlineService.getConnectionStatus();
      return response.data;
    },
    refetchInterval: 5000, // Refresh every 5 seconds to detect auth completion
  });

  // Mutation to get authorization URL
  const getAuthUrlMutation = useMutation({
    mutationFn: async () => {
      const response = await exactOnlineService.getAuthorizationUrl();
      return response.data;
    },
    onSuccess: (data: AuthorizationUrlResponse) => {
      setAuthUrl(data.authorizationUrl);
      toastService.success('Authorization URL generated successfully');
    },
    onError: (error: Error) => {
      toastService.error(`Failed to get authorization URL: ${error.message}`);
    },
  });

  // Mutation to refresh token
  const refreshTokenMutation = useMutation({
    mutationFn: async () => {
      const response = await exactOnlineService.refreshToken();
      return response.data;
    },
    onSuccess: (data: RefreshTokenResponse) => {
      if (data.success) {
        toastService.success(data.message || 'Token refreshed successfully');
        refetchStatus();
      } else {
        toastService.error(data.message || 'Failed to refresh token');
      }
    },
    onError: (error: Error) => {
      toastService.error(`Failed to refresh token: ${error.message}`);
    },
  });

  // Mutation to sync from Exact Online
  const syncFromExactMutation = useMutation({
    mutationFn: async () => {
      const response = await exactOnlineService.syncFromExact();
      return response.data;
    },
    onSuccess: (data: SyncFromExactResponse) => {
      if (data.success) {
        toastService.success(
          `Synchronisatie voltooid: ${data.recordsSynced} records (${data.recordsCreated} nieuw, ${data.recordsUpdated} bijgewerkt)`
        );
      } else {
        toastService.error(data.message || 'Synchronisatie mislukt');
      }
    },
    onError: (error: Error) => {
      toastService.error(`Synchronisatie mislukt: ${error.message}`);
    },
  });

  const handleGetAuthUrl = () => {
    getAuthUrlMutation.mutate();
  };

  const handleOpenAuthUrl = () => {
    if (authUrl) {
      // Redirect in the same window so the OAuth callback can redirect back properly
      window.location.href = authUrl;
    }
  };

  const handleRefreshToken = () => {
    refreshTokenMutation.mutate();
  };

  const handleTestConnection = () => {
    refetchStatus();
    toastService.success('Checking connection status...');
  };

  const handleSyncFromExact = () => {
    syncFromExactMutation.mutate();
  };

  // Handle OAuth callback results from query parameters
  useEffect(() => {
    const exactConnected = searchParams.get('exact_connected');
    const exactError = searchParams.get('exact_error');
    const exactErrorDescription = searchParams.get('exact_error_description');

    if (exactConnected === 'true') {
      toastService.success(
        'Exact Online verbinding succesvol tot stand gebracht!'
      );
      refetchStatus(); // Refresh connection status
      // Clear URL parameters
      setSearchParams({});
    } else if (exactError) {
      const errorMessage = exactErrorDescription
        ? `Exact Online autorisatie mislukt: ${exactError} - ${exactErrorDescription}`
        : `Exact Online autorisatie mislukt: ${exactError}`;
      toastService.error(errorMessage);
      // Clear URL parameters
      setSearchParams({});
    }
  }, [searchParams, refetchStatus, setSearchParams]);

  // Clear auth URL when connection becomes established
  useEffect(() => {
    if (connectionStatus?.connected) {
      setAuthUrl(null);
    }
  }, [connectionStatus?.connected]);

  const tabs: { name: string; component: () => ReactNode }[] = [
    { name: 'Verbinding', component: () => <ExactConnectionTab /> },
    {
      name: 'Sync Conflicten',
      component: () => (
        <ExactSyncConflictsTab key={`conflicts-${selectedIndex}`} />
      ),
    },
    {
      name: 'LMA Import',
      component: () => <LmaImportTab key={`lma-import-${selectedIndex}`} />,
    },
  ];

  // Inner component for connection tab to keep the main component clean
  const ExactConnectionTab = () => (
    <div className="w-full flex flex-col gap-6 px-4 pb-4">
      {/* Exact Online Integration Section */}
      <div className="border border-solid border-color-border-primary rounded-2xl bg-color-surface-primary">
        <div className="flex items-center justify-between px-6 py-4 border-b border-solid border-color-border-primary">
          <div className="flex flex-col gap-1">
            <h4 className="text-color-text-primary">Exact Online Integratie</h4>
            <span className="text-color-text-secondary">
              Beheer de connectie met Exact Online voor automatische
              synchronisatie
            </span>
          </div>
        </div>

        {/* Connection Status */}
        <div className="px-6 py-4 border-b border-solid border-color-border-primary">
          <div className="flex items-center justify-between">
            <div className="flex flex-col gap-1">
              <h5 className="text-color-text-primary">Verbindingsstatus</h5>
              <div className="flex items-center gap-2">
                <div
                  className={`w-3 h-3 rounded-full ${
                    connectionStatus?.connected
                      ? 'bg-color-status-success-dark'
                      : 'bg-color-status-error-dark'
                  }`}
                />
                <span className="text-color-text-secondary">
                  {connectionStatus?.connected ? 'Verbonden' : 'Niet verbonden'}
                </span>
              </div>
              {connectionStatus?.message && (
                <p className="text-sm text-color-text-secondary mt-1">
                  {connectionStatus.message}
                </p>
              )}
            </div>
            <Button
              variant="secondary"
              label="Test Verbinding"
              onClick={handleTestConnection}
              disabled={isLoadingStatus}
            />
          </div>
        </div>

        {/* Authorization Section */}
        {!connectionStatus?.connected && (
          <div className="px-6 py-4 border-b border-solid border-color-border-primary">
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1">
                <h5 className="text-color-text-primary">
                  Autorisatie instellen
                </h5>
                <span className="text-sm text-color-text-secondary">
                  Klik op de knop hieronder om de autorisatie URL te genereren.
                  U wordt doorgestuurd naar Exact Online om de applicatie te
                  autoriseren.
                </span>
              </div>

              <div className="flex gap-3">
                <Button
                  variant="primary"
                  label="Genereer Autorisatie URL"
                  onClick={handleGetAuthUrl}
                  disabled={getAuthUrlMutation.isPending || authUrl !== null}
                />
                {authUrl && (
                  <Button
                    variant="secondary"
                    label="Ga naar Exact Online Autorisatie"
                    onClick={handleOpenAuthUrl}
                  />
                )}
              </div>

              {authUrl && (
                <div className="flex flex-col gap-2 p-4 bg-color-surface-tertiary rounded-radius-md">
                  <p className="text-sm text-color-text-secondary">
                    ✅ Autorisatie URL succesvol gegenereerd. Klik op de knop
                    hierboven om naar Exact Online te gaan.
                  </p>
                  <p className="text-xs text-color-text-secondary mt-2">
                    💡 Na het voltooien van de autorisatie bij Exact Online
                    wordt u automatisch teruggeleid naar deze pagina en ontvangt
                    u een bevestigingsmelding.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Token Management Section */}
        <div className="px-6 py-4 border-b border-solid border-color-border-primary">
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <h5 className="text-color-text-primary">Token Beheer</h5>
              <span className="text-sm text-color-text-secondary">
                Vernieuw het toegangstoken wanneer deze is verlopen. Dit wordt
                normaal gesproken automatisch gedaan.
              </span>
            </div>

            <div>
              <Button
                variant="secondary"
                label="Vernieuw Token"
                onClick={handleRefreshToken}
                disabled={refreshTokenMutation.isPending}
              />
            </div>
          </div>
        </div>

        {/* Data Synchronization Section */}
        <div className="px-6 py-4">
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <h5 className="text-color-text-primary">Data Synchronisatie</h5>
              <span className="text-sm text-color-text-secondary">
                Synchroniseer bedrijfsgegevens van Exact Online naar de lokale
                database. Bestaande gegevens worden overschreven met de waarden
                uit Exact Online.
              </span>
            </div>

            <div>
              <Button
                variant="primary"
                label={
                  syncFromExactMutation.isPending
                    ? 'Synchroniseren...'
                    : 'Synchroniseer van Exact Online'
                }
                onClick={handleSyncFromExact}
                disabled={
                  syncFromExactMutation.isPending ||
                  !connectionStatus?.connected
                }
              />
            </div>

            {!connectionStatus?.connected && (
              <p className="text-sm text-color-text-secondary">
                ⚠️ Maak eerst verbinding met Exact Online om te kunnen
                synchroniseren.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );

  if (isLoadingStatus) {
    return <ContentContainer title={'Instellingen'}>Laden...</ContentContainer>;
  }

  return (
    <ContentContainer title={'Instellingen'}>
      <div className="flex-1 flex flex-col items-start self-stretch gap-4 rounded-b-radius-lg border-color-border-primary overflow-hidden">
        <TabGroup
          selectedIndex={selectedIndex}
          onChange={setSelectedIndex}
          className="w-full flex-1 flex flex-col min-h-0"
        >
          <TabList className="relative z-10">
            {tabs.map((tab) => (
              <Tab label={tab.name} key={tab.name} />
            ))}
          </TabList>
          <TabPanels
            className="
              flex flex-col flex-1
              bg-color-surface-primary
              border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary
              pt-4
              gap-4
              min-h-0
              -mt-[2px]
            "
          >
            {tabs.map((tab, index) => (
              <TabPanel
                key={index}
                className="flex-1 flex flex-col items-start self-stretch gap-4 overflow-hidden"
              >
                {tab.component()}
              </TabPanel>
            ))}
          </TabPanels>
        </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default SettingsPage;
