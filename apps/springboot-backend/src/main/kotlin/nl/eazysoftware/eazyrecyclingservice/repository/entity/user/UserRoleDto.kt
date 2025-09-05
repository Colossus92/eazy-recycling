package nl.eazysoftware.eazyrecyclingservice.repository.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_roles")
data class UserRoleDto(
    @Id
    @Column(name = "id")
    val id: Int?,

    @Column(name = "user_id")
    val userId: UUID?,

    @Column(name = "role")
    val role: String,
)
