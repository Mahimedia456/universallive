import { Body, Controller, Get, Headers, Put } from '@nestjs/common';
import { readBearerToken } from '../common/bearer-token';
import { ProfilesV2Service } from './profiles-v2.service';

@Controller('profiles/me')
export class ProfilesV2Controller {
  constructor(private readonly profiles: ProfilesV2Service) {}

  @Get()
  get(@Headers('authorization') authorization?: string) {
    return this.profiles.getProfile(readBearerToken(authorization));
  }

  @Put()
  update(
    @Headers('authorization') authorization: string | undefined,
    @Body()
    body: {
      displayName?: string;
      username?: string;
      avatarUrl?: string | null;
      creatorType?: string | null;
      onboardingCompleted?: boolean;
      locale?: string | null;
      timezone?: string | null;
    },
  ) {
    return this.profiles.upsertProfile(readBearerToken(authorization), body);
  }
}
