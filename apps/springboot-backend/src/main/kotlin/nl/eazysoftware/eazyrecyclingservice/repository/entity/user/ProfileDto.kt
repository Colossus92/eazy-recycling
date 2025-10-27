package nl.eazysoftware.eazyrecyclingservice.repository.entity.user

import jakarta.persistence.*
import java.util.*

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
    val roles: MutableList<UserRoleDto> = mutableListOf(),
)
