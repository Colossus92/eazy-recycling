import { useForm } from 'react-hook-form';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipLoader } from 'react-spinners';
import { Logo } from '@/components/ui/Logo';
import { Button } from '@/components/ui/button/Button';
import { PasswordFormField } from '@/components/ui/form/PasswordFormField';
import { toastService } from '@/components/ui/toast/toastService';
import { supabase } from '@/api/supabaseClient';

interface ChangePasswordFormInputs {
  password: string;
  confirmPassword: string;
}

export const ChangePasswordPage = () => {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormInputs>();
  const [formError, setFormError] = useState<string | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
  const navigate = useNavigate();

  const password = watch('password');

  useEffect(() => {
    // Check if user is authenticated (came from email link)
    const checkAuth = async () => {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      setIsAuthenticated(!!session);

      if (!session) {
        toastService.error('Ongeldige of verlopen reset link.');
      }
    };

    checkAuth();
  }, []);

  const onSubmit = async (data: ChangePasswordFormInputs) => {
    setFormError(null);

    try {
      const { error } = await supabase.auth.updateUser({
        password: data.password,
      });

      if (error) {
        setFormError('Wachtwoord wijzigen mislukt. Probeer het opnieuw.');
        toastService.error('Wachtwoord wijzigen mislukt.');
        console.error('Password update error:', error);
      } else {
        toastService.success('Wachtwoord succesvol gewijzigd!');
        // Sign out and redirect to login
        await supabase.auth.signOut();
        navigate('/login');
      }
    } catch (error) {
      console.error('Password update error:', error);
      setFormError('Er is een fout opgetreden. Probeer het later opnieuw.');
      toastService.error('Er is een fout opgetreden.');
    }
  };

  // Show loading while checking authentication
  if (isAuthenticated === null) {
    return (
      <div className="flex items-center justify-center h-screen w-screen bg-color-surface-secondary">
        <ClipLoader size={40} color="#4A6CF7" aria-label="Loading..." />
      </div>
    );
  }

  // Show error if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="flex items-center justify-center h-screen w-screen overflow-hidden py-20 bg-color-surface-secondary">
        <div
          className={
            'flex flex-col justify-center items-center sm:p-10 md:w-[488px] gap-8 p-6 bg-color-surface-primary rounded-radius-lg border border-solid border-color-border-primary'
          }
        >
          <div className={'flex flex-col items-center gap-5 self-stretch'}>
            <div className="flex items-center gap-2">
              <Logo />
            </div>
            <div className="flex flex-col items-center justify-center self-stretch gap-2">
              <h3>Link verlopen</h3>
              <span className="text-body-2 text-color-text-secondary text-center">
                De reset link is ongeldig of verlopen. Vraag een nieuwe link aan.
              </span>
            </div>
          </div>
          <Button
            label="Terug naar inloggen"
            fullWidth={true}
            onClick={() => navigate('/login')}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center h-screen w-screen overflow-hidden py-20 bg-color-surface-secondary">
      <div
        className={
          'flex flex-col justify-center items-center sm:p-10 md:w-[488px] gap-8 p-6 bg-color-surface-primary rounded-radius-lg border border-solid border-color-border-primary'
        }
      >
        <div className={'flex flex-col items-center gap-5 self-stretch'}>
          <div className="flex items-center gap-2">
            <Logo />
          </div>
          <div className="flex flex-col items-center justify-center self-stretch gap-2">
            <h3>Nieuw wachtwoord instellen</h3>
            <span className="text-body-2 text-color-text-secondary text-center">
              Voer uw nieuwe wachtwoord in.
            </span>
          </div>
        </div>
        <form
          className="flex flex-col items-start gap-5 self-stretch w-full"
          onSubmit={handleSubmit(onSubmit)}
        >
          <PasswordFormField
            title="Nieuw wachtwoord"
            placeholder="Voer nieuw wachtwoord in"
            formHook={{
              register,
              name: 'password',
              errors,
              rules: {
                required: 'Wachtwoord is verplicht',
                minLength: {
                  value: 6,
                  message: 'Wachtwoord moet minimaal 6 tekens bevatten',
                },
              },
            }}
          />
          <PasswordFormField
            title="Bevestig wachtwoord"
            placeholder="Bevestig nieuw wachtwoord"
            formHook={{
              register,
              name: 'confirmPassword',
              errors,
              rules: {
                required: 'Bevestig uw wachtwoord',
                validate: (value: string) =>
                  value === password || 'Wachtwoorden komen niet overeen',
              },
            }}
          />
          {formError && (
            <span className="text-body-2 text-color-status-error-dark">
              {formError}
            </span>
          )}
          <Button
            label={isSubmitting ? 'Opslaan...' : 'Wachtwoord opslaan'}
            fullWidth={true}
            type="submit"
            disabled={isSubmitting}
            icon={
              isSubmitting
                ? () => (
                    <ClipLoader
                      size={20}
                      color={'text-color-text-invert-primary'}
                      aria-label="Laad spinner"
                    />
                  )
                : undefined
            }
          />
        </form>
      </div>
    </div>
  );
};

export default ChangePasswordPage;
