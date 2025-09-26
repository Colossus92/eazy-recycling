import { UserFormValues } from "@/features/users/UserForm";
import { CreateUserRequest, UpdateUserRequest, UserControllerApi, UserResponse } from "../client";
import { DriverControllerApi } from "../client/apis/driver-controller-api";
import { apiInstance } from "./apiInstance";


const userApi = new UserControllerApi(apiInstance.config);
const driverApi = new DriverControllerApi(apiInstance.config);

export type User = UserResponse;

export const userService = {
    getAll: () => userApi.getAllUsers().then((r) => r.data),
    create: (t: Omit<CreateUserRequest, 'id'>) => userApi.createUser(t).then((r) => r.data),
    update: (user: User) => {
        if (!user.email || !user.firstName || !user.lastName) {
            throw new Error('User must have email, first name and last name');
        }

        const updateUserRequest: UpdateUserRequest = {
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            roles: user.roles,
        };
        return userApi.updateUser(user.id, updateUserRequest).then((r) => r.data);
    },
    delete: (id: string) => userApi.deleteUser(id),
    listDrivers: () => driverApi.getAllDrivers().then((r) => r.data),
};

export function toUser(data: UserFormValues) {
    return {
      id: data.id,
      email: data.email,
      roles: data.roles,
      firstName: data.firstName,
      lastName: data.lastName,
      password: data.password,
      lastSignInAt: data.lastSignInAt,
    } as User;
  }