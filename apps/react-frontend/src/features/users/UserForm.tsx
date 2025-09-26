import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent } from 'react';
import Avatar from 'react-avatar';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { PasswordFormField } from '@/components/ui/form/PasswordFormField.tsx';
import { fallbackRender } from '@/utils/fallbackRender';
import { User } from '@/api/services/userService.ts';
import { CreateUserRequest } from '@/api/client';

interface UserFormProps {
  onCancel: () => void;
  onSubmit: (data: User | CreateUserRequest) => void;
  user?: User;
}

export interface UserFormValues extends FieldValues {
  id: string;
  email: string;
  roles: string[];
  firstName: string;
  lastName: string;
  password: string;
  lastSignInAt: string;
}

export const UserForm = ({ onCancel, onSubmit, user }: UserFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors },
  } = useForm<UserFormValues>({
    defaultValues: user
      ? {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        role: user.roles,
        lastSignInAt: user.lastSignInAt,
      }
      : undefined,
  });

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data: UserFormValues) => {
      try {
        if (isEditing && user) {
          // For editing existing users
          const updatedUser: User = {
            ...user,
            email: data.email || '',
            firstName: data.firstName || '',
            lastName: data.lastName || '',
            roles: data.roles || []
          };
          await onSubmit(updatedUser);
        } else {
          // For new users, create a CreateUserRequest object
          const createUserRequest: CreateUserRequest = {
            email: data.email || '',
            firstName: data.firstName || '',
            lastName: data.lastName || '',
            password: data.password || '',
            roles: data.roles || []
          };
          await onSubmit(createUserRequest);
        }
        onCancel(); // Only close the form if submission was successful
      } catch (error) {
        handleError(error);
      }
    })();
  };

  const isEditing = !!user;
  const formTitle = isEditing ? 'Gebruiker bewerken' : 'Gebruiker toevoegen';

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <form
        className="flex flex-col items-center self-stretch"
        onSubmit={(e) => submitForm(e)}
      >
        <FormTopBar title={formTitle} onClick={onCancel} />
        <div className="flex flex-col items-center self-stretch p-4 gap-4">
          <Avatar
            name={`${watch('firstName')} ${watch('lastName')}`}
            maxInitials={2}
            size={'64px'}
            round={true}
          />
          <div className="flex items-start self-stretch gap-4">
            <TextFormField
              title={'Voornaam'}
              placeholder={'Vul voornaam in'}
              formHook={{
                register,
                name: 'firstName',
                rules: {
                  required: 'Voornaam is verplicht',
                  validate: (value: string) =>
                    value?.trim() !== '' || 'Voornaam mag niet leeg zijn',
                },
                errors,
              }}
              value={user?.firstName}
            />
            <TextFormField
              title={'Achternaam'}
              placeholder={'Vul achternaam in'}
              formHook={{
                register,
                name: 'lastName',
                rules: {
                  required: 'Achternaam is verplicht',
                  validate: (value: string) =>
                    value?.trim() !== '' || 'Achternaam mag niet leeg zijn',
                },
                errors,
              }}
              value={user?.lastName}
            />
          </div>
          <TextFormField
            title={'Email'}
            placeholder={'Vul email in'}
            formHook={{
              register,
              name: 'email',
              rules: {
                required: 'Email is verplicht',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message:
                    'Ongeldig email, gebruik een geldig email adres zoals test@test.nl',
                },
              },
              errors,
            }}
            value={user?.email}
          />
          {!isEditing && (
            <PasswordFormField
              title={'Wachtwoord'}
              placeholder={'Vul wachtwoord in'}
              formHook={{
                register,
                name: 'password',
                rules: {
                  required: 'Wachtwoord is verplicht',
                  minLength: {
                    value: 8,
                    message: 'Wachtwoord moet minimaal 8 tekens bevatten',
                  },
                  pattern: {
                    value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$/,
                    message:
                      'Wachtwoord moet een kleine letter, hoofdletter, cijfer en speciaal karakter bevatten',
                  },
                },
                errors,
              }}
            />
          )}
          <SelectFormField
            title={'Rol'}
            placeholder={'Selecteer een rol'}
            options={[
              {
                value: 'admin',
                label: 'Admin',
              },
              {
                value: 'planner',
                label: 'Planner',
              },
              {
                value: 'chauffeur',
                label: 'Chauffeur',
              },
            ]}
            formHook={{
              register,
              name: 'roles',
              rules: { required: 'Rol is verplicht' },
              errors,
              control,
            }}
            value={user?.roles}
            isMulti={true}
          />
        </div>
        <FormActionButtons onClick={onCancel} item={user} />
      </form>

      <ErrorDialogComponent />
    </ErrorBoundary>
  );
};
