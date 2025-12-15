import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { ProductCategoryResponse } from '@/api/client';
import { useProductCategoriesCrud } from '@/features/crud/masterdata/productcategories/useProductCategories';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { ProductCategoryForm } from './ProductCategoryForm';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const ProductCategoriesTab = () => {
  const { read, form, deletion } = useProductCategoriesCrud();

  const columns: Column<ProductCategoryResponse>[] = [
    {
      key: 'code',
      label: 'Code',
      width: '15',
      accessor: (item) => item.code,
    },
    {
      key: 'name',
      label: 'Naam',
      width: '25',
      accessor: (item) => item.name,
    },
    {
      key: 'description',
      label: 'Beschrijving',
      width: '60',
      accessor: (item) => item.description,
    },
  ];

  const data: DataTableProps<ProductCategoryResponse> = {
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
            text={'Geen productcategorieën gevonden'}
            onClick={open}
          />
        )}
        isLoading={read.isLoading}
        errorHandling={read.errorHandling}
      />
      <ProductCategoryForm
        isOpen={form.isOpen}
        onCancel={form.close}
        onSubmit={form.submit}
        initialData={form.item}
      />
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        onDelete={() => deletion.item && deletion.confirm(deletion.item)}
        title={'Productcategorie verwijderen'}
        description={`Weet u zeker dat u productcategorie "${deletion.item?.name}" wilt verwijderen?`}
      />
    </>
  );
};
