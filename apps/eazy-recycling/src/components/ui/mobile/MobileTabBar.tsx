import { MobileTab } from './MobileTab';

interface MobileTabBarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  transportType: string;
}

export const MobileTabBar = ({
  activeTab,
  setActiveTab,
  transportType,
}: MobileTabBarProps) => {
  return (
    <div className="flex pb-[1px] border-b border-solid border-color-border-primary w-full overflow-x-auto">
      <MobileTab
        text="Algemeen"
        activeTab={activeTab}
        setActiveTab={setActiveTab}
      />
      {transportType === 'WASTE' && (
        <MobileTab
          text="Handtekeningen"
          activeTab={activeTab}
          setActiveTab={setActiveTab}
        />
      )}
    </div>
  );
};
