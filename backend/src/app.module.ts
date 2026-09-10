import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AuthModule } from './auth/auth.module';
import { HealthModule } from './health/health.module';
import { ProfilesModule } from './profiles/profiles.module';
import { StreamsModule } from './streams/streams.module';
import { SupabaseModule } from './supabase/supabase.module';
import { DestinationsModule } from './destinations/destinations.module';
import { SettingsModule } from './settings/settings.module';
import { ScenesModule } from './scenes/scenes.module';
import { CredentialsModule } from './credentials/credentials.module';
import { AccountModule } from './account/account.module';
import { AdminModule } from './admin/admin.module';

import { FoundationModule } from './foundation/foundation.module';

import { AuthV2Module } from './auth-v2/auth-v2.module';

import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';

import { DevicesModule } from './devices/devices.module';

import { StreamingConnectionsModule } from './streaming-connections/streaming-connections.module';

import { PlatformOauthModule } from './platform-oauth/platform-oauth.module';

import { RtmpCredentialsModule } from './rtmp-credentials/rtmp-credentials.module';

import { ScenesV2Module } from './scenes-v2/scenes-v2.module';

import { SceneSourcesModule } from './scene-sources/scene-sources.module';

import { AssetsV2Module } from './assets-v2/assets-v2.module';

import { StreamConfigV2Module } from './stream-config-v2/stream-config-v2.module';

import { BroadcastSessionsModule } from './broadcast-sessions/broadcast-sessions.module';

import { StreamTelemetryModule } from './stream-telemetry/stream-telemetry.module';

import { StreamHistoryV2Module } from './stream-history-v2/stream-history-v2.module';

import { EntitlementsModule } from './entitlements/entitlements.module';

import { BillingModule } from './billing/billing.module';

import { NotificationsV2Module } from './notifications-v2/notifications-v2.module';

import { SupportV2Module } from './support-v2/support-v2.module';

import { SecurityAuditModule } from './security-audit/security-audit.module';

import { RealtimeFinalModule } from './realtime-final/realtime-final.module';

@Module({
  imports: [
    RealtimeFinalModule,
    SecurityAuditModule,
    SupportV2Module,
    NotificationsV2Module,
    BillingModule,
    EntitlementsModule,
    StreamHistoryV2Module,
    StreamTelemetryModule,
    BroadcastSessionsModule,
    StreamConfigV2Module,
    AssetsV2Module,
    SceneSourcesModule,
    ScenesV2Module,
    RtmpCredentialsModule,
    PlatformOauthModule,
    StreamingConnectionsModule,
    DevicesModule,
    ProfilesV2Module,
    AuthV2Module,
    FoundationModule,
    ConfigModule.forRoot({ isGlobal: true }),
    SupabaseModule,
    HealthModule,
    AuthModule,
    ProfilesModule,
    StreamsModule,
    DestinationsModule,
    SettingsModule,
    ScenesModule,
    CredentialsModule,
    AccountModule,
    AdminModule,
  ],
})
export class AppModule {}






