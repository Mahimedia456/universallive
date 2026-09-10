import { Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class RealtimeFinalService {
  constructor(private readonly db: BackendSupabase) {}

  async publishUserEvent(token: string, input: {
    topic: string;
    eventType: string;
    sessionId?: string | null;
    payload?: Record<string, unknown>;
  }) {
    const user = await this.db.currentUser(token);

    const rows = await this.db.adminRest<any[]>(
      'ul_realtime_events',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          session_id: input.sessionId || null,
          topic: input.topic,
          event_type: input.eventType,
          payload: input.payload || {},
        }),
      },
    );

    return rows?.[0] || null;
  }

  finalStatus() {
    return {
      backendPhase: 20,
      status: 'ready-for-mobile-integration',
      apiVersion: 'v1',
      controlPlane: 'nestjs-supabase',
      mediaPlane: 'device-to-platform-rtmp',
      realtime: 'supabase-realtime',
    };
  }
}
