package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.auth.user.UserInfo
import nl.eazysoftware.eazyrecyclingservice.controller.user.CreateUserRequest
import nl.eazysoftware.eazyrecyclingservice.controller.user.UpdateUserRequest
import nl.eazysoftware.eazyrecyclingservice.repository.UserRepository
import org.springframework.stereotype.Service
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getAllUsers(): List<UserInfo> {
        return userRepository.getAllUsers()
    }

    fun createUser(createUserRequest: CreateUserRequest) {
        userRepository.createUser(createUserRequest)
    }

    fun deleteUser(id: String) {
        userRepository.deleteUser(id)
    }

    fun getById(id: String): UserInfo {
        return userRepository.getById(id)
    }

    fun updateUser(id: String, updateUserRequest: UpdateUserRequest) {
        return userRepository.updateUserIncludingRoles(id, updateUserRequest)
    }

    fun updateProfile(id: String, updateUserRequest: UpdateUserRequest) {
        return userRepository.updateProfile(id, updateUserRequest)
    }

    fun findAllDrivers(): List<UserInfo> {
        return userRepository.getAllUsers()
            .filter { user ->
                user.userMetadata?.get("roles")?.let { rolesJson ->
                    when (rolesJson) {
                        is JsonArray -> rolesJson.any { role ->
                            (role as? JsonPrimitive)?.content == Roles.CHAUFFEUR
                        }
                        else -> false
                    }
                } ?: false
            }
    }

}
