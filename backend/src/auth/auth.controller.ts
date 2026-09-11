import { Controller, Get, UseGuards } from '@nestjs/common';
import { AuthUser } from './auth-user.decorator';
import type { UniversalLiveAuthUser } from './auth.types';
import { UniversalLiveAuthGuard } from './universallive-auth.guard';

@Controller('auth')
export class AuthController {
  @Get('me')
  @UseGuards(UniversalLiveAuthGuard)
  me(@AuthUser() user: UniversalLiveAuthUser) {
    return {
      user: {
        id: user.id,
        email: user.email,
        phone: user.phone,
        createdAt: user.created_at,
        emailVerifiedAt: user.email_confirmed_at,
      },
    };
  }
}
