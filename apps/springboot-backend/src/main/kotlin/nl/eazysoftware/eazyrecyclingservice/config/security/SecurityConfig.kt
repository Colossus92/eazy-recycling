package nl.eazysoftware.eazyrecyclingservice.config.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
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
import java.util.*

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

  private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${supabase.api.jwt-issuer}")
    private lateinit var supabaseJwtIssuer: String

    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOrigins: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/admin/exact/callback**").permitAll()
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
            "^/admin/exact/callback.*".toRegex(),  // /admin/exact/callback** (after /api stripped by ingress)
            "^/v3/api-docs\\.yaml$".toRegex(),      // /v3/api-docs.yaml (exact match)
            "^/actuator/.*".toRegex()               // /actuator/**
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
        // Use JWKS endpoint for JWT validation - works with both HS256 and ES256
        // Automatically handles key rotation and caching
        val jwksUri = "$supabaseJwtIssuer/.well-known/jwks.json"

        log.info("Configuring JWT decoder with JWKS URI: {}", jwksUri)
        log.info("Expected JWT issuer: {}", supabaseJwtIssuer)

        // Test JWKS endpoint connectivity
        try {
            val url = java.net.URL(jwksUri)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.getInputStream().bufferedReader().readText()
            log.info("JWKS endpoint is accessible. Response: {}", response)
        } catch (e: Exception) {
            log.error("Failed to connect to JWKS endpoint: {}", jwksUri, e)
        }

        val jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri(jwksUri)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build()

        jwtDecoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(supabaseJwtIssuer)
        )

        // Wrap decoder to add debug logging
        return JwtDecoder { token ->
            try {
                // Parse token header without validation to inspect it
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))
                    val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
                    log.debug("JWT Header: {}", headerJson)
                    log.debug("JWT Payload: {}", payloadJson)

                    // Extract issuer from payload
                    val issMatch = """"iss":"([^"]+)"""".toRegex().find(payloadJson)
                    if (issMatch != null) {
                        log.error("Token issuer claim: {}", issMatch.groupValues[1])
                        log.error("Expected issuer: {}", supabaseJwtIssuer)
                        log.error("Issuers match: {}", issMatch.groupValues[1] == supabaseJwtIssuer)
                    }
                }

                log.debug("Attempting to decode JWT token with JWKS validation")
                val jwt = jwtDecoder.decode(token)
                log.debug("JWT decoded successfully. Claims: {}", jwt.claims)
                log.debug("JWT issuer: {}", jwt.issuer)
                log.debug("JWT subject: {}", jwt.subject)
                log.debug("JWT algorithm: {}", jwt.headers["alg"])
                jwt
            } catch (e: Exception) {
                log.error("JWT validation failed", e)
                log.error("Token (first 50 chars): {}", token.take(50))
                log.error("Expected issuer: {}", supabaseJwtIssuer)
                log.error("JWKS URI: {}", jwksUri)
                throw e
            }
        }
    }

}
