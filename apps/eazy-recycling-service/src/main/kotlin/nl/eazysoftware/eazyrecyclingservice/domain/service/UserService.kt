package nl.eazysoftware.eazyrecyclingservice.domain.service

import io.github.jan.supabase.auth.user.UserInfo
import nl.eazysoftware.eazyrecyclingservice.controller.user.CreateUserRequest
import nl.eazysoftware.eazyrecyclingservice.repository.UserRepository
import org.springframework.stereotype.Service

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

}
