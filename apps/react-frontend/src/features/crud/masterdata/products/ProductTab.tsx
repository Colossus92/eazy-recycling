import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { ProductResponse } from '@/api/client';
import { useProductsCrud } from '@/features/crud/masterdata/products/useProducts';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { ProductForm } from './ProductForm';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const ProductsTab = () => {
  const { read, form, deletion } = useProductsCrud();

  const columns: Column<ProductResponse>[] = [
    {
      key: 'code',
      label: 'Code',
      width: '12',
      accessor: (item) => item.code,
    },
    {
      key: 'name',
      label: 'Naam',
      width: '20',
      accessor: (item) => item.name,
    },
    {
      key: 'unitOfMeasure',
      label: 'Eenheid',
      width: '10',
      accessor: (item) => item.unitOfMeasure,
    },
    {
      key: 'vatCode',
      label: 'BTW Code',
      width: '10',
      accessor: (item) => item.vatCode,
    },
    {
      key: 'glAccountCode',
      label: 'Grootboekrekening',
      width: '12',
      accessor: (item) => item.glAccountCode,
    },
    {
      key: 'defaultPrice',
      label: 'Standaardprijs',
      width: '12',
      accessor: (item) =>
        item.defaultPrice !== undefined
          ? `€ ${item.defaultPrice.toFixed(2)}`
          : undefined,
    },
    {
      key: 'description',
      label: 'Beschrijving',
      width: '24',
      accessor: (item) => item.description,
    },
  ];

  const data: DataTableProps<ProductResponse> = {
    columns,
    items: read.items,
  };

  return (
    <>
      <MasterDataTab
        data={data}
        searchQuery={(query) => read.setSearchQuery(query)}
        openAddForm={form.openForCreate}
        editAction={(item) => form.openForEdit(item)}
        removeAction={(item) => deletion.initiate(item)}
        renderEmptyState={(open) => (
          <EmptyState
            icon={ArchiveBook}
            text={'Geen producten gevonden'}
            onClick={open}
          />
        )}
        isLoading={read.isLoading}
        errorHandling={read.errorHandling}
      />
      <ProductForm
        isOpen={form.isOpen}
        onCancel={form.close}
        onSubmit={form.submit}
        initialData={form.item}
      />
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        onDelete={() => deletion.item && deletion.confirm(deletion.item)}
        title={'Product verwijderen'}
        description={`Weet u zeker dat u product "${deletion.item?.name}" wilt verwijderen?`}
      />
    </>
  );
};
