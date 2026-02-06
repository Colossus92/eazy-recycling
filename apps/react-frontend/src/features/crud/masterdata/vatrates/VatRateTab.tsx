import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { VatRateResponse } from '@/api/client';
import { useVatRates } from '@/features/crud/masterdata/vatrates/useVatRates';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const VatRatesTab = () => {
  const { items, setSearchQuery, isLoading, errorHandling } = useVatRates();

  const columns: Column<VatRateResponse>[] = [
    {
      key: 'vatCode',
      label: 'BTW Code',
      width: '15',
      accessor: (item) => item.vatCode,
    },
    {
      key: 'percentage',
      label: 'Percentage',
      width: '15',
      accessor: (item) => `${item.percentage}%`,
    },
    {
      key: 'countryCode',
      label: 'Land',
      width: '10',
      accessor: (item) => item.countryCode,
    },
    {
      key: 'description',
      label: 'Beschrijving',
      width: '25',
      accessor: (item) => item.description,
    },
    {
      key: 'taxScenario',
      label: 'Type',
      width: '20',
      accessor: (item) => item.taxScenario,
    },
  ];

  const data: DataTableProps<VatRateResponse> = {
    columns,
    items,
  };

  return (
    <MasterDataTab
      data={data}
      searchQuery={(query) => setSearchQuery(query)}
      renderEmptyState={() => (
        <EmptyState
          icon={ArchiveBook}
          text={'Geen BTW tarieven gevonden'}
        />
      )}
      isLoading={isLoading}
      errorHandling={errorHandling}
    />
  );
};
