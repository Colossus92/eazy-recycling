package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import java.util.*

interface MaterialGroups {
  fun getAllMaterialGroups(): List<MaterialGroup>
  fun getMaterialGroupById(id: UUID): MaterialGroup?
  fun createMaterialGroup(materialGroup: MaterialGroup): MaterialGroup
  fun updateMaterialGroup(id: UUID, materialGroup: MaterialGroup): MaterialGroup
  fun deleteMaterialGroup(id: UUID)
}
