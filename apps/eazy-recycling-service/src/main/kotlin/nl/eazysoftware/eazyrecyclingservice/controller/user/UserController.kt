package nl.eazysoftware.eazyrecyclingservice.controller.user

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import nl.eazysoftware.eazyrecyclingservice.domain.service.UserService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    fun getAllUsers(): List<UserResponse> {
        return userService.getAllUsers()
            .map { UserResponse.from(it) }
    }

    @GetMapping(path = ["/{id}"])
    fun getUserById(@PathVariable id: String): UserResponse {
        val user = userService.getById(id)

        return UserResponse.from(user)
    }

    @PostMapping
    fun createUser(@RequestBody createUserRequest: CreateUserRequest) {
        userService.createUser(createUserRequest)
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteUser(@PathVariable id: String) {
        userService.deleteUser(id)
    }

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
}