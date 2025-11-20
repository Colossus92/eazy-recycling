package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup

interface MaterialGroups {
  fun getAllMaterialGroups(): List<MaterialGroup>
  fun getMaterialGroupById(id: Long): MaterialGroup?
  fun createMaterialGroup(materialGroup: MaterialGroup): MaterialGroup
  fun updateMaterialGroup(id: Long, materialGroup: MaterialGroup): MaterialGroup
  fun deleteMaterialGroup(id: Long)
}
