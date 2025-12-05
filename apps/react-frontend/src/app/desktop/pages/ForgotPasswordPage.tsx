import { useForm } from 'react-hook-form';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipLoader } from 'react-spinners';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { Logo } from '@/components/ui/Logo';
import { Button } from '@/components/ui/button/Button';
import { toastService } from '@/components/ui/toast/toastService';
import { supabase } from '@/api/supabaseClient';

interface ForgotPasswordFormInputs {
  email: string;
}

export const ForgotPasswordPage = () => {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormInputs>();
  const [emailSent, setEmailSent] = useState(false);

  const onSubmit = async (data: ForgotPasswordFormInputs) => {
    try {
      const { error } = await supabase.auth.resetPasswordForEmail(data.email, {
        redirectTo: `${window.location.origin}/change-password`,
      });

      if (error) {
        toastService.error('Er is een fout opgetreden. Probeer het later opnieuw.');
        console.error('Password reset error:', error);
      } else {
        setEmailSent(true);
        toastService.success('Controleer uw e-mail voor de reset link.');
      }
    } catch (error) {
      console.error('Password reset error:', error);
      toastService.error('Er is een fout opgetreden. Probeer het later opnieuw.');
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
            <h3>Wachtwoord vergeten</h3>
            <span className="text-body-2 text-color-text-secondary text-center">
              {emailSent
                ? 'We hebben een e-mail gestuurd met instructies om uw wachtwoord te resetten.'
                : 'Voer uw e-mailadres in om een reset link te ontvangen.'}
            </span>
          </div>
        </div>

        {!emailSent ? (
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
                rules: {
                  required: 'Email is verplicht',
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: 'Voer een geldig e-mailadres in',
                  },
                },
              }}
            />
            <Button
              label={isSubmitting ? 'Verzenden...' : 'Verstuur reset link'}
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
            <div className="flex justify-center w-full">
              <Link
                to="/login"
                className="text-body-2 text-color-brand-primary hover:underline"
                data-testid="back-to-login-link"
              >
                Terug naar inloggen
              </Link>
            </div>
          </form>
        ) : (
          <div className="flex flex-col items-center gap-5 self-stretch w-full">
            <Link
              to="/login"
              className="text-body-2 text-color-brand-primary hover:underline"
              data-testid="back-to-login-link"
            >
              Terug naar inloggen
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
