package nl.eazysoftware.eazyrecyclingservice.controller.user

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.collections.map


data class UserResponse(
    val id: String,
    val email: String?,
    val lastSignInAt: String?,
    val firstName: String?,
    val lastName: String?,
    val roles: List<String>
) {
    companion object {
        fun from(user: UserInfo): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                lastSignInAt = user.lastSignInAt?.toString(),
                firstName = extractStringFromMetadata(user, "first_name"),
                lastName = extractStringFromMetadata(user, "last_name"),
                roles = extractRolesFromMetadata(user)
            )
        }

        private fun extractStringFromMetadata(user: UserInfo, key: String): String? {
            return user.userMetadata?.get(key)?.let {
                (it as JsonPrimitive).content
            }
        }

        private fun extractRolesFromMetadata(user: UserInfo): List<String> {
            return user.userMetadata?.get("roles")?.let { rolesJson ->
                when (rolesJson) {
                    is JsonArray -> rolesJson.map { role ->
                        (role as JsonPrimitive).content
                    }
                    else -> emptyList()
                }
            } ?: emptyList()
        }
    }
}
