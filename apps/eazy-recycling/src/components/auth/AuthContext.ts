import { createContext } from 'react';
import { AuthContextProps } from '@/components/auth/AuthProvider';

export const AuthContext = createContext<AuthContextProps | undefined>(
  undefined
);
