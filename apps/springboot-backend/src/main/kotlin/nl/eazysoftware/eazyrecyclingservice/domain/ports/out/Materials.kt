package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialQueryResult
import java.math.BigDecimal
import java.util.*

interface Materials {
  fun getMaterialById(id: UUID): Material?
  fun getAllMaterialsWithGroupDetails(): List<MaterialQueryResult>
  fun getMaterialWithGroupDetailsById(id: UUID): MaterialQueryResult?
  fun searchMaterials(query: String, limit: Int = 50): List<MaterialQueryResult>
  fun createMaterial(material: Material): Material
  fun updateMaterial(id: UUID, material: Material): Material
  fun deleteMaterial(id: UUID)
  fun updateMaterialPrice(id: UUID, price: BigDecimal?): Boolean
}
