import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Tab } from '@/components/ui/tab/Tab';
import { InvoicesTab } from '@/features/invoices/components/InvoicesTab';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { ReactNode, useState } from 'react';

export const FinancieelManagement = () => {
  const [selectedIndex, setSelectedIndex] = useState(0);

  const tabs: {name: string, disabled: boolean, component: () => ReactNode}[] = [
    {name: "Facturen", disabled: false, component: () => <InvoicesTab key={`invoices-${selectedIndex}`} />},
    {name: "Kasbonnen", disabled: true, component: () => <div className="flex items-center justify-center h-full text-color-text-secondary">Kasbonnen worden binnenkort ondersteund</div>},
  ];

  return (
    <ContentContainer title={'Financieel'}>
      <div className="flex-1 flex flex-col items-start self-stretch gap-4 rounded-b-radius-lg border-color-border-primary overflow-hidden">
        <TabGroup
          selectedIndex={selectedIndex}
          onChange={setSelectedIndex}
          className="w-full flex-1 flex flex-col min-h-0"
        >
          <TabList className="relative z-10">
            {
              tabs.map((tab) => (
                <Tab label={tab.name} key={tab.name}  disabled={tab.disabled} />
              ))
            }
          </TabList>
          <TabPanels
            className="
              flex flex-col flex-1
              bg-color-surface-primary
              border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary
              pt-4
              gap-4
              min-h-0
              -mt-[2px]
            "
          >
            {
              tabs.map((tab, index) => (
                <TabPanel key={index} className="flex-1 flex flex-col items-start self-stretch gap-4 overflow-hidden">
                  {tab.component()}
                </TabPanel>
              ))
            }
          </TabPanels>
        </TabGroup>
      </div>
    </ContentContainer>
  );
};

export default FinancieelManagement;
