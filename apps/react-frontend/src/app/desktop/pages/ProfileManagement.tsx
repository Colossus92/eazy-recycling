import Avatar from 'react-avatar';
import { useForm } from 'react-hook-form';
import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ContentContainer } from '@/components/layouts/ContentContainer.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { UserFormValues } from '@/features/users/UserForm.tsx';
import { toUser, User } from '@/api/services/userService.ts';
import { ErrorDialog } from '@/components/ui/dialog/ErrorDialog.tsx';
import { supabase } from '@/api/supabaseClient.tsx';
import { useUserCrud } from '@/features/users/useUserCrud.ts';

export const ProfileManagement = () => {
  const navigate = useNavigate();
  const [isErrorDialogOpen, setIsErrorDialogOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const { updateProfile } = useUserCrud();
  const { data: userProfile, isLoading } = useQuery<User>({
    queryKey: ['user'],
    queryFn: async () => {
      const { data: userData, error } = await supabase.auth.getUser();

      if (error) {
        throw new Error(error.message);
      }

      if (!userData || !userData.user) {
        throw new Error('User not found');
      }

      // Map Supabase user to our User type
      return {
        id: userData.user.id,
        firstName: userData.user.user_metadata.first_name || '',
        lastName: userData.user.user_metadata.last_name || '',
        email: userData.user.email || '',
        roles: userData.user.user_metadata.roles,
        lastSignInAt: userData.user.last_sign_in_at,
      } as User;
    },
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<UserFormValues>();

  useEffect(() => {
    if (userProfile) {
      reset({
        id: userProfile.id,
        firstName: userProfile.firstName,
        lastName: userProfile.lastName,
        email: userProfile.email,
        roles: userProfile.roles,
        lastSignInAt: userProfile.lastSignInAt,
      });
    }
  }, [userProfile, reset]);

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        // Ensure id is set from the profile
        if (userProfile) {
          data.id = userProfile.id;
        }

        await updateProfile(toUser(data));
        navigate('/');
      } catch (error) {
        // Show error dialog and keep form open
        setErrorMessage(
          error instanceof Error ? error.message : 'Er is iets misgegaan'
        );
        setIsErrorDialogOpen(true);
      }
    })();
  };

  if (isLoading) {
    return <ContentContainer title={'Profiel'}>Laden...</ContentContainer>;
  }

  return (
    <ContentContainer title={'Profiel'}>
      <div
        className={
          'w-fullflex flex-col items-start gap-4 pt-4 w-[640px] border border-solid border-color-border-primary rounded-2xl bg-color-surface-primary'
        }
      >
        <div className={'flex items-center self-stretch px-4 justify-between'}>
          <div className={'flex flex-col items-start gap-1'}>
            <h4 className={'text-color-text-primary'}>Mijn Profiel</h4>
            <span className={'text-color-text-secondary'}>
              Beheer je profiel
            </span>
          </div>
        </div>
        <form onSubmit={(e) => submitForm(e)}>
          <div
            className={
              'flex justify-center items-start self-stretch p-4 gap-3 border-t border-solid border-color-border-primary'
            }
          >
            <div className={'w-full flex flex-col items-center gap-4 px-4'}>
              <Avatar
                name={`${userProfile?.firstName || ''} ${userProfile?.lastName || ''}`}
                size="64px"
                round={true}
              />
              <div className="flex w-full gap-4">
                <div className="flex-1">
                  <TextFormField
                    title={'Voornaam'}
                    placeholder={'Vul voornaam in'}
                    formHook={{
                      register,
                      name: 'firstName',
                      rules: { required: 'Voornaam is verplicht' },
                      errors,
                    }}
                  />
                </div>
                <div className="flex-1">
                  <TextFormField
                    title={'Achternaam'}
                    placeholder={'Vul achternaam in'}
                    formHook={{
                      register,
                      name: 'lastName',
                      rules: { required: 'Achternaam is verplicht' },
                      errors,
                    }}
                  />
                </div>
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
                      value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                      message: 'Vul een geldig emailadres in',
                    },
                  },
                  errors,
                }}
              />
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
                  rules: {},
                  errors,
                  control,
                }}
                isMulti={true}
                disabled={true}
              />
            </div>
          </div>
          <FormActionButtons onClick={() => navigate('/')} item={userProfile} />
        </form>
      </div>
      <ErrorDialog
        isOpen={isErrorDialogOpen}
        setIsOpen={setIsErrorDialogOpen}
        errorMessage={errorMessage}
      />
    </ContentContainer>
  );
};

export default ProfileManagement;
