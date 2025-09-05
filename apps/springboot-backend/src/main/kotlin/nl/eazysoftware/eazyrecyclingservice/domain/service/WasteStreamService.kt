package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.repository.WasteStreamRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import org.springframework.stereotype.Service

@Service
class WasteStreamService(
    private val wasteStreamRepository: WasteStreamRepository
) {

    fun getWasteStreams(): List<WasteStreamDto> {
        return wasteStreamRepository.findAll()
    }

    fun createWasteStream(wasteStreamDto: WasteStreamDto): WasteStreamDto {
        wasteStreamRepository.existsById(wasteStreamDto.number)
            .let { if (it) throw IllegalArgumentException("Afvalstroom met nummer ${wasteStreamDto.number} bestaat al") }

        return wasteStreamRepository.save(wasteStreamDto)
    }

    fun deleteWasteStream(wasteStreamNumber: String) {
        wasteStreamRepository.deleteById(wasteStreamNumber)
    }

    fun updateWasteStream(number: String, wasteStreamDto: WasteStreamDto): WasteStreamDto {
        if (wasteStreamDto.number != number) {
            throw IllegalArgumentException("Nieuw afvalstroomnummer moet overeen komen met bestaande nummer $number")
        }

        wasteStreamRepository.findById(number)
            .orElseThrow { EntityNotFoundException("Geen afvalstroom met nummer $number gevonden") }

        return wasteStreamRepository.save(wasteStreamDto)
    }

}
