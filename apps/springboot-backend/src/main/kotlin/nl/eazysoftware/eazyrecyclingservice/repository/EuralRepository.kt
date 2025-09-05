package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import org.springframework.data.jpa.repository.JpaRepository

interface EuralRepository: JpaRepository<Eural, String>