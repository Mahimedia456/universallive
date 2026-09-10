import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class AdminGuard implements CanActivate {
  constructor(private readonly config: ConfigService) {}
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<{ headers: Record<string, string | string[] | undefined> }>();
    const expected = this.config.get<string>('ADMIN_API_KEY');
    const provided = request.headers['x-admin-key'];
    const value = Array.isArray(provided) ? provided[0] : provided;
    if (!expected || !value || value !== expected) throw new UnauthorizedException('Invalid admin key');
    return true;
  }
}
