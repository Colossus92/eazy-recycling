import Indicator from '@/assets/blocks/Indicator.svg?react';

interface MobileTabProps {
  text: string;
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export const MobileTab = ({
  text,
  activeTab,
  setActiveTab,
}: MobileTabProps) => {
  const isActive = activeTab === text;
  const textColor = isActive
    ? 'text-color-brand-primary'
    : 'text-color-text-secondary';

  return (
    <div
      className="flex flex-col items-center self-stretch w-[135px]"
      onClick={() => setActiveTab(text)}
    >
      <div className="flex flex-col content-end items-center self-stretch px-4 pt-3.5">
        <span className={`text-subtitle-tab ${textColor}`}>{text}</span>
      </div>
      <Indicator className={isActive ? 'block' : 'hidden'} />
    </div>
  );
};
