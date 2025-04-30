package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.Container
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class ContainerRepository {

    private val containers: MutableMap<UUID, Container> = mutableMapOf()

    fun save(container: Container): Container {
        containers.put(container.uuid, container)

        return container
    }

    fun findAll(): List<Container> {
        return containers.values.toList()
    }

    fun findById(id: UUID): Container? {
        return containers[id]
    }

    fun deleteById(id: UUID) {
        containers.remove(id)
    }

}