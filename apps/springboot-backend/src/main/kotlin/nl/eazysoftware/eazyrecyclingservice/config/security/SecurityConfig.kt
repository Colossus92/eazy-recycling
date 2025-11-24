package nl.eazysoftware.eazyrecyclingservice.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Value("\${supabase.api.jwt-secret}")
    private lateinit var jwtSecret: String

    @Value("\${supabase.api.jwt-issuer}")
    private lateinit var supabaseJwtIssuer: String

    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOrigins: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/admin/exact/callback**").permitAll()
                    .requestMatchers("/v3/api-docs.yaml").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .bearerTokenResolver(bearerTokenResolver())
                    .jwt { jwt ->
                        jwt.decoder(jwtDecoder())
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                    }
            }
            .sessionManagement {
              it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .build()
    }

    @Bean
    fun bearerTokenResolver(): BearerTokenResolver {
        // Define public endpoints as regex patterns that should skip JWT authentication
        // Convert Ant-style patterns to regex:
        // ** = .* (match any characters)
        // * = [^/]* (match any characters except /)
        val publicEndpointPatterns = listOf(
            "^/api/admin/exact/callback.*".toRegex(),  // /api/admin/exact/callback**
            "^/v3/api-docs\\.yaml$".toRegex(),          // /v3/api-docs.yaml (exact match)
            "^/actuator/.*".toRegex()                  // /actuator/**
        )
        
        val defaultResolver = DefaultBearerTokenResolver()
        
        return BearerTokenResolver { request ->
            val requestUri = request.requestURI
            // If this is a public endpoint, return null to skip JWT authentication
            if (publicEndpointPatterns.any { it.matches(requestUri) }) {
                null
            } else {
                // For protected endpoints, use the default resolver
                defaultResolver.resolve(request)
            }
        }
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("user_roles")
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("")

        return JwtAuthenticationConverter()
            .apply {
                this.setJwtGrantedAuthoritiesConverter { jwt -> jwtGrantedAuthoritiesConverter.convert(jwt) }
            }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        val origins = allowedOrigins.split(",").map { it.trim() }
        configuration.allowedOriginPatterns = origins

        // Set default values for other CORS settings since you simplified the config
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.exposedHeaders = listOf("Authorization")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val secretBytes: ByteArray = jwtSecret.toByteArray(StandardCharsets.UTF_8)
        val hmacKey: SecretKey = SecretKeySpec(secretBytes, "HMACSHA256")

        val jwtDecoder = NimbusJwtDecoder.withSecretKey(hmacKey).build()

        jwtDecoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(supabaseJwtIssuer)
        )

        return jwtDecoder
    }

}
