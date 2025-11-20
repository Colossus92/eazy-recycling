package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material

interface Materials {
  fun getAllMaterials(): List<Material>
  fun getMaterialById(id: Long): Material?
  fun createMaterial(material: Material): Material
  fun updateMaterial(id: Long, material: Material): Material
  fun deleteMaterial(id: Long)
}
