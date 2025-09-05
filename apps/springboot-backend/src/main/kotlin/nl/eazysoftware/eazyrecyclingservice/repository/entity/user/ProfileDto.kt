package nl.eazysoftware.eazyrecyclingservice.repository.entity.user

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "profiles")
data class ProfileDto(
    @Id
    @Column(name = "id")
    val id: UUID,

    @Column(name = "first_name")
    val firstName: String?,

    @Column(name = "last_name")
    val lastName: String?,

    @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val roles: List<UserRoleDto> = emptyList(),
)