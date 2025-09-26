import { useQueryClient } from '@tanstack/react-query';
import Avatar from 'react-avatar';
import { Column } from '@/features/crud/ContentTable.tsx';
import { CrudPage } from '@/features/crud/CrudPage.tsx';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import IdentificationCard from '@/assets/icons/IdentificationCard.svg?react';
import { useUserCrud } from '@/features/users/useUserCrud.ts';
import { UserForm } from '@/features/users/UserForm.tsx';
import { formatDateString } from '@/utils/dateUtils.ts';
import { User } from '@/api/services/userService.ts';
import { CreateUserRequest } from '@/api/client';

export const UserManagement = () => {
  const queryClient = useQueryClient();
  const {
    displayedUsers,
    setQuery,
    isAdding,
    setIsAdding,
    setEditing,
    setDeleting,
    editing,
    deleting,
    create,
    update,
    remove,
    error,
    isLoading,
  } = useUserCrud();

  const columns: Column<User>[] = [
    {
      key: 'email',
      label: 'Naam',
      accessor: (item: User) => {
        return (
          <div className="flex items-center self-stretch gap-2">
            <Avatar
              name={`${item.firstName} ${item.lastName}`}
              maxInitials={2}
              size={'40px'}
              round={true}
            />
            <div className="flex flex-col content-center items-start">
              <span className="text-body-2 text-color-text-primary">
                {item.firstName} {item.lastName}
              </span>
              <span className="text-caption-1 text-color-text-secondary">
                {item.email}
              </span>
            </div>
          </div>
        );
      },
    },
    {
      key: 'roles',
      label: 'Rollen',
      accessor: (item: User) => item.roles.join(', '),
    },
    {
      key: 'lastSignInAt',
      label: 'Laatst ingelogd',
      accessor: (item: User) => formatDateString(item.lastSignInAt),
    },
  ];

  return (
    <CrudPage<User>
      title={'Gebruikersbeheer'}
      data={{
        items: displayedUsers,
        columns,
        setQuery,
      }}
      dialogs={{
        add: {
          open: isAdding,
          onClose: () => setIsAdding(false),
          onSave: (data) => {
            return create(data as CreateUserRequest);
          },
        },
        update: {
          open: !!editing,
          item: editing,
          onClose: () => setEditing(undefined),
          onSave: (data) => {
            // We know this is a User because we're in the update dialog
            return update(data as User);
          },
        },
        delete: {
          title: 'Gebruiker verwijderen',
          description: `Weet u zeker dat u gebruiker met email ${deleting?.email} wilt verwijderen?`,
          open: !!deleting,
          item: deleting,
          onClose: () => setDeleting(undefined),
          onConfirm: (data) => {
            return remove(data);
          },
        },
      }}
      actions={{
        onAdd: setIsAdding,
        onDelete: setDeleting,
        onEdit: setEditing,
      }}
      renderForm={(close, onSubmit, itemToEdit) => {
        // Create a properly typed onSubmit function that can handle both User and CreateUserRequest
        const typedOnSubmit = (data: User | CreateUserRequest) => {
          onSubmit(data as any);
        };
        return (
          <UserForm onCancel={close} onSubmit={typedOnSubmit} user={itemToEdit} />
        );
      }}
      renderEmptyState={(open) => (
        <EmptyState
          icon={IdentificationCard}
          text={'Geen gebruikers gevonden'}
          onClick={open}
        />
      )}
      error={error}
      onReset={() => {
        queryClient.invalidateQueries({ queryKey: ['users'] }).catch(() => {});
      }}
      isLoading={isLoading}
    />
  );
};

export default UserManagement;
