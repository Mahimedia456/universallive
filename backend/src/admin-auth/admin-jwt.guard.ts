import {CanActivate,ExecutionContext,Injectable,UnauthorizedException} from '@nestjs/common';
import {AdminAuthService} from './admin-auth.service';
@Injectable() export class AdminJwtGuard implements CanActivate{constructor(private readonly auth:AdminAuthService){} async canActivate(ctx:ExecutionContext){const req=ctx.switchToHttp().getRequest<any>();const raw=String(req.headers.authorization||'').replace(/^Bearer\s+/i,'').trim();if(!raw)throw new UnauthorizedException('Bearer token required');req.admin=await this.auth.verifyAccess(raw);return true}}
