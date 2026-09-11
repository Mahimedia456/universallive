import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import type { UniversalLiveAuthUser } from './auth.types';

export const AuthUser = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): UniversalLiveAuthUser => {
    return ctx.switchToHttp().getRequest<{ user: UniversalLiveAuthUser }>().user;
  },
);
