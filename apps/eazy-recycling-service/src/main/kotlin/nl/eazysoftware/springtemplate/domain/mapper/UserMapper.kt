package nl.eazysoftware.springtemplate.domain.mapper

import org.mapstruct.Mapper

@Mapper
interface UserMapper {
    fun toDto(user: User): UserDto
    fun toBean(userDto: UserDto): User
}