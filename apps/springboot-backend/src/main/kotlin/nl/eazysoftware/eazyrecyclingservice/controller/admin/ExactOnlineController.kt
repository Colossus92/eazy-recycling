package nl.eazysoftware.eazyrecyclingservice.controller.admin

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
@RequestMapping("/admin/exact")
class ExactOnlineController(
    private val exactOAuthService: ExactOAuthService,
    private val exactOnlineSync: ExactOnlineSync,
    @Value("\${frontend.url}") private val frontendUrl: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * GET /api/admin/exact/auth-url
     *
     * Returns the Exact Online authorization URL that the frontend should redirect to.
     * The URL includes the OAuth2 parameters (client_id, redirect_uri, state, etc.)
     */
    @PreAuthorize(HAS_ROLE_ADMIN)
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
     * This endpoint is publicly accessible (configured in SecurityConfig) to allow
     * Exact Online to redirect here without authentication.
     *
     * Query parameters:
     * - code: Authorization code from Exact Online
     * - state: CSRF protection token
     */
    @GetMapping("/callback")
    fun handleCallback(
        @RequestParam code: String,
        @RequestParam state: String,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) error_description: String?
    ): RedirectView {

        // Handle OAuth errors
        if (error != null) {
            logger.error("OAuth error: $error - $error_description")
            return RedirectView("$frontendUrl/settings?exact_error=${error}&exact_error_description=${error_description ?: ""}")
        }

        // Verify state to prevent CSRF attacks
        if (!exactOAuthService.verifyState(state)) {
            logger.error("Invalid or expired state parameter")
            return RedirectView("$frontendUrl/settings?exact_error=invalid_state")
        }

        try {
            // Exchange code for tokens
            logger.info("Exchanging authorization code for tokens")
            exactOAuthService.exchangeCodeForTokens(code)

            // Redirect back to frontend with success indicator
            return RedirectView("$frontendUrl/settings?exact_connected=true")

        } catch (e: Exception) {
            logger.error("Failed to complete OAuth flow", e)
            return RedirectView("$frontendUrl/settings?exact_error=token_exchange_failed")
        }
    }

    /**
     * GET /api/admin/exact/status
     *
     * Check if we have valid Exact Online tokens
     */
    @PreAuthorize(HAS_ROLE_ADMIN)
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
    @PreAuthorize(HAS_ROLE_ADMIN)
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

    /**
     * POST /api/admin/exact/sync
     *
     * Sync companies from Exact Online to our database.
     * Uses the Exact Online Sync API with timestamp-based pagination.
     */
    @PreAuthorize(HAS_ROLE_ADMIN)
    @PostMapping("/sync")
    fun syncFromExact(): ResponseEntity<SyncFromExactResponse> {
        return try {
            logger.info("Starting sync from Exact Online")
            val result = exactOnlineSync.syncFromExact()
            ResponseEntity.ok(SyncFromExactResponse(
                success = true,
                message = "Sync completed successfully",
                recordsSynced = result.recordsSynced,
                recordsCreated = result.recordsCreated,
                recordsUpdated = result.recordsUpdated,
                recordsConflicted = result.recordsConflicted,
                recordsPendingReview = result.recordsPendingReview
            ))
        } catch (e: Exception) {
            logger.error("Failed to sync from Exact Online", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SyncFromExactResponse(
                    success = false,
                    message = "Failed to sync: ${e.message}",
                    recordsSynced = 0,
                    recordsCreated = 0,
                    recordsUpdated = 0,
                    recordsConflicted = 0,
                    recordsPendingReview = 0
                ))
        }
    }
    
    /**
     * GET /api/admin/exact/conflicts
     *
     * Get all sync records that have conflicts requiring manual resolution.
     */
    @PreAuthorize(HAS_ROLE_ADMIN)
    @GetMapping("/conflicts")
    fun getConflicts(): ResponseEntity<SyncConflictsResponse> {
        val conflicts = exactOnlineSync.getConflicts()
        val pendingReviews = exactOnlineSync.getPendingReviews()
        return ResponseEntity.ok(SyncConflictsResponse(
            conflicts = conflicts.map { SyncConflictDto.fromEntity(it) },
            pendingReviews = pendingReviews.map { SyncConflictDto.fromEntity(it) }
        ))
    }

    data class SyncFromExactResponse(
        val success: Boolean,
        val message: String,
        val recordsSynced: Int,
        val recordsCreated: Int,
        val recordsUpdated: Int,
        val recordsConflicted: Int,
        val recordsPendingReview: Int
    )
    
    data class SyncConflictsResponse(
        val conflicts: List<SyncConflictDto>,
        val pendingReviews: List<SyncConflictDto>
    )
    
    data class SyncConflictDto(
        val id: String,
        val companyId: String,
        val externalId: String?,
        val exactGuid: String?,
        val syncStatus: String,
        val conflictDetails: Map<String, Any>?,
        val syncedFromSourceAt: String
    ) {
        companion object {
            fun fromEntity(entity: nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto): SyncConflictDto {
                return SyncConflictDto(
                    id = entity.id?.toString() ?: "",
                    companyId = entity.companyId.toString(),
                    externalId = entity.externalId,
                    exactGuid = entity.exactGuid?.toString(),
                    syncStatus = entity.syncStatus.name,
                    conflictDetails = entity.conflictDetails,
                    syncedFromSourceAt = entity.syncedFromSourceAt.toString()
                )
            }
        }
    }
}
