import { http } from '@/api/http.ts';
import { ProcessingMethod } from '@/types/api.ts';

export const processingMethodService = {
  list: () =>
    http.get<ProcessingMethod[]>('/processing-methods').then((r) => r.data),
};
