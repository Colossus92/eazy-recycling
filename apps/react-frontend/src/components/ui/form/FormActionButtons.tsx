import { Button } from '../button/Button.tsx';

export const FormActionButtons = <T,>(props: {
  onClick: () => void;
  item: T | undefined;
  disabled?: boolean;
}) => {
  return (
    <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary">
      <div className={'flex-1'}>
        <Button
          variant={'secondary'}
          label={'Annuleren'}
          onClick={props.onClick}
          fullWidth={true}
          data-testid={'cancel-button'}
        />
      </div>
      <div className={'flex-1'}>
        <Button
          type="submit"
          variant={'primary'}
          label={props.item ? 'Aanpassen' : 'Toevoegen'}
          fullWidth={true}
          disabled={props.disabled}
          data-testid={'submit-button'}
        />
      </div>
    </div>
  );
};
