package nl.eazysoftware.eazyrecyclingservice.repository.exact

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SyncCursorRepository : JpaRepository<SyncCursorDto, UUID> {
    /**
     * Find cursor by entity name (defaults to sync cursor type for backward compatibility).
     */
    fun findByEntityAndCursorType(entity: String, cursorType: String): SyncCursorDto?
    
    /**
     * @deprecated Use findByEntityAndCursorType instead
     */
    @Deprecated("Use findByEntityAndCursorType instead", ReplaceWith("findByEntityAndCursorType(entity, SyncCursorDto.CURSOR_TYPE_SYNC)"))
    fun findByEntity(entity: String): SyncCursorDto?
}
