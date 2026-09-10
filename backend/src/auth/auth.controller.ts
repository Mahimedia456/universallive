import { Controller, Get, UseGuards } from '@nestjs/common';
import type { User } from '@supabase/supabase-js';
import { AuthUser } from './auth-user.decorator';
import { SupabaseAuthGuard } from './supabase-auth.guard';

@Controller('auth')
export class AuthController {
  @Get('me')
  @UseGuards(SupabaseAuthGuard)
  me(@AuthUser() user: User) {
    return { user: { id: user.id, email: user.email, phone: user.phone, createdAt: user.created_at } };
  }
}
