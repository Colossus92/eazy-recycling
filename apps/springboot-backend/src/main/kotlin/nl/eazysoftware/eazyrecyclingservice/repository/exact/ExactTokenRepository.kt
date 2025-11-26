package nl.eazysoftware.eazyrecyclingservice.repository.exact

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface ExactTokenRepository : JpaRepository<ExactTokenDto, String> {
    
    /**
     * Find the most recently created token (assumes single-tenant setup for now)
     */
    fun findFirstByOrderByCreatedAtDesc(): ExactTokenDto?
    
    /**
     * Find tokens that are about to expire (within the next specified duration)
     */
    @Query("SELECT t FROM ExactTokenDto t WHERE t.expiresAt <= :expiryThreshold")
    fun findTokensExpiringBefore(expiryThreshold: Instant): List<ExactTokenDto>
    
    /**
     * Delete all tokens that have already expired
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ExactTokenDto t WHERE t.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Int
}
