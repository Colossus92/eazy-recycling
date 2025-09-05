import axios from 'axios';
import { supabase } from './supabaseClient';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add a request interceptor to include the JWT token in every request
http.interceptors.request.use(async (config) => {
  try {
    // Get the current session from Supabase
    const { data } = await supabase.auth.getSession();
    const token = data.session?.access_token;

    // If we have a token, add it to the Authorization header
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  } catch (error) {
    console.error('Error adding auth token to request:', error);
    return config;
  }
});
