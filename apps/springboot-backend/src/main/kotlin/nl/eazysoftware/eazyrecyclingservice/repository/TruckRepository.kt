package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.springframework.data.jpa.repository.JpaRepository

interface TruckRepository: JpaRepository<Truck, String>