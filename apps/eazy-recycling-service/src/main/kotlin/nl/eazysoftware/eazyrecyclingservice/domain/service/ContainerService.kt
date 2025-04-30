package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.repository.ContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.Container
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ContainerService(private val containerRepository: ContainerRepository) {

    fun createContainer(container: Container) {
        containerRepository.save(container)
    }

    fun getAllContainers(): List<Container> {
        return containerRepository.findAll()
    }

    fun getContainerById(id: UUID): Container {
        return containerRepository.findById(id)
            ?: throw EntityNotFoundException("Container with id $id not found")
    }

    fun updateContainer(id: UUID, container: Container): Container {
        containerRepository.findById(id)
            ?: throw EntityNotFoundException("Container with id $id not found")

        return containerRepository.save(container)
    }

    fun deleteContainer(id: UUID) {
        containerRepository.deleteById(id)
    }
}