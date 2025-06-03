package nl.eazysoftware.eazyrecyclingservice.config.security

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles

/**
 * Security expressions for use with @PreAuthorize annotations
 */
object SecurityExpressions {

    // Combined role expressions
    const val HAS_ANY_ROLE = "hasAnyAuthority('${Roles.ADMIN}','${Roles.CHAUFFEUR}','${Roles.PLANNER}')"
    const val HAS_ADMIN_OR_PLANNER = "hasAnyAuthority('${Roles.ADMIN}','${Roles.PLANNER}')"
}