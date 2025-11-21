package nl.eazysoftware.eazyrecyclingservice.controller.admin

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView

/**
 * Controller for Exact Online OAuth2 integration.
 * Handles authorization flow and token management for Exact Online API access.
 */
@RestController
@RequestMapping("/api/admin/exact")
@PreAuthorize(HAS_ROLE_ADMIN)
class ExactOnlineController(
    private val exactOAuthService: ExactOAuthService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * GET /api/admin/exact/auth-url
     *
     * Returns the Exact Online authorization URL that the frontend should redirect to.
     * The URL includes the OAuth2 parameters (client_id, redirect_uri, state, etc.)
     */
    @GetMapping("/auth-url")
    fun getAuthorizationUrl(): AuthorizationUrlResponse {
        logger.info("Generating Exact Online authorization URL")

        val response = exactOAuthService.buildAuthorizationUrl()

        return AuthorizationUrlResponse(
            authorizationUrl = response.authorizationUrl,
            state = response.state
        )
    }

    /**
     * GET /api/admin/exact/callback
     *
     * OAuth2 callback endpoint. Exact Online redirects here after user authorization.
     * Exchanges the authorization code for access and refresh tokens.
     *
     * Query parameters:
     * - code: Authorization code from Exact Online
     * - state: CSRF protection token
     */
    @GetMapping("/callback")
    @PreAuthorize("permitAll()") // Allow unauthenticated access for OAuth callback
    fun handleCallback(
        @RequestParam code: String,
        @RequestParam state: String,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) error_description: String?
    ): RedirectView {

        // Handle OAuth errors
        if (error != null) {
            logger.error("OAuth error: $error - $error_description")
            return RedirectView("/admin/integrations/exact?error=${error}&error_description=${error_description ?: ""}")
        }

        // Verify state to prevent CSRF attacks
        if (!exactOAuthService.verifyState(state)) {
            logger.error("Invalid or expired state parameter")
            return RedirectView("/admin/integrations/exact?error=invalid_state")
        }

        try {
            // Exchange code for tokens
            logger.info("Exchanging authorization code for tokens")
            exactOAuthService.exchangeCodeForTokens(code)

            // Redirect back to frontend with success indicator
            return RedirectView("/admin/integrations/exact?connected=1")

        } catch (e: Exception) {
            logger.error("Failed to complete OAuth flow", e)
            return RedirectView("/admin/integrations/exact?error=token_exchange_failed")
        }
    }

    /**
     * GET /api/admin/exact/status
     *
     * Check if we have valid Exact Online tokens
     */
    @GetMapping("/status")
    fun getConnectionStatus(): ConnectionStatusResponse {
        val hasValidTokens = exactOAuthService.hasValidTokens()

        return ConnectionStatusResponse(
            connected = hasValidTokens,
            message = if (hasValidTokens) "Connected to Exact Online" else "Not connected"
        )
    }

    /**
     * POST /api/admin/exact/refresh
     *
     * Manually refresh the access token (mainly for testing)
     */
    @PostMapping("/refresh")
    fun refreshToken(): ResponseEntity<RefreshTokenResponse> {
        return try {
            exactOAuthService.refreshAccessToken()
            ResponseEntity.ok(RefreshTokenResponse(success = true, message = "Token refreshed successfully"))
        } catch (e: Exception) {
            logger.error("Failed to refresh token", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RefreshTokenResponse(success = false, message = "Failed to refresh token: ${e.message}"))
        }
    }

    data class AuthorizationUrlResponse(
        val authorizationUrl: String,
        val state: String
    )

    data class ConnectionStatusResponse(
        val connected: Boolean,
        val message: String
    )

    data class RefreshTokenResponse(
        val success: Boolean,
        val message: String
    )
}
