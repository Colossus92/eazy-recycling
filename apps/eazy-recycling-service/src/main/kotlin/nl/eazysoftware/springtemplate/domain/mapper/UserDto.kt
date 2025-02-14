package nl.eazysoftware.springtemplate.domain.mapper

import java.util.*

data class UserDto(
    val id: Long,
    var name: String,
    val createdAt: Date
)
