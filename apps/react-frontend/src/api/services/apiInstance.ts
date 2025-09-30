import { Configuration } from '@/api/client';
import axios from 'axios';
import { supabase } from '@/api/supabaseClient';

const axiosInstance = axios.create({
    timeout: 10000,
});

axiosInstance.interceptors.response.use(
    response => response,
    error => Promise.reject(error)
);

export const apiInstance = {
    axios: axiosInstance,
    config: new Configuration({
        basePath: import.meta.env.VITE_API_URL,
        accessToken: async () => {
            try {
                const { data } = await supabase.auth.getSession();
                return data.session?.access_token || '';
            } catch (error) {
                console.error('Error getting access token:', error);
                return '';
            }
        },
    }),
};