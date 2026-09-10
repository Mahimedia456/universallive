import type { PropsWithChildren } from 'react';
import { useEffect } from 'react';
import { loadCloudProfile } from '@/services/cloud-profile';
import { useSessionStore } from '@/store/session.store';

export function AuthBootstrap({ children }: PropsWithChildren) {
  useEffect(() => {
    let mounted = true;

    const initialize = async () => {
      await useSessionStore.getState().hydrateSession();
      if (!mounted) return;
      if (useSessionStore.getState().isAuthenticated) {
        try { await loadCloudProfile(); } catch { /* backend may still be starting during local development */ }
      }
    };

    void initialize();
    return () => { mounted = false; };
  }, []);

  return children;
}
