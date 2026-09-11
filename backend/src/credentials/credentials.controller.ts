import { Body, Controller, Delete, Get, NotFoundException, Param, Put, UseGuards } from '@nestjs/common';
import type { User } from '@supabase/supabase-js';
import { AuthUser } from '../auth/auth-user.decorator';
import { UniversalLiveAuthGuard } from '../auth/universallive-auth.guard';
import { SupabaseService } from '../supabase/supabase.service';
import { CredentialVaultService } from './credential-vault.service';

@Controller('destinations/:destinationId/credential')
@UseGuards(UniversalLiveAuthGuard)
export class CredentialsController {
  constructor(private readonly supabase: SupabaseService, private readonly vault: CredentialVaultService) {}

  private async assertOwned(userId: string, destinationId: string) {
    const { data } = await this.supabase.admin.from('stream_destinations').select('id').eq('id', destinationId).eq('user_id', userId).maybeSingle();
    if (!data) throw new NotFoundException('Destination not found');
  }

  @Get()
  async status(@AuthUser() user: User, @Param('destinationId') destinationId: string) {
    await this.assertOwned(user.id, destinationId);
    const { data, error } = await this.supabase.admin.from('stream_destination_secrets').select('updated_at').eq('destination_id', destinationId).maybeSingle();
    if (error) throw error;
    return { configured: !!data, updatedAt: data?.updated_at ?? null };
  }

  @Put()
  async save(@AuthUser() user: User, @Param('destinationId') destinationId: string, @Body() body: { secret?: string }) {
    await this.assertOwned(user.id, destinationId);
    const encrypted = this.vault.encrypt(body.secret ?? '');
    const row = {
      destination_id: destinationId,
      user_id: user.id,
      ciphertext: encrypted.ciphertext,
      iv: encrypted.iv,
      auth_tag: encrypted.authTag,
      key_version: encrypted.keyVersion,
      updated_at: new Date().toISOString(),
    };
    const { error } = await this.supabase.admin.from('stream_destination_secrets').upsert(row, { onConflict: 'destination_id' });
    if (error) throw error;
    await this.supabase.admin.from('stream_destinations').update({ secret_ref: `vault:${destinationId}`, updated_at: new Date().toISOString() }).eq('id', destinationId).eq('user_id', user.id);
    return { ok: true, configured: true };
  }

  @Delete()
  async remove(@AuthUser() user: User, @Param('destinationId') destinationId: string) {
    await this.assertOwned(user.id, destinationId);
    const { error } = await this.supabase.admin.from('stream_destination_secrets').delete().eq('destination_id', destinationId).eq('user_id', user.id);
    if (error) throw error;
    await this.supabase.admin.from('stream_destinations').update({ secret_ref: null, updated_at: new Date().toISOString() }).eq('id', destinationId).eq('user_id', user.id);
    return { ok: true, configured: false };
  }
}
