import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';

import { BackendSupabase } from '../common/backend-supabase';

type AdminContext = {
  user: any;
  admin: any;
};

@Injectable()
export class AdminConsoleService {
  constructor(private readonly supabase: BackendSupabase) {}

  private async context(accessToken: string): Promise<AdminContext> {
    if (!accessToken) {
      throw new UnauthorizedException('Bearer token required');
    }

    const user = await this.supabase.currentUser(accessToken);

    const rows = await this.supabase.adminRest<any[]>(
      `ul_admin_users?user_id=eq.${encodeURIComponent(user.id)}&is_active=eq.true&select=id,user_id,role,is_active,display_name`,
      { method: 'GET' },
    );

    const admin = rows?.[0];

    if (!admin) {
      throw new ForbiddenException('This account does not have admin access');
    }

    return { user, admin };
  }

  private requireRole(ctx: AdminContext, allowed: string[]) {
    if (!allowed.includes(ctx.admin.role)) {
      throw new ForbiddenException('Your admin role cannot perform this action');
    }
  }

  private async audit(
    ctx: AdminContext,
    action: string,
    targetType: string,
    targetId?: string,
    details: Record<string, unknown> = {},
  ) {
    await this.supabase.adminRest(
      'ul_admin_actions',
      {
        method: 'POST',
        body: JSON.stringify({
          admin_user_id: ctx.user.id,
          action,
          target_type: targetType,
          target_id: targetId || null,
          details,
        }),
      },
    );
  }

  async me(accessToken: string) {
    const { user, admin } = await this.context(accessToken);

    return {
      id: admin.id,
      userId: user.id,
      email: user.email,
      displayName:
        admin.display_name ||
        user.user_metadata?.full_name ||
        user.email,
      role: admin.role,
      isActive: admin.is_active,
    };
  }

  async overview(accessToken: string) {
    await this.context(accessToken);

    const rows = await this.supabase.adminRest<any[]>(
      'rpc/ul_admin_dashboard_overview',
      {
        method: 'POST',
        body: JSON.stringify({}),
      },
    );

    return Array.isArray(rows) ? rows[0] || {} : rows || {};
  }

  async recentUsers(accessToken: string) {
    await this.context(accessToken);

    return await this.supabase.adminRest<any[]>(
      'ul_creator_profiles?select=user_id,display_name,username,creator_type,onboarding_completed,created_at,updated_at&order=created_at.desc&limit=10',
      { method: 'GET' },
    );
  }

  async recentBroadcasts(accessToken: string) {
    await this.context(accessToken);

    return await this.supabase.adminRest<any[]>(
      'ul_broadcast_sessions?select=id,user_id,title,status,started_at,ended_at,created_at&order=created_at.desc&limit=10',
      { method: 'GET' },
    );
  }

  async openSupport(accessToken: string) {
    await this.context(accessToken);

    return await this.supabase.adminRest<any[]>(
      'ul_support_tickets?select=id,user_id,category,subject,status,priority,created_at&status=not.in.(closed,resolved)&order=created_at.desc&limit=10',
      { method: 'GET' },
    );
  }

  async creators(accessToken: string) {
    await this.context(accessToken);

    const profiles = await this.supabase.adminRest<any[]>(
      'ul_creator_profiles?select=user_id,display_name,username,creator_type,onboarding_completed,created_at,updated_at&order=created_at.desc',
      { method: 'GET' },
    );

    const entitlements = await this.supabase.adminRest<any[]>(
      'ul_user_entitlements?select=user_id,plan_key,status,source,starts_at,expires_at,updated_at',
      { method: 'GET' },
    );

    const map = new Map(entitlements.map((item) => [item.user_id, item]));

    return profiles.map((profile) => ({
      ...profile,
      entitlement: map.get(profile.user_id) || {
        plan_key: 'free',
        status: 'active',
      },
    }));
  }

  async updateMembership(
    accessToken: string,
    userId: string,
    body: {
      planKey: 'free' | 'creator' | 'pro';
      status?: string;
    },
  ) {
    const ctx = await this.context(accessToken);
    this.requireRole(ctx, ['owner', 'admin']);

    if (!['free', 'creator', 'pro'].includes(body.planKey)) {
      throw new BadRequestException('Invalid plan key');
    }

    const result = await this.supabase.adminRest<any[]>(
      'ul_user_entitlements?on_conflict=user_id',
      {
        method: 'POST',
        headers: {
          Prefer: 'resolution=merge-duplicates,return=representation',
        },
        body: JSON.stringify({
          user_id: userId,
          plan_key: body.planKey,
          status: body.status || 'active',
          source: 'admin-console',
          starts_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );

    await this.audit(ctx, 'membership.update', 'user', userId, body);

    return result?.[0] || result;
  }

  async connections(accessToken: string) {
    await this.context(accessToken);

    return await this.supabase.adminRest<any[]>(
      'ul_streaming_connections?select=id,user_id,platform,display_name,status,is_default,is_enabled,last_tested_at,last_error_message,created_at,updated_at&order=created_at.desc',
      { method: 'GET' },
    );
  }

  async updateConnection(
    accessToken: string,
    id: string,
    body: { isEnabled?: boolean; status?: string },
  ) {
    const ctx = await this.context(accessToken);
    this.requireRole(ctx, ['owner', 'admin']);

    const patch: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    };

    if (typeof body.isEnabled === 'boolean') {
      patch.is_enabled = body.isEnabled;
    }

    if (body.status) {
      patch.status = body.status;
    }

    const result = await this.supabase.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify(patch),
      },
    );

    await this.audit(ctx, 'connection.update', 'connection', id, body);

    return result?.[0] || result;
  }

  async broadcasts(accessToken: string) {
    await this.context(accessToken);

    return await this.supabase.adminRest<any[]>(
      'ul_broadcast_sessions?select=id,user_id,title,status,started_at,ended_at,last_heartbeat_at,created_at&order=created_at.desc',
      { method: 'GET' },
    );
  }

  async stopBroadcast(accessToken: string, id: string) {
    const ctx = await this.context(accessToken);
    this.requireRole(ctx, ['owner', 'admin']);

    const existing = await this.supabase.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&select=id,status`,
      { method: 'GET' },
    );

    if (!existing?.length) {
      throw new NotFoundException('Broadcast not found');
    }

    const result = await this.supabase.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'ended_by_admin',
          ended_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );

    await this.audit(ctx, 'broadcast.stop', 'broadcast', id);

    return result?.[0] || result;
  }

  async supportTickets(accessToken: string) {
    await this.context(accessToken);

    return await this.supabase.adminRest<any[]>(
      'ul_support_tickets?select=id,user_id,category,subject,description,status,priority,created_at,updated_at&order=created_at.desc',
      { method: 'GET' },
    );
  }

  async updateSupport(
    accessToken: string,
    id: string,
    body: { status?: string; priority?: string },
  ) {
    const ctx = await this.context(accessToken);
    this.requireRole(ctx, ['owner', 'admin', 'support']);

    const patch: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    };

    if (body.status) patch.status = body.status;
    if (body.priority) patch.priority = body.priority;

    const result = await this.supabase.adminRest<any[]>(
      `ul_support_tickets?id=eq.${encodeURIComponent(id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify(patch),
      },
    );

    await this.audit(ctx, 'support.update', 'support_ticket', id, body);

    return result?.[0] || result;
  }
}
