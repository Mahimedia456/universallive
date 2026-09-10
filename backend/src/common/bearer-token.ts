import { UnauthorizedException } from '@nestjs/common';

export function readBearerToken(authorization?: string): string {
  const token = (authorization || '').replace(/^Bearer\s+/i, '').trim();
  if (!token) throw new UnauthorizedException('Bearer token is required');
  return token;
}
