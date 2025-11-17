package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import org.springframework.data.jpa.repository.JpaRepository

interface TruckRepository: JpaRepository<TruckDto, String>
