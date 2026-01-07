import CaretLeft from "@/assets/icons/CaretLeft.svg?react";
import { Button } from "@/components/ui/button/Button";
import { useNavigate } from "react-router-dom";
import { ContainerSelectFormField } from "@/components/ui/form/selectfield/ContainerSelectFormField";
import { FormProvider } from "react-hook-form";
import { AddressFormField } from "@/components/ui/form/addressformfield";
import { useMobileContainerLocationForm } from "../../../features/mobile/containerlocation/useMobileContainerLocationForm";
import { toastService } from "@/components/ui/toast/toastService";

export const MobileContainerLocationPage = () => {
  const navigate = useNavigate();
  const navigateBack = () => navigate(-1);
  const { formContext, handleSubmit, isSubmitting, currentLocation } = useMobileContainerLocationForm();

  const onSubmit = formContext.handleSubmit(async (values) => {
    try {
      await handleSubmit(values);
      toastService.success('Containerlocatie succesvol bijgewerkt');
      navigateBack();
    } catch (error) {
      toastService.error('Fout bij het bijwerken van de containerlocatie');
    }
  });

  return (
    <div className="flex flex-col w-full h-full">
      <div className="flex items-center py-2 px-4 gap-3 border-b border-solid border-color-border-primary">
        <Button
          variant="icon"
          icon={CaretLeft}
          showText={false}
          onClick={navigateBack}
        />
        <h4>Containerlocatie doorgeven</h4>
      </div>
      <FormProvider {...formContext}>
        <form id="container-location-form" onSubmit={onSubmit} className="flex flex-col flex-1">
          <div className="flex flex-col items-start self-stretch flex-1 p-4 gap-4">
              <div className="flex flex-col items-start self-stretch gap-1">
                <ContainerSelectFormField
                  formHook={{
                    register: formContext.register,
                    name: 'containerId',
                    errors: formContext.formState.errors,
                    control: formContext.control,
                  }}
                  required={true}
                  />
                {currentLocation && (
                  <span className="text-caption-1 text-color-text-secondary italic break-words">
                    Huidige locatie: {currentLocation}
                  </span>
                )}
              </div>
              <div className="flex flex-col items-start self-stretch gap-1">
                <AddressFormField
                  control={formContext.control}
                  name="location"
                  label="Locatie van container"
                  required={true}
                  isNoLocationAllowed={false}
                />
              </div>
            </div>
            <div className="flex justify-end items-center py-3 px-4 gap-4 border-t border-solid border-color-border-primary">
              <div className="flex-1">
                <Button
                  variant="secondary"
                  size="medium"
                  label="Annuleren"
                  onClick={navigateBack}
                  fullWidth
                  disabled={isSubmitting}
                />
              </div>
              <div className="flex-1">
                <Button
                  variant="primary"
                  size="medium"
                  label="Bewaren"
                  type="submit"
                  fullWidth
                  disabled={isSubmitting}
                />
              </div>
            </div>
        </form>
      </FormProvider>
    </div>
  );
};
