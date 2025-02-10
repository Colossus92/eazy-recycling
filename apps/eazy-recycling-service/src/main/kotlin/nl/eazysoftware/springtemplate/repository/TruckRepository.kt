package nl.eazysoftware.springtemplate.repository

import org.springframework.data.jpa.repository.JpaRepository

interface TruckRepository : JpaRepository<Truck, String> {
}