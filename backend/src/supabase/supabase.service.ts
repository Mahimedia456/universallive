import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

@Injectable()
export class SupabaseService {
  readonly admin: SupabaseClient;
  readonly publicClient: SupabaseClient;

  constructor(config: ConfigService) {
    const url = config.getOrThrow<string>('SUPABASE_URL');
    const publishable = config.getOrThrow<string>('SUPABASE_PUBLISHABLE_KEY');
    const secret = config.getOrThrow<string>('SUPABASE_SECRET_KEY');

    this.admin = createClient(url, secret, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    this.publicClient = createClient(url, publishable, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
  }
}
