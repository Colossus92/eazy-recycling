import { ProductControllerApi, ProductRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const productApi = new ProductControllerApi(apiInstance.config);

export const productService = {
  getAll: () => productApi.getAllProducts().then((r) => r.data),
  create: (product: ProductRequest) =>
    productApi.createProduct(product).then((r) => r.data),
  update: (id: number, product: ProductRequest) =>
    productApi.updateProduct(id, product).then((r) => r.data),
  delete: (id: number) => productApi.deleteProduct(id),
};
