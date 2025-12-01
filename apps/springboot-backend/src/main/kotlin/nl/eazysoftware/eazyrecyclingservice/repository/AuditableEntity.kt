package nl.eazysoftware.eazyrecyclingservice.repository

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditableEntity {
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  var createdAt: Instant? = null

  @CreatedBy
  @Column(name = "created_by", updatable = false)
  var createdBy: String? = null

  @LastModifiedDate
  @Column(name = "last_modified_at", nullable = false)
  var updatedAt: Instant? = null

  @LastModifiedBy
  @Column(name = "last_modified_by")
  var updatedBy: String? = null
}
