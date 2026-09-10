import {
  Body,
  Controller,
  Get,
  Headers,
  Post,
} from '@nestjs/common';

import { AuthV2Service } from './auth-v2.service';

@Controller('auth/mobile')
export class AuthV2Controller {
  constructor(private readonly auth: AuthV2Service) {}

  @Get('status')
  status() {
    return {
      success: true,
      data: {
        provider: 'supabase-auth',
        emailPassword: true,
        emailVerification: true,
        passwordRecovery: true,
      },
    };
  }

  @Post('register')
  register(
    @Body()
    body: {
      email: string;
      password: string;
      fullName?: string;
      username?: string;
    },
  ) {
    return this.auth.signUp(body);
  }

  @Post('login')
  login(@Body() body: { email: string; password: string }) {
    return this.auth.signIn(body);
  }

  @Post('refresh')
  refresh(@Body() body: { refreshToken: string }) {
    return this.auth.refresh(body.refreshToken);
  }

  @Post('verify-email')
  verifyEmail(@Body() body: { email: string; token: string }) {
    return this.auth.verifyEmail(body);
  }

  @Post('resend-verification')
  resend(@Body() body: { email: string }) {
    return this.auth.resendSignupOtp(body.email);
  }

  @Post('forgot-password')
  forgot(@Body() body: { email: string }) {
    return this.auth.requestPasswordReset(body.email);
  }

  @Post('verify-recovery')
  verifyRecovery(@Body() body: { email: string; token: string }) {
    return this.auth.verifyRecovery(body);
  }

  @Post('update-password')
  updatePassword(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: { password: string },
  ) {
    const token = (authorization || '').replace(/^Bearer\s+/i, '').trim();
    return this.auth.updatePassword(token, body.password);
  }

  @Post('logout')
  logout(@Headers('authorization') authorization?: string) {
    const token = (authorization || '').replace(/^Bearer\s+/i, '');
    return this.auth.signOut(token);
  }
}
