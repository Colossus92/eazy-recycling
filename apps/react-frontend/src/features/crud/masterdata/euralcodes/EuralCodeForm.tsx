import { Eural } from "@/api/client";
import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { FormEvent } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";

interface EuralCodeFormProps {
    isOpen: boolean;
    onCancel: () => void;
    onSubmit: (eural: Eural) => void;
    initialData?: Eural;
}

export const EuralCodeForm = ({ isOpen, onCancel, onSubmit, initialData }: EuralCodeFormProps) => {
    const { handleError, ErrorDialogComponent } = useErrorHandling();
    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<Eural>({
        values: initialData,
    });

    const cancel = () => {
        reset({code: '', description: ''});
        onCancel();
    }

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await handleSubmit(async (data) => {
            try {
                await onSubmit({
                    code: data.code,
                    description: data.description,
                });
                cancel();
            } catch (error) {
                handleError(error);
            }
        })();
    };

    return (
        <FormDialog isOpen={isOpen} setIsOpen={cancel}>
            <ErrorBoundary fallbackRender={fallbackRender}>
                <form
                    className="flex flex-col items-center self-stretch"
                    onSubmit={(e) => submitForm(e)}
                >
                    <FormTopBar title={initialData ? "Eural code bewerken" : "Eural code toevoegen"} onClick={cancel} />
                    <div className="flex flex-col items-center self-stretch p-4 gap-4">
                        <div className="flex items-start self-stretch gap-4">
                            <TextFormField
                                title={'Code'}
                                placeholder={'Vul een code in'}
                                formHook={{
                                    register,
                                    name: 'code',
                                    rules: { required: 'Code is verplicht' },
                                    errors,
                                }}
                                disabled={Boolean(initialData?.code)}
                            />
                            <TextFormField
                                title={'Beschrijving'}
                                placeholder={'Vul een beschrijving in'}
                                formHook={{
                                    register,
                                    name: 'description',
                                    rules: { required: 'Beschrijving is verplicht' },
                                    errors,
                                }}
                            />
                        </div>
                    </div>
                    <FormActionButtons onClick={cancel} item={undefined} />
                </form>
                <ErrorDialogComponent />
            </ErrorBoundary>
        </FormDialog>
    )
}
