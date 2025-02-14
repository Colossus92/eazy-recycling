package nl.eazysoftware.springtemplate.domain.mapper

import java.util.*

data class User(
    val id: Long,
    var name: String,
    val createdAt: Date
)
