package nl.eazysoftware.eazyrecyclingservice.controller.user

import nl.eazysoftware.eazyrecyclingservice.domain.service.UserService
import org.springframework.web.bind.annotation.*

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

    @PutMapping(path = ["/{id}"])
    fun updateUser(@PathVariable id: String, @RequestBody updateUserRequest: UpdateUserRequest) {
        userService.updateUser(id, updateUserRequest)
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteUser(@PathVariable id: String) {
        userService.deleteUser(id)
    }

}