import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { Button } from '@/components/ui/button/Button';
import { SyncPreviewResponse } from '@/api/client';
import ArrowCounterClockwise from '@/assets/icons/ArrowCounterClockwise.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import Plus from '@/assets/icons/Plus.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import X from '@/assets/icons/X.svg?react';

interface MaterialPriceSyncDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSync: () => void;
  preview: SyncPreviewResponse | undefined;
  isLoading: boolean;
  isExecuting: boolean;
  hasChanges: boolean | undefined;
}

export const MaterialPriceSyncDialog = ({
  isOpen,
  onClose,
  onSync,
  preview,
  isLoading,
  isExecuting,
  hasChanges,
}: MaterialPriceSyncDialogProps) => {
  if (!isOpen) return null;
  const formatPrice = (price: number | undefined) => {
    if (price === undefined || price === null) return '-';
    return `€ ${price.toFixed(2)}`;
  };

  const getPriceStatusIcon = (status: number) => {
    switch (status) {
      case 1:
        return <CaretRight className="w-4 h-4 text-color-success -rotate-90" />;
      case 2:
        return <CaretDown className="w-4 h-4 text-color-error" />;
      default:
        return null;
    }
  };

  const getPriceStatusColor = (status: number) => {
    switch (status) {
      case 1:
        return 'text-color-success';
      case 2:
        return 'text-color-error';
      default:
        return 'text-color-text-primary';
    }
  };

  return (
    <FormDialog isOpen={isOpen} setIsOpen={() => onClose()} width="w-[800px]">
      <div className="flex flex-col w-full">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-color-border">
          <h2 className="text-xl font-semibold">
            Prijzen synchroniseren met externe app
          </h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-color-surface-secondary rounded"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex flex-col gap-6 p-6 max-h-[70vh] overflow-y-auto">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <ArrowCounterClockwise className="w-8 h-8 animate-spin text-color-brand-primary" />
              <span className="ml-2 text-color-text-secondary">
                Voorvertoning laden...
              </span>
            </div>
          ) : preview ? (
            <>
              {/* Summary */}
              <div className="grid grid-cols-4 gap-4 p-4 bg-color-surface-secondary rounded-lg">
                <div className="text-center">
                  <div className="text-2xl font-semibold text-color-success">
                    {preview.summary.totalToCreate}
                  </div>
                  <div className="text-sm text-color-text-secondary">
                    Nieuw aan te maken
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-semibold text-color-brand-primary">
                    {preview.summary.totalToUpdate}
                  </div>
                  <div className="text-sm text-color-text-secondary">
                    Bij te werken
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-semibold text-color-error">
                    {preview.summary.totalToDelete}
                  </div>
                  <div className="text-sm text-color-text-secondary">
                    Te verwijderen
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-semibold text-color-text-secondary">
                    {preview.summary.totalUnchanged}
                  </div>
                  <div className="text-sm text-color-text-secondary">
                    Ongewijzigd
                  </div>
                </div>
              </div>

              {/* To Create */}
              {preview.toCreate.length > 0 && (
                <div>
                  <h3 className="flex items-center gap-2 text-lg font-medium mb-3">
                    <Plus className="w-5 h-5 text-color-success" />
                    Nieuw aan te maken ({preview.toCreate.length})
                  </h3>
                  <div className="border border-color-border rounded-lg overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-color-surface-secondary">
                        <tr>
                          <th className="px-4 py-2 text-left text-sm font-medium text-color-text-secondary">
                            Materiaal
                          </th>
                          <th className="px-4 py-2 text-right text-sm font-medium text-color-text-secondary">
                            Prijs
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {preview.toCreate.map((item) => (
                          <tr
                            key={item.materialId}
                            className="border-t border-color-border"
                          >
                            <td className="px-4 py-2">{item.materialName}</td>
                            <td className="px-4 py-2 text-right">
                              {formatPrice(item.currentPrice)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* To Update */}
              {preview.toUpdate.length > 0 && (
                <div>
                  <h3 className="flex items-center gap-2 text-lg font-medium mb-3">
                    <ArrowCounterClockwise className="w-5 h-5 text-color-brand-primary" />
                    Bij te werken ({preview.toUpdate.length})
                  </h3>
                  <div className="border border-color-border rounded-lg overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-color-surface-secondary">
                        <tr>
                          <th className="px-4 py-2 text-left text-sm font-medium text-color-text-secondary">
                            Materiaal
                          </th>
                          <th className="px-4 py-2 text-right text-sm font-medium text-color-text-secondary">
                            Oude prijs
                          </th>
                          <th className="px-4 py-2 text-right text-sm font-medium text-color-text-secondary">
                            Nieuwe prijs
                          </th>
                          <th className="px-4 py-2 text-center text-sm font-medium text-color-text-secondary">
                            Status
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {preview.toUpdate.map((item) => (
                          <tr
                            key={item.materialId}
                            className="border-t border-color-border"
                          >
                            <td className="px-4 py-2">{item.materialName}</td>
                            <td className="px-4 py-2 text-right text-color-text-secondary">
                              {formatPrice(item.lastSyncedPrice)}
                            </td>
                            <td
                              className={`px-4 py-2 text-right font-medium ${getPriceStatusColor(item.priceStatus)}`}
                            >
                              {formatPrice(item.currentPrice)}
                            </td>
                            <td className="px-4 py-2">
                              <div className="flex items-center justify-center gap-1">
                                {getPriceStatusIcon(item.priceStatus)}
                                <span
                                  className={`text-sm ${getPriceStatusColor(item.priceStatus)}`}
                                >
                                  {item.priceStatusLabel}
                                </span>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* To Delete */}
              {preview.toDelete.length > 0 && (
                <div>
                  <h3 className="flex items-center gap-2 text-lg font-medium mb-3">
                    <TrashSimple className="w-5 h-5 text-color-error" />
                    Te verwijderen ({preview.toDelete.length})
                  </h3>
                  <div className="border border-color-border rounded-lg overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-color-surface-secondary">
                        <tr>
                          <th className="px-4 py-2 text-left text-sm font-medium text-color-text-secondary">
                            Product
                          </th>
                          <th className="px-4 py-2 text-right text-sm font-medium text-color-text-secondary">
                            Externe ID
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {preview.toDelete.map((item) => (
                          <tr
                            key={item.externalProductId}
                            className="border-t border-color-border"
                          >
                            <td className="px-4 py-2">{item.productName}</td>
                            <td className="px-4 py-2 text-right text-color-text-secondary">
                              {item.externalProductId}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* No changes message */}
              {!hasChanges && (
                <div className="text-center py-8 text-color-text-secondary">
                  <ArrowCounterClockwise className="w-12 h-12 mx-auto mb-2 text-color-text-tertiary" />
                  <p>Alle prijzen zijn al gesynchroniseerd.</p>
                  <p className="text-sm">
                    Er zijn geen wijzigingen om door te voeren.
                  </p>
                </div>
              )}
            </>
          ) : null}
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 p-6 border-t border-color-border">
          <Button
            variant="secondary"
            label="Annuleren"
            onClick={onClose}
            disabled={isExecuting}
          />
          <Button
            variant="primary"
            label={isExecuting ? 'Synchroniseren...' : 'Synchroniseren'}
            icon={isExecuting ? ArrowCounterClockwise : undefined}
            onClick={onSync}
            disabled={!hasChanges || isExecuting || isLoading}
          />
        </div>
      </div>
    </FormDialog>
  );
};
