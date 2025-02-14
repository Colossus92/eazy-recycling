package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.PlanningEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PlanningRepository : JpaRepository<PlanningEntry, UUID> {
}