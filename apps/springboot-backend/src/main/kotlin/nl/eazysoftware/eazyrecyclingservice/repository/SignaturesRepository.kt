package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.SignaturesDto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SignaturesRepository: JpaRepository<SignaturesDto, UUID> {

}