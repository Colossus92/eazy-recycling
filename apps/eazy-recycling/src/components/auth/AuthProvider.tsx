import { ReactNode, useEffect, useState } from 'react';
import { Session } from '@supabase/supabase-js';
import { lazy } from 'react';
import { ClipLoader } from 'react-spinners';
import { supabase } from '@/api/supabaseClient.tsx';
import { AuthContext } from '@/components/auth/AuthContext.ts';
import { User } from '@/types/api.ts';

const LoginPage = lazy(() => import('../../app/desktop/pages/LoginPage.tsx'));

export interface AuthContextProps {
  session: Session | null;
  signOut: () => Promise<void>;
  userRoles: string[];
  user: User | null;
  hasRole: (role: string) => boolean;
  userId: string | null;
}

interface JWTClaims {
  user_metadata: {
    first_name: string;
    last_name: string;
  };
  email: string;
  user_roles: string[];
  sub: string;
}

function createUser(decodedClaims: JWTClaims): User {
  return {
    id: '',
    firstName: decodedClaims.user_metadata.first_name,
    lastName: decodedClaims.user_metadata.last_name,
    email: decodedClaims.email,
    roles: decodedClaims.user_roles,
    lastSignInAt: new Date().toISOString(),
  };
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [session, setSession] = useState<Session | null>(null);
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [user, setUser] = useState<User | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      if (session?.access_token) {
        const claims = session.access_token.split('.')[1];
        try {
          const decodedClaims = JSON.parse(atob(claims)) as JWTClaims;
          setUser(createUser(decodedClaims));
          setUserRoles(decodedClaims.user_roles || []);
        } catch (error) {
          console.error('Failed to decode JWT claims:', error);
          setUserRoles([]);
        }
      }
      setLoading(false);
    });

    supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
      if (session?.access_token) {
        const claims = session.access_token.split('.')[1];
        try {
          const decodedClaims = JSON.parse(atob(claims)) as JWTClaims;
          setUser(createUser(decodedClaims));
          setUserRoles(decodedClaims.user_roles || []);
          setUserId(decodedClaims.sub || null);
        } catch (error) {
          console.error('Failed to decode JWT claims:', error);
          setUserRoles([]);
          setUserId(null);
        }
      } else {
        setUserRoles([]);
        setUserId(null);
      }
    });
  }, []);

  const signOut = async () => {
    await supabase.auth.signOut();
    setSession(null);
    setUserRoles([]);
    setUserId(null);
  };

  const hasRole = (role: string): boolean => {
    return userRoles.includes(role);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen w-screen">
        <ClipLoader size={40} color="#4A6CF7" aria-label="Loading..." />
      </div>
    );
  }

  if (!session) {
    return (
      <AuthContext.Provider
        value={{ session, signOut, userRoles, user, hasRole, userId }}
      >
        <LoginPage />
      </AuthContext.Provider>
    );
  }

  return (
    <AuthContext.Provider
      value={{ session, signOut, userRoles, user, hasRole, userId }}
    >
      {children}
    </AuthContext.Provider>
  );
};
