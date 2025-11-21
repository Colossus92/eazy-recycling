# Exact Online OAuth2 Integration

This document describes the Exact Online OAuth2 integration implementation in the backend.

## Overview

The integration allows the application to authenticate with Exact Online's API using OAuth2 authorization code flow. The system automatically manages token lifecycle including refresh and expiration handling.

## Architecture

### Components

1. **Configuration** (`ExactOnlineProperties`)
   - Manages OAuth2 credentials and endpoints
   - Configured via environment variables

2. **Database** (`ExactTokenDto`, `ExactTokenRepository`)
   - Stores access tokens, refresh tokens, and expiration times
   - Supports querying tokens by expiration

3. **Service** (`ExactOAuthService`)
   - Handles OAuth2 authorization flow
   - Exchanges authorization codes for tokens
   - Refreshes expired tokens
   - Validates CSRF state parameters

4. **Controller** (`ExactOnlineController`)
   - Provides REST endpoints for OAuth flow
   - Admin-only access with `@PreAuthorize`

5. **Scheduled Job** (`ExactTokenRefreshScheduler`)
   - Automatically refreshes tokens before expiration
   - Runs hourly, refreshes tokens expiring within 10 minutes

6. **API Client** (`ExactApiClient`)
   - Provides RestTemplate with automatic token injection
   - Handles 401 responses by refreshing token and retrying

## OAuth2 Flow

### Initial Authorization

```
1. Frontend → GET /api/admin/exact/auth-url
   ← Returns authorization URL with state parameter

2. Frontend redirects user to Exact Online login

3. User logs in and grants permissions

4. Exact redirects → GET /api/admin/exact/callback?code=...&state=...
   
5. Backend:
   - Verifies state (CSRF protection)
   - Exchanges code for tokens
   - Stores tokens in database
   - Redirects to frontend success page
```

### Token Refresh

**Automatic (Scheduled)**
- Runs every hour via `ExactTokenRefreshScheduler`
- Refreshes tokens expiring within 10 minutes

**On-Demand (API Error)**
- `ExactApiClient` intercepts 401 responses
- Automatically refreshes token
- Retries the original request

**Manual**
- POST `/api/admin/exact/refresh` (admin only)

## Configuration

### Environment Variables

Add to `.env` or set as environment variables:

```bash
# Required
EXACT_CLIENT_ID=your-client-id-from-exact-app-center
EXACT_CLIENT_SECRET=your-client-secret-from-exact-app-center

# Optional (defaults shown)
EXACT_REDIRECT_URI=http://localhost:8080/api/admin/exact/callback
```

### Application.yaml

Configuration is automatically loaded via `ExactOnlineProperties`:

```yaml
exact:
  oauth:
    client-id: ${EXACT_CLIENT_ID:}
    client-secret: ${EXACT_CLIENT_SECRET:}
    redirect-uri: ${EXACT_REDIRECT_URI:http://localhost:8080/api/admin/exact/callback}
    authorization-endpoint: https://start.exactonline.nl/api/oauth2/auth
    token-endpoint: https://start.exactonline.nl/api/oauth2/token
```

## Database Schema

```sql
create table exact_tokens (
    id varchar(36) not null,
    access_token varchar(2000) not null,
    refresh_token varchar(2000) not null,
    token_type varchar(50) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    primary key (id)
);
```

## API Endpoints

### GET `/api/admin/exact/auth-url`
**Authentication**: Admin role required

Returns the Exact Online authorization URL.

**Response**:
```json
{
  "authorizationUrl": "https://start.exactonline.nl/api/oauth2/auth?client_id=...",
  "state": "uuid-for-csrf-protection"
}
```

### GET `/api/admin/exact/callback`
**Authentication**: Public (no auth required for OAuth callback)

OAuth2 callback endpoint. Exact Online redirects here after user authorization.

**Query Parameters**:
- `code`: Authorization code from Exact
- `state`: CSRF protection token
- `error` (optional): Error code if authorization failed
- `error_description` (optional): Error description

**Response**: Redirects to frontend
- Success: `/admin/integrations/exact?connected=1`
- Error: `/admin/integrations/exact?error=...`

### GET `/api/admin/exact/status`
**Authentication**: Admin role required

Check if valid Exact Online tokens exist.

**Response**:
```json
{
  "connected": true,
  "message": "Connected to Exact Online"
}
```

### POST `/api/admin/exact/refresh`
**Authentication**: Admin role required

Manually refresh the access token.

**Response**:
```json
{
  "success": true,
  "message": "Token refreshed successfully"
}
```

## Usage in Code

### Making Authenticated Requests to Exact Online

```kotlin
@Service
class ExactIntegrationService(
    private val exactApiClient: ExactApiClient
) {
    fun getExactData(): String {
        val restTemplate = exactApiClient.getRestTemplate()
        
        // Token is automatically added, refreshed on 401
        val response = restTemplate.getForEntity(
            "https://start.exactonline.nl/api/v1/...",
            String::class.java
        )
        
        return response.body ?: ""
    }
}
```

### Getting Access Token Directly

```kotlin
@Service
class MyService(
    private val exactOAuthService: ExactOAuthService
) {
    fun doSomething() {
        // Automatically refreshes if expired
        val accessToken = exactOAuthService.getValidAccessToken()
        
        // Use token...
    }
}
```

## Security Considerations

1. **CSRF Protection**: State parameter prevents cross-site request forgery
2. **Admin-Only Access**: OAuth flow initiation requires admin role
3. **Token Storage**: Tokens stored in database with expiration tracking
4. **Secure Transmission**: Always use HTTPS in production
5. **Token Rotation**: Tokens automatically refreshed before expiration

## Exact Online App Registration

To use this integration, you need to register an application in Exact Online:

1. Go to [Exact App Center](https://apps.exactonline.com/)
2. Create a new application
3. Configure:
   - **Redirect URI**: `https://your-domain.com/api/admin/exact/callback`
   - **Grant Type**: Authorization Code
   - **Permissions**: Select required API scopes
4. Note the Client ID and Client Secret
5. Add credentials to your environment variables

## Troubleshooting

### Token Refresh Fails
- Check that refresh token is still valid (Exact may revoke)
- Verify client credentials are correct
- Check Exact API status

### 401 Errors Persist
- Verify token is not expired beyond refresh capability
- Check that user hasn't revoked application access
- Re-authenticate if necessary

### State Verification Fails
- State expires after 5 minutes
- Ensure callback completes within timeout
- Check for clock synchronization issues

## References

- [Exact Online OAuth Documentation](https://support.exactonline.com/community/s/knowledge-base#All-All-DNO-Content-oauth-eol-oauth-dev-impleovervw)
- [Exact Online API Reference](https://start.exactonline.nl/docs/HlpRestAPIResources.aspx)
