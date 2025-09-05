package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProfileRepository: JpaRepository<ProfileDto, UUID> {
}