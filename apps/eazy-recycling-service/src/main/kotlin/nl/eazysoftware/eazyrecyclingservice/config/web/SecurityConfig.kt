package nl.eazysoftware.eazyrecyclingservice.config.web


import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    private val logger = org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${supabase.api.jwt-secret}")
    private lateinit var jwtSecret: String

    @Value("\${supabase.api.url}")
    private lateinit var supabaseUrl: String


    @Bean
    fun jwtDecoder(): JwtDecoder {
        logger.info("JWT Secret: $jwtSecret")
        val secretBytes: ByteArray = jwtSecret.toByteArray(StandardCharsets.UTF_8)
        val hmacKey: SecretKey = SecretKeySpec(secretBytes, "HMACSHA256")

        val jwtDecoder = NimbusJwtDecoder.withSecretKey(hmacKey).build()

        jwtDecoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer("$supabaseUrl/auth/v1")
        )

        return jwtDecoder
    }
}