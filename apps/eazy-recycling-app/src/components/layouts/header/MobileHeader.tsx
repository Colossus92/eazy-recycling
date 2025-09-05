import { AvatarMenu } from '@/components/layouts/header/AvatarMenu';

export const MobileHeader = () => {
  return (
    <div className="flex w-full items-center py-2 px-4 gap-3 border-b border-solid border-color-border-primary">
      <div className="flex-1">
        <h4>Planning</h4>
      </div>
      <AvatarMenu hideProfileItem={true} />
    </div>
  );
};
