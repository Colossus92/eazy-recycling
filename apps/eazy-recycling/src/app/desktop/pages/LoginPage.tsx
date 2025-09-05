import { useForm } from 'react-hook-form';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipLoader } from 'react-spinners';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { Logo } from '@/components/ui/Logo';
import { Button } from '@/components/ui/button/Button';
import { PasswordFormField } from '@/components/ui/form/PasswordFormField';
import { toastService } from '@/components/ui/toast/toastService';
import { supabase } from '@/api/supabaseClient';
import { useMobileHook } from '@/hooks/useMobileHook';

interface LoginFormInputs {
  email: string;
  password: string;
}

export const LoginPage = () => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormInputs>();
  const [formError, setFormError] = useState<string | null>(null);
  const navigate = useNavigate();
  const { isMobileSession } = useMobileHook();

  const onSubmit = async (data: LoginFormInputs) => {
    setFormError(null);
    try {
      const { error } = await supabase.auth.signInWithPassword({
        email: data.email,
        password: data.password,
      });
      if (error) {
        setFormError('Inloggen mislukt');
        toastService.error('Inloggen mislukt.');
      } else {
        navigate(isMobileSession ? '/mobile' : '/');
      }
    } catch (error) {
      console.error('Login error:', error);
      setFormError('Er is een probleem opgetreden bij het inloggen.');
      toastService.error(
        'Er is een probleem opgetreden bij het inloggen. Probeer het later opnieuw.'
      );
    }
  };

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
            <h3>Welkom bij Eazy Recycling</h3>
            <span className="text-body-2 text-color-text-secondary">
              Voer uw inloggegevens in
            </span>
          </div>
        </div>
        <form
          className="flex flex-col items-start gap-5 self-stretch w-full"
          onSubmit={handleSubmit(onSubmit)}
        >
          <TextFormField
            title="Email"
            placeholder="Voer emailadres in"
            formHook={{
              register,
              name: 'email',
              errors,
              rules: { required: 'Email is verplicht' },
            }}
          />
          <PasswordFormField
            title="Wachtwoord"
            placeholder="Voer wachtwoord in"
            formHook={{
              register,
              name: 'password',
              errors,
              rules: { required: 'Wachtwoord is verplicht' },
            }}
          />
          {formError && (
            <span className="text-body-2 text-color-status-error-dark">
              {formError}
            </span>
          )}
          <Button
            label={isSubmitting ? 'Inloggen...' : 'Inloggen'}
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
            iconPosition="left"
          />
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
