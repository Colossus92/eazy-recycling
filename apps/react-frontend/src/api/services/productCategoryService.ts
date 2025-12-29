import {
  ProductCategoryControllerApi,
  ProductCategoryRequest,
} from '@/api/client';
import { apiInstance } from './apiInstance';

const productCategoryApi = new ProductCategoryControllerApi(apiInstance.config);

export const productCategoryService = {
  getAll: () => productCategoryApi.getAllCategories().then((r) => r.data),
  create: (category: ProductCategoryRequest) =>
    productCategoryApi.createCategory(category).then((r) => r.data),
  update: (id: string, category: ProductCategoryRequest) =>
    productCategoryApi.updateCategory(id, category).then((r) => r.data),
  delete: (id: string) => productCategoryApi.deleteCategory(id),
};
