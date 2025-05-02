package nl.eazysoftware.eazyrecyclingservice.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import nl.eazysoftware.eazyrecyclingservice.controller.user.CreateUserRequest
import nl.eazysoftware.eazyrecyclingservice.controller.user.UpdateUserRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class UserRepository(private val supabaseClient: SupabaseClient) {

    private val log: Logger = LoggerFactory.getLogger(UserRepository::class.java)

    fun getAllUsers(): List<UserInfo> {
        return runBlocking {
            supabaseClient.auth.admin.retrieveUsers()
        }
    }

    fun createUser(createUserRequest: CreateUserRequest) {
        val rolesJsonArray = buildJsonArray {
            createUserRequest.roles.forEach { role ->
                add(JsonPrimitive(role))
            }
        }
        runBlocking {
            supabaseClient.auth.admin.createUserWithEmail {
                email = createUserRequest.email
                password = createUserRequest.password
                autoConfirm = true
                userMetadata {
                    put("first_name", createUserRequest.firstName)
                    put("last_name", createUserRequest.lastName)
                    put("roles", rolesJsonArray)
                }
            }
        }
    }

    fun deleteUser(id: String) {
        runBlocking {
            supabaseClient.auth.admin.deleteUser(id)
        }
    }

    fun getById(id: String): UserInfo {
        return runBlocking {
            supabaseClient.auth.admin.retrieveUserById(id)
        }
    }

    fun updateUser(id: String, updateUserRequest: UpdateUserRequest) {
        val rolesJsonArray = buildJsonArray {
            updateUserRequest.roles.forEach { role ->
                add(JsonPrimitive(role))
            }
        }

        return runBlocking {
            supabaseClient.auth.admin.updateUserById(id) {
                email = updateUserRequest.email
                userMetadata = buildJsonObject {
                    put("first_name", updateUserRequest.firstName)
                    put("last_name", updateUserRequest.lastName)
                    put("roles", rolesJsonArray)
                }
            }
        }
    }

}
