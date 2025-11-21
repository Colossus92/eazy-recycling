package nl.eazysoftware.eazyrecyclingservice.repository.exact

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "exact_tokens")
data class ExactTokenDto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "access_token", nullable = false)
    var accessToken: String,

    @Column(name = "refresh_token", nullable = false)
    var refreshToken: String,

    @Column(name = "token_type", nullable = false)
    var tokenType: String = "Bearer",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
