import { LmaImportControllerApi } from '../client';
import { apiInstance } from './apiInstance';

const lmaImportApi = new LmaImportControllerApi(apiInstance.config);

export const lmaImportService = {
  importCsv: (file: File) => lmaImportApi.importCsv(file),
  getErrors: () => lmaImportApi.getErrors(),
  deleteAllErrors: () => lmaImportApi.deleteAllErrors(),
};
