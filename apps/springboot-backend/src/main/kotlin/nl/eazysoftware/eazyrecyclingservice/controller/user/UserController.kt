package nl.eazysoftware.eazyrecyclingservice.controller.user

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.service.UserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    @PreAuthorize(HAS_ANY_ROLE)
    fun getAllUsers(): List<UserResponse> {
        return userService.getAllUsers()
            .map { UserResponse.from(it) }
    }

    @GetMapping(path = ["/{id}"])
    @PreAuthorize(HAS_ANY_ROLE)
    fun getUserById(@PathVariable id: String): UserResponse {
        val user = userService.getById(id)

        return UserResponse.from(user)
    }

    @PostMapping
    @PreAuthorize(HAS_ROLE_ADMIN)
    fun createUser(@RequestBody createUserRequest: CreateUserRequest) {
        userService.createUser(createUserRequest)
    }

    @PutMapping(path = ["/{id}"])
    @PreAuthorize(HAS_ROLE_ADMIN)
    fun updateUser(@PathVariable id: String, @RequestBody updateUserRequest: UpdateUserRequest) {
        userService.updateUser(id, updateUserRequest)
    }

    @PutMapping(path = ["/{id}/profile"])
    @PreAuthorize("$HAS_ANY_ROLE and #id == authentication.principal.claims['sub']")
    fun updateProfile(@PathVariable id: String, @RequestBody updateUserRequest: UpdateUserRequest) {
        userService.updateProfile(id, updateUserRequest)
    }

    @DeleteMapping(path = ["/{id}"])
    @PreAuthorize(HAS_ROLE_ADMIN)
    fun deleteUser(@PathVariable id: String) {
        userService.deleteUser(id)
    }

}