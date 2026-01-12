import { Button } from '@/components/ui/button/Button';
import { toastService } from '@/components/ui/toast/toastService';
import { ErrorBoundary } from 'react-error-boundary';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { fallbackRender } from '@/utils/fallbackRender';
import { useLmaImport } from '../hooks/useLmaImport';
import { useLmaImportErrors } from '../hooks/useLmaImportErrors';
import { LmaImportErrorsTable } from './LmaImportErrorsTable';
import { useRef, useState } from 'react';
import CarbonDocumentAttachment from '@/assets/icons/CarbonDocumentAttachment.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';

export const LmaImportTab = () => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const { errors, isLoading, error, refetch } = useLmaImportErrors();
  const {
    importCsv,
    isImporting,
    importResult,
    reset,
    deleteAllErrors,
    isDeletingErrors,
  } = useLmaImport();

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      reset(); // Clear previous import result
    }
  };

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  const handleImport = async () => {
    if (!selectedFile) {
      toastService.error('Selecteer eerst een CSV bestand');
      return;
    }

    try {
      const result = await importCsv(selectedFile);

      if (result.success) {
        toastService.success(
          `Import voltooid: ${result.successfulImports} afvalstromen geïmporteerd` +
            (result.skippedRows > 0
              ? `, ${result.skippedRows} overgeslagen`
              : '')
        );
      } else {
        toastService.warn(
          `Import voltooid met ${result.errorCount} fouten. Bekijk de fouten hieronder.`
        );
      }

      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      refetch();
    } catch (err) {
      console.log(err);
      toastService.error(`Import mislukt: ${(err as Error).message}`);
    }
  };

  const handleDeleteErrors = async () => {
    try {
      await deleteAllErrors();
      toastService.success('Alle importfouten zijn verwijderd');
    } catch (err) {
      console.log(err);
      toastService.error(`Verwijderen mislukt: ${(err as Error).message}`);
    }
  };

  return (
    <>
      {/* Upload Section */}
      <div className="w-full px-4">
        <div className="border border-solid border-color-border-primary rounded-2xl bg-color-surface-primary">
          <div className="flex items-center justify-between px-6 py-4 border-b border-solid border-color-border-primary">
            <div className="flex flex-col gap-1">
              <h4 className="text-color-text-primary">LMA CSV Import</h4>
              <span className="text-color-text-secondary">
                Importeer afvalstromen vanuit een LMA portal export (CSV
                formaat)
              </span>
            </div>
          </div>

          <div className="px-6 py-4">
            <div className="flex flex-col gap-4">
              {/* File Selection */}
              <div className="flex flex-col gap-2">
                <label className="text-subtitle-2 text-color-text-primary">
                  CSV Bestand
                </label>
                <div className="flex items-center gap-3">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".csv"
                    onChange={handleFileSelect}
                    className="hidden"
                  />
                  <Button
                    variant="secondary"
                    label="Bestand kiezen"
                    icon={CarbonDocumentAttachment}
                    onClick={handleBrowseClick}
                    disabled={isImporting}
                  />
                  <span className="text-body-2 text-color-text-secondary">
                    {selectedFile
                      ? selectedFile.name
                      : 'Geen bestand geselecteerd'}
                  </span>
                </div>
                <p className="text-caption text-color-text-tertiary">
                  Exporteer de ontvangstmeldingen vanuit het LMA portaal als CSV
                  en upload het bestand hier.
                </p>
              </div>

              {/* Import Button */}
              <div className="flex items-center gap-3">
                <Button
                  variant="primary"
                  label={isImporting ? 'Importeren...' : 'Importeren'}
                  onClick={handleImport}
                  disabled={!selectedFile || isImporting}
                  loading={isImporting}
                />
              </div>

              {/* Import Result */}
              {importResult && (
                <div
                  className={`p-4 rounded-radius-md border ${
                    importResult.success
                      ? 'bg-color-success-light border-color-success'
                      : 'bg-color-warning-light border-color-warning'
                  }`}
                >
                  <div className="flex flex-col gap-2">
                    <p className="text-subtitle-2 text-color-text-primary">
                      {importResult.message}
                    </p>
                    <div className="flex gap-4 text-body-2 text-color-text-secondary">
                      <span>Totaal: {importResult.totalRows} rijen</span>
                      <span>
                        Geïmporteerd: {importResult.successfulImports}
                      </span>
                      <span>Overgeslagen: {importResult.skippedRows}</span>
                      <span>Fouten: {importResult.errorCount}</span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Errors Section Header */}
      <div className="flex items-center justify-between w-full px-4">
        <div className="flex items-center gap-4">
          <h5 className="text-subtitle-1 text-color-text-primary">
            Importfouten
          </h5>
          {errors.length > 0 && (
            <span className="px-2 py-0.5 bg-color-error-light text-color-error rounded-full text-caption font-medium">
              {errors.length} {errors.length === 1 ? 'fout' : 'fouten'}
            </span>
          )}
        </div>
        {errors.length > 0 && (
          <Button
            variant="destructive"
            label={isDeletingErrors ? 'Verwijderen...' : 'Fouten verwijderen'}
            icon={TrashSimple}
            onClick={handleDeleteErrors}
            disabled={isDeletingErrors}
          />
        )}
      </div>

      {/* Guidance message */}
      {errors.length > 0 && (
        <div className="mx-4 p-4 bg-color-warning-light rounded-radius-md border border-color-warning">
          <p className="text-body-2 text-color-text-primary">
            <strong>Fouten oplossen:</strong> Controleer de foutmeldingen en
            corrigeer de ontbrekende bedrijven via <strong>Relaties</strong> of
            pas de CSV data aan. Na correctie kunt u het bestand opnieuw
            importeren.
          </p>
        </div>
      )}

      {/* Errors Table */}
      <ErrorBoundary fallbackRender={fallbackRender}>
        <ErrorThrowingComponent error={error as Error | null} />
        <LmaImportErrorsTable errors={errors} isLoading={isLoading} />
      </ErrorBoundary>
    </>
  );
};
