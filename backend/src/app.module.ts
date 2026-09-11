import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { AccountModule } from './account/account.module';
import { AdminConsoleModule } from './admin-console/admin-console.module';
import { AdminModule } from './admin/admin.module';
import { AppController } from './app.controller';
import { AssetsV2Module } from './assets-v2/assets-v2.module';
import { AuthV2Module } from './auth-v2/auth-v2.module';
import { AuthModule } from './auth/auth.module';
import { BillingModule } from './billing/billing.module';
import { BroadcastSessionsModule } from './broadcast-sessions/broadcast-sessions.module';
import { BackendSupabase } from './common/backend-supabase';
import { validateEnv } from './config/env';
import { CredentialsModule } from './credentials/credentials.module';
import { DestinationsModule } from './destinations/destinations.module';
import { DevicesModule } from './devices/devices.module';
import { DiagnosticsModule } from './diagnostics/diagnostics.module';
import { EntitlementsModule } from './entitlements/entitlements.module';
import { FoundationModule } from './foundation/foundation.module';
import { HealthModule } from './health/health.module';
import { HomeDashboardModule } from './home-dashboard/home-dashboard.module';
import { LegalModule } from './legal/legal.module';
import { NotificationsV2Module } from './notifications-v2/notifications-v2.module';
import { PlatformOauthModule } from './platform-oauth/platform-oauth.module';
import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';
import { ProfilesModule } from './profiles/profiles.module';
import { QaFinalModule } from './qa-final/qa-final.module';
import { PushModule } from './push/push.module';
import { RealtimeFinalModule } from './realtime-final/realtime-final.module';
import { RtmpCredentialsModule } from './rtmp-credentials/rtmp-credentials.module';
import { SceneSourcesModule } from './scene-sources/scene-sources.module';
import { ScenesV2Module } from './scenes-v2/scenes-v2.module';
import { ScenesModule } from './scenes/scenes.module';
import { SecurityAuditModule } from './security-audit/security-audit.module';
import { SettingsModule } from './settings/settings.module';
import { StreamConfigV2Module } from './stream-config-v2/stream-config-v2.module';
import { StreamHistoryV2Module } from './stream-history-v2/stream-history-v2.module';
import { StreamLifecycleModule } from './stream-lifecycle/stream-lifecycle.module';
import { StreamTelemetryModule } from './stream-telemetry/stream-telemetry.module';
import { StreamingConnectionsModule } from './streaming-connections/streaming-connections.module';
import { StreamsModule } from './streams/streams.module';
import { StudioWorkspaceModule } from './studio-workspace/studio-workspace.module';
import { SupabaseModule } from './supabase/supabase.module';
import { SupportV2Module } from './support-v2/support-v2.module';
import { SystemStateModule } from './system-state/system-state.module';

@Module({
  controllers: [AppController],
  providers: [BackendSupabase],
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      validate: validateEnv,
    }),
    SupabaseModule,
    PushModule,
    HealthModule,
    FoundationModule,
    HomeDashboardModule,
    LegalModule,
    AuthModule,
    AuthV2Module,
    ProfilesModule,
    ProfilesV2Module,
    QaFinalModule,
    DevicesModule,
    DiagnosticsModule,
    StreamsModule,
    DestinationsModule,
    StreamingConnectionsModule,
    PlatformOauthModule,
    RtmpCredentialsModule,
    SettingsModule,
    ScenesModule,
    ScenesV2Module,
    SceneSourcesModule,
    AssetsV2Module,
    StreamConfigV2Module,
    BroadcastSessionsModule,
    StreamTelemetryModule,
    StreamHistoryV2Module,
    StreamLifecycleModule,
    StudioWorkspaceModule,
    CredentialsModule,
    AccountModule,
    EntitlementsModule,
    BillingModule,
    NotificationsV2Module,
    SupportV2Module,
    SystemStateModule,
    SecurityAuditModule,
    RealtimeFinalModule,
    AdminModule,
    AdminConsoleModule,
  ],
})
export class AppModule {}
