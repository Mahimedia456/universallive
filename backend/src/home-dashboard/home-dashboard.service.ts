import { Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class HomeDashboardService {
  constructor(private readonly db: BackendSupabase) {}

  async dashboard(token: string) {
    const user = await this.db.currentUser(token);
    const uid = encodeURIComponent(user.id);

    const [
      profileRows,
      onboardingRows,
      connectionRows,
      credentialRows,
      sceneRows,
      configRows,
      activeRows,
      recentSessionRows,
      notificationRows,
      entitlementRows,
    ] = await Promise.all([
      this.db.adminRest<any[]>(`ul_creator_profiles?user_id=eq.${uid}&select=*`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_onboarding_state?user_id=eq.${uid}&select=*`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_streaming_connections?user_id=eq.${uid}&select=*&order=is_default.desc,created_at.desc`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_stream_credentials?user_id=eq.${uid}&credential_type=eq.rtmp&select=connection_id,last_rotated_at`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_scenes?user_id=eq.${uid}&is_archived=eq.false&select=*&order=is_default.desc,sort_order.asc,created_at.asc`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_stream_configs?user_id=eq.${uid}&select=*&order=is_default.desc,updated_at.desc&limit=1`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_broadcast_sessions?user_id=eq.${uid}&status=in.(created,starting,live,reconnecting)&select=id,title,status,started_at,last_heartbeat_at,created_at&order=created_at.desc&limit=1`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_broadcast_sessions?user_id=eq.${uid}&select=id,title,status,started_at,ended_at,created_at&order=created_at.desc&limit=5`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_notifications?user_id=eq.${uid}&select=id,type,title,body,severity,read_at,created_at&order=created_at.desc&limit=5`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_user_entitlements?user_id=eq.${uid}&select=*`, { method: 'GET' }),
    ]);

    const credentialMap = new Map(
      (credentialRows || []).map((row) => [row.connection_id, row]),
    );

    const connections = (connectionRows || []).map((connection) => {
      const credential = credentialMap.get(connection.id);
      return {
        ...connection,
        credential_configured: !!credential,
        credential_updated_at: credential?.last_rotated_at || null,
        ready_to_publish:
          connection.is_enabled !== false &&
          connection.status === 'connected' &&
          !!credential,
      };
    });

    const defaultConnection =
      connections.find((item) => item.is_default && item.ready_to_publish) ||
      connections.find((item) => item.ready_to_publish) ||
      connections.find((item) => item.is_default) ||
      null;

    const defaultScene =
      (sceneRows || []).find((scene) => scene.is_default) ||
      sceneRows?.[0] ||
      null;

    const onboarding = onboardingRows?.[0] || null;
    const config = configRows?.[0] || {
      resolution: '1080p',
      width: 1920,
      height: 1080,
      fps: 30,
      bitrate_kbps: 6800,
      microphone_enabled: true,
      internal_audio_enabled: true,
      facecam_enabled: false,
      orientation: 'auto',
    };

    const readyDestinationCount = connections.filter(
      (item) => item.ready_to_publish,
    ).length;
    const permissionReady = onboarding?.permission_education_completed === true;
    const destinationReady = readyDestinationCount > 0;

    const entitlement = entitlementRows?.[0] || {
      plan_key: 'free',
      status: 'active',
    };

    return {
      generated_at: new Date().toISOString(),
      user: {
        id: user.id,
        email: user.email,
      },
      profile: profileRows?.[0] || null,
      onboarding,
      membership: {
        plan_key: entitlement.plan_key || 'free',
        status: entitlement.status || 'active',
      },
      readiness: {
        ready_to_stream: destinationReady && permissionReady,
        destination_ready: destinationReady,
        permission_setup_ready: permissionReady,
        ready_destination_count: readyDestinationCount,
        destination_count: connections.length,
      },
      default_destination: defaultConnection,
      default_scene: defaultScene,
      stream_config: config,
      active_broadcast: activeRows?.[0] || null,
      recent_streams: recentSessionRows || [],
      recent_notifications: (notificationRows || []).map((row) => ({
        ...row,
        is_read: !!row.read_at,
      })),
    };
  }
}
