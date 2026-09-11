import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';

import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class UniversalLiveAuthGuard implements CanActivate {
  constructor(private readonly backend: BackendSupabase) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<{
      headers: Record<string, string | undefined>;
      user?: unknown;
      accessToken?: string;
      userId?: string;
    }>();
    const header = request.headers.authorization;
    if (!header?.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing bearer token');
    }

    const token = header.slice(7).trim();
    const user = await this.backend.currentUser(token);
    request.user = user;
    request.accessToken = token;
    request.userId = user.id;
    return true;
  }
}
