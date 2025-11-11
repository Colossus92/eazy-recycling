package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

interface MonthlyWasteDeclarationJobs {
  fun save(vararg jobs: MonthlyWasteDeclarationJob)

  /**
   * Finds all pending monthly waste declaration jobs.
   *
   * @return List of jobs with PENDING status
   */
  fun findPending(): List<MonthlyWasteDeclarationJob>

}
