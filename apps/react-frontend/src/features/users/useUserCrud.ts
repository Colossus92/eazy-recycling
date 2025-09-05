import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { DeleteResponse, User } from '@/types/api.ts';
import { userService } from '@/api/userService.ts';

export function useUserCrud() {
  const queryClient = useQueryClient();
  const {
    data: users = [],
    error,
    isLoading,
  } = useQuery<User[]>({
    queryKey: ['users'],
    queryFn: () => userService.list(),
  });
  const [query, setQuery] = useState<string>('');
  const displayedUsers = useMemo(
    () =>
      users.filter((users) => {
        return (
          users.email.toLowerCase().includes(query.toLowerCase()) ||
          users.firstName.toLowerCase().includes(query.toLowerCase()) ||
          users.lastName.toLowerCase().includes(query.toLowerCase())
        );
      }),
    [users, query]
  );
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<User | undefined>(undefined);
  const [deleting, setDeleting] = useState<User | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: (item: Omit<User, 'id'>) => userService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['users'] })
        .then(() => setIsAdding(false));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: User) => userService.remove(item.id),
    onSuccess: (response: DeleteResponse) => {
      if (response.success) {
        queryClient
          .invalidateQueries({ queryKey: ['users'] })
          .then(() => setDeleting(undefined));
      }
    },
  });

  const updateMutation = useMutation({
    mutationFn: (item: User) => userService.update(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['users'] })
        .then(() => setEditing(undefined));
    },
  });

  const updateProfileMutation = useMutation({
    mutationFn: (item: User) => userService.updateProfile(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['users'] })
        .then(() => setEditing(undefined));
    },
  });

  const create = async (item: Omit<User, 'id'>): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (item: User): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateProfile = async (item: User): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateProfileMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: User): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    displayedUsers,
    setQuery,
    isAdding,
    setIsAdding,
    editing,
    setEditing,
    deleting,
    setDeleting,
    create,
    update,
    updateProfile,
    remove,
    error,
    isLoading,
  };
}
