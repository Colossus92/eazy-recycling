package nl.eazysoftware.eazyrecyclingservice.domain.model

/**
 * Constants class for role names used throughout the application
 * These constants should be used for role-based access control with @RolesAllowed annotations
 */
object Roles {
    /**
     * Administrator role with full system access
     */
    const val ADMIN = "admin"

    /**
     * Chauffeur role for truck drivers
     */
    const val CHAUFFEUR = "chauffeur"

    /**
     * Planner role for scheduling and logistics
     */
    const val PLANNER = "planner"
}
