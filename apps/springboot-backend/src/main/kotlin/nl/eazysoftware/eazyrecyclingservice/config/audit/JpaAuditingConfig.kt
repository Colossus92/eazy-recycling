package nl.eazysoftware.eazyrecyclingservice.config.audit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.*

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditingConfig {

    @Bean
    fun auditorProvider(): AuditorAware<String> {
        return AuditorAware {
            val authentication = SecurityContextHolder.getContext().authentication

            if (authentication == null || !authentication.isAuthenticated) {
                return@AuditorAware Optional.of("system")
            }

            if (authentication is JwtAuthenticationToken) {
                val jwt = authentication.token
                
                // Extract first_name and last_name from user_metadata
                @Suppress("UNCHECKED_CAST")
                val userMetadata = jwt.claims["user_metadata"] as? Map<String, Any>
                val firstName = userMetadata?.get("first_name") as? String ?: ""
                val lastName = userMetadata?.get("last_name") as? String ?: ""
                val fullName = "$firstName $lastName".trim()
                
                if (fullName.isNotEmpty()) {
                    return@AuditorAware Optional.of(fullName)
                }
                
                // Fallback to email if name not available
                val email = jwt.claims["email"] as? String
                return@AuditorAware Optional.of(email ?: "unknown")
            }

            Optional.of(authentication.name ?: "unknown")
        }
    }
}
