package nl.eazysoftware.eazyrecyclingservice.application.usecase.lmaimport

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Fuzzy matching service for LMA import operations.
 * Provides fuzzy string matching using Levenshtein distance for company matching.
 */
@Component
class FuzzyMatcher {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val levenshtein = LevenshteinDistance.getDefaultInstance()

  /**
   * Finds the best matching company from candidates using fuzzy name matching.
   * 
   * @param candidates List of companies with matching KvK number
   * @param companyName Company name to match against (from LMA data "Naam Ontdoener")
   * @return Best matching company, or null if no candidates
   */
  fun findBestMatchingCompany(candidates: List<Company>, companyName: String?): Company? {
    return when (candidates.size) {
      0 -> null
      1 -> candidates.first()
      else -> {
        logger.warn("Found ${candidates.size} companies: ${candidates.joinToString { it.name }}")
        
        if (companyName.isNullOrBlank()) {
          logger.warn("No company name provided for disambiguation, using first match")
          return candidates.first()
        }
        
        val bestMatch = candidates
          .map { company ->
            val distance = levenshtein.apply(company.name.lowercase(), companyName.lowercase())
            company to distance
          }
          .minByOrNull { it.second }
        
        bestMatch?.let { (company, distance) ->
          logger.info("Selected company '${company.name}' (distance: $distance from '$companyName')")
          company
        }
      }
    }
  }

}
