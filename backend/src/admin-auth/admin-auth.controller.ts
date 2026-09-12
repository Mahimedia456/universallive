import {Body,Controller,Get,Post,Req,UseGuards} from '@nestjs/common';
import {AdminAuthService} from './admin-auth.service';
import {AdminJwtGuard} from './admin-jwt.guard';
@Controller('admin/auth') export class AdminAuthController{constructor(private readonly auth:AdminAuthService){}
@Post('login') login(@Body() b:{email:string;password:string}){return this.auth.login(b.email,b.password)}
@Post('refresh') refresh(@Body() b:{refreshToken:string}){return this.auth.refresh(b.refreshToken)}
@Post('forgot-password') forgot(@Body() b:{email:string}){return this.auth.forgot(b.email)}
@Post('verify-reset-otp') verify(@Body() b:{email:string;code:string}){return this.auth.verifyResetOtp(b.email,b.code)}
@Post('reset-password') reset(@Body() b:{resetToken:string;password:string}){return this.auth.resetPassword(b.resetToken,b.password)}
@Get('me') @UseGuards(AdminJwtGuard) me(@Req() req:any){return this.auth.publicAdmin(req.admin)}
@Post('logout') @UseGuards(AdminJwtGuard) logout(@Req() req:any,@Body() b:{refreshToken:string}){return this.auth.logout(req.admin.sub,b.refreshToken)} }
