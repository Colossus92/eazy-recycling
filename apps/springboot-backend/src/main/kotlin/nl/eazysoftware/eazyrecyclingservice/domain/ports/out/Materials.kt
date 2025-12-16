package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialQueryResult
import java.math.BigDecimal

interface Materials {
  fun getMaterialById(id: Long): Material?
  fun getAllMaterialsWithGroupDetails(): List<MaterialQueryResult>
  fun getMaterialWithGroupDetailsById(id: Long): MaterialQueryResult?
  fun searchMaterials(query: String, limit: Int = 50): List<MaterialQueryResult>
  fun createMaterial(material: Material): Material
  fun updateMaterial(id: Long, material: Material): Material
  fun deleteMaterial(id: Long)
  fun updateMaterialPrice(id: Long, price: BigDecimal?): Boolean
}
