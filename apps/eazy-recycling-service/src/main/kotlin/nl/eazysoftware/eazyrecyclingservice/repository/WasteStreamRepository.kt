package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import org.springframework.data.jpa.repository.JpaRepository

interface WasteStreamRepository: JpaRepository<WasteStreamDto, String>