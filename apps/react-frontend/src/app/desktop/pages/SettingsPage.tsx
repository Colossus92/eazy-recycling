import type {
    AuthorizationUrlResponse,
    ConnectionStatusResponse,
    RefreshTokenResponse,
} from '@/api/client';
import { exactOnlineService } from '@/api/services/exactOnlineService.ts';
import { ContentContainer } from '@/components/layouts/ContentContainer.tsx';
import { Button } from '@/components/ui/button/Button.tsx';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';


export const SettingsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [authUrl, setAuthUrl] = useState<string | null>(null);

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

  // Handle OAuth callback results from query parameters
  useEffect(() => {
    const exactConnected = searchParams.get('exact_connected');
    const exactError = searchParams.get('exact_error');
    const exactErrorDescription = searchParams.get('exact_error_description');

    if (exactConnected === 'true') {
      toastService.success('Exact Online verbinding succesvol tot stand gebracht!');
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

  if (isLoadingStatus) {
    return <ContentContainer title={'Instellingen'}>Laden...</ContentContainer>;
  }

  return (
    <ContentContainer title={'Instellingen'}>
      <div className="w-full max-w-4xl flex flex-col gap-6">
        {/* Exact Online Integration Section */}
        <div className="border border-solid border-color-border-primary rounded-2xl bg-color-surface-primary">
          <div className="flex items-center justify-between px-6 py-4 border-b border-solid border-color-border-primary">
            <div className="flex flex-col gap-1">
              <h4 className="text-color-text-primary">
                Exact Online Integratie
              </h4>
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
                    {connectionStatus?.connected
                      ? 'Verbonden'
                      : 'Niet verbonden'}
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
                    Klik op de knop hieronder om de autorisatie URL te
                    genereren. U wordt doorgestuurd naar Exact Online om de
                    applicatie te autoriseren.
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
                      ✅ Autorisatie URL succesvol gegenereerd. Klik op de knop hierboven om naar Exact Online te gaan.
                    </p>
                    <p className="text-xs text-color-text-secondary mt-2">
                      💡 Na het voltooien van de autorisatie bij Exact Online wordt u automatisch 
                      teruggeleid naar deze pagina en ontvangt u een bevestigingsmelding.
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Token Management Section */}
          {connectionStatus?.connected && (
            <div className="px-6 py-4">
              <div className="flex flex-col gap-4">
                <div className="flex flex-col gap-1">
                  <h5 className="text-color-text-primary">Token Beheer</h5>
                  <span className="text-sm text-color-text-secondary">
                    Vernieuw het toegangstoken wanneer deze is verlopen. Dit
                    wordt normaal gesproken automatisch gedaan.
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
          )}
        </div>

        {/* API Endpoints Documentation */}
        <div className="border border-solid border-color-border-primary rounded-2xl bg-color-surface-primary">
          <div className="flex items-center justify-between px-6 py-4 border-b border-solid border-color-border-primary">
            <div className="flex flex-col gap-1">
              <h4 className="text-color-text-primary">
                API Endpoints Documentatie
              </h4>
              <span className="text-color-text-secondary">
                Beschikbare endpoints voor Exact Online integratie
              </span>
            </div>
          </div>

          <div className="px-6 py-4">
            <div className="flex flex-col gap-4">
              {/* Get Authorization URL */}
              <div className="p-4 bg-color-surface-tertiary rounded-radius-md">
                <div className="flex items-center gap-2 mb-2">
                  <span className="px-2 py-1 text-xs font-medium bg-color-brand-primary text-color-text-invert-primary rounded">
                    GET
                  </span>
                  <code className="text-sm text-color-text-primary">
                    /api/admin/exact/auth-url
                  </code>
                </div>
                <p className="text-sm text-color-text-secondary">
                  Genereer een OAuth2 autorisatie URL om te connecteren met
                  Exact Online.
                </p>
              </div>

              {/* Get Connection Status */}
              <div className="p-4 bg-color-surface-tertiary rounded-radius-md">
                <div className="flex items-center gap-2 mb-2">
                  <span className="px-2 py-1 text-xs font-medium bg-color-brand-primary text-color-text-invert-primary rounded">
                    GET
                  </span>
                  <code className="text-sm text-color-text-primary">
                    /api/admin/exact/status
                  </code>
                </div>
                <p className="text-sm text-color-text-secondary">
                  Controleer de huidige verbindingsstatus met Exact Online.
                </p>
              </div>

              {/* Refresh Token */}
              <div className="p-4 bg-color-surface-tertiary rounded-radius-md">
                <div className="flex items-center gap-2 mb-2">
                  <span className="px-2 py-1 text-xs font-medium bg-green-600 text-white rounded">
                    POST
                  </span>
                  <code className="text-sm text-color-text-primary">
                    /api/admin/exact/refresh
                  </code>
                </div>
                <p className="text-sm text-color-text-secondary">
                  Vernieuw het toegangstoken voor des Exact Online connectie.
                </p>
              </div>

              {/* Handle Callback */}
              <div className="p-4 bg-color-surface-tertiary rounded-radius-md">
                <div className="flex items-center gap-2 mb-2">
                  <span className="px-2 py-1 text-xs font-medium bg-color-brand-primary text-color-text-invert-primary rounded">
                    GET
                  </span>
                  <code className="text-sm text-color-text-primary">
                    /api/admin/exact/callback
                  </code>
                </div>
                <p className="text-sm text-color-text-secondary">
                  OAuth2 callback endpoint (automatisch aangeroepen door Exact
                  Online na autorisatie).
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ContentContainer>
  );
};

export default SettingsPage;
