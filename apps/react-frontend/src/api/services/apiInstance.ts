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
            console.log('accessToken function called at:', new Date().toISOString());
            try {
                const { data } = await supabase.auth.getSession();
                if (data.session?.access_token) {   
                    console.log('Access token retrieved successfully');
                    // Only log a small part of the token for security
                    const tokenPreview = data.session.access_token.substring(0, 10) + '...';
                    console.log('Token preview:', tokenPreview);
                } else {
                    console.warn('No access token found in session');
                }
                return data.session?.access_token || '';
            } catch (error) {
                console.error('Error getting access token:', error);
                return '';
            }
        },
    }),
};