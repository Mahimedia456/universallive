import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards, Delete } from '@nestjs/common';
import { AdminJwtGuard } from '../admin-auth/admin-jwt.guard';
import { AdminConsoleService } from './admin-console.service';
@Controller('admin')
@UseGuards(AdminJwtGuard)
export class AdminConsoleController {
 constructor(private readonly service:AdminConsoleService){}
 @Get('dashboard') dashboard(){return this.service.dashboard()}
 @Get('users') users(@Query() q:any){return this.service.users(q)} @Get('users/:id') user(@Param('id') id:string){return this.service.user(id)} @Post('users') createUser(@Req() r:any,@Body() b:any){return this.service.createUser(r.admin,b)} @Patch('users/:id') updateUser(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateUser(r.admin,id,b)} @Post('users/:id/status') userStatus(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.setStatus(r.admin,id,!!b.isActive)} @Post('users/:id/verify-email') verify(@Req() r:any,@Param('id') id:string){return this.service.verifyEmail(r.admin,id)} @Delete('users/:id') remove(@Req() r:any,@Param('id') id:string,@Query('mode') mode?:string){return this.service.removeUser(r.admin,id,mode==='hard')}
 @Get('creators') creators(@Query() q:any){return this.service.creators(q)} @Get('creators/:id') creator(@Param('id') id:string){return this.service.creator(id)} @Post('creators') createCreator(@Req() r:any,@Body() b:any){return this.service.createCreator(r.admin,b)} @Patch('creators/:id') updateCreator(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateCreator(r.admin,id,b)}
 @Get('streams') streams(@Query() q:any){return this.service.streams(q)} @Get('streams/:id') stream(@Param('id') id:string){return this.service.stream(id)} @Post('streams') createStream(@Req() r:any,@Body() b:any){return this.service.createStream(r.admin,b)} @Patch('streams/:id') updateStream(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateStream(r.admin,id,b)} @Post('streams/:id/end') endStream(@Req() r:any,@Param('id') id:string){return this.service.endStream(r.admin,id)}
 @Get('connections') connections(@Query() q:any){return this.service.connections(q)} @Get('connections/:id') connection(@Param('id') id:string){return this.service.connection(id)} @Post('connections') createConnection(@Req() r:any,@Body() b:any){return this.service.createConnection(r.admin,b)} @Patch('connections/:id') updateConnection(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateConnection(r.admin,id,b)} @Post('connections/:id/enabled') connectionEnabled(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.setConnectionEnabled(r.admin,id,!!b.isEnabled)}

 @Get('diagnostics') diagnostics(@Query() q:any){return this.service.diagnostics(q)}
 @Get('diagnostics/streams/:id') diagnosticStream(@Param('id') id:string){return this.service.diagnosticStream(id)}
 @Get('analytics') analytics(@Query() q:any){return this.service.analytics(q)}
 @Get('plans') plans(){return this.service.plans()}
 @Get('plans/:key') plan(@Param('key') key:string){return this.service.plan(key)}
 @Post('plans') createPlan(@Req() r:any,@Body() b:any){return this.service.createPlan(r.admin,b)}
 @Patch('plans/:key') updatePlan(@Req() r:any,@Param('key') key:string,@Body() b:any){return this.service.updatePlan(r.admin,key,b)}
 @Post('plans/:key/active') planActive(@Req() r:any,@Param('key') key:string,@Body() b:any){return this.service.setPlanActive(r.admin,key,!!b.isActive)}
 @Get('entitlements') entitlements(@Query() q:any){return this.service.entitlements(q)}
 @Get('entitlements/:userId') entitlement(@Param('userId') userId:string){return this.service.entitlement(userId)}
 @Patch('entitlements/:userId') updateEntitlement(@Req() r:any,@Param('userId') userId:string,@Body() b:any){return this.service.updateEntitlement(r.admin,userId,b)}
 @Get('billing') billing(@Query() q:any){return this.service.billing(q)}
 @Get('billing/:id') purchase(@Param('id') id:string){return this.service.purchase(id)}
 @Patch('billing/:id') updatePurchase(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updatePurchase(r.admin,id,b)}
 @Get('notifications') notifications(@Query() q:any){return this.service.notifications(q)}
 @Get('notifications/:id') notification(@Param('id') id:string){return this.service.notification(id)}
 @Post('notifications') sendNotification(@Req() r:any,@Body() b:any){return this.service.sendNotification(r.admin,b)}
 @Delete('notifications/:id') deleteNotification(@Req() r:any,@Param('id') id:string){return this.service.deleteNotification(r.admin,id)}
 @Get('support') supportTickets(@Query() q:any){return this.service.supportTickets(q)}
 @Get('support/:id') supportTicket(@Param('id') id:string){return this.service.supportTicket(id)}
 @Post('support') createSupportTicket(@Req() r:any,@Body() b:any){return this.service.createSupportTicket(r.admin,b)}
 @Patch('support/:id') updateSupportTicket(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateSupportTicket(r.admin,id,b)}
 @Post('support/:id/reply') replySupportTicket(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.replySupportTicket(r.admin,id,b)}
 @Get('moderation') moderation(@Query() q:any){return this.service.moderation(q)}
 @Get('moderation/:id') moderationCase(@Param('id') id:string){return this.service.moderationCase(id)}
 @Post('moderation') createModerationCase(@Req() r:any,@Body() b:any){return this.service.createModerationCase(r.admin,b)}
 @Patch('moderation/:id') updateModerationCase(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateModerationCase(r.admin,id,b)}
 @Get('system-health') systemHealth(){return this.service.systemHealth()}
 @Get('system-flags') systemFlags(){return this.service.systemFlags()}
 @Get('system-flags/:key') systemFlag(@Param('key') key:string){return this.service.systemFlag(key)}
 @Post('system-flags') createSystemFlag(@Req() r:any,@Body() b:any){return this.service.createSystemFlag(r.admin,b)}
 @Patch('system-flags/:key') updateSystemFlag(@Req() r:any,@Param('key') key:string,@Body() b:any){return this.service.updateSystemFlag(r.admin,key,b)}
 @Delete('system-flags/:key') deleteSystemFlag(@Req() r:any,@Param('key') key:string){return this.service.deleteSystemFlag(r.admin,key)}
 @Get('admin-users') adminUsers(@Query() q:any){return this.service.adminUsers(q)}
 @Get('admin-users/:id') adminUser(@Param('id') id:string){return this.service.adminUser(id)}
 @Post('admin-users') createAdminUser(@Req() r:any,@Body() b:any){return this.service.createAdminUser(r.admin,b)}
 @Patch('admin-users/:id') updateAdminUser(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.updateAdminUser(r.admin,id,b)}
 @Post('admin-users/:id/active') setAdminActive(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.setAdminActive(r.admin,id,!!b.isActive)}
 @Post('admin-users/:id/password') resetAdminPassword(@Req() r:any,@Param('id') id:string,@Body() b:any){return this.service.resetAdminPassword(r.admin,id,String(b.password||''))}
 @Get('audit') auditLogs(@Query() q:any){return this.service.auditLogs(q)}
 @Get('settings') adminSettings(@Req() r:any){return this.service.adminSettings(r.admin)}
 @Patch('settings/profile') updateAdminProfile(@Req() r:any,@Body() b:any){return this.service.updateAdminProfile(r.admin,b)}
 @Patch('settings/preferences') updateAdminPreferences(@Req() r:any,@Body() b:any){return this.service.updateAdminPreferences(r.admin,b)}
 @Get('settings/global') globalAdminSettings(@Req() r:any){return this.service.globalAdminSettings(r.admin)}
 @Patch('settings/global') updateGlobalAdminSettings(@Req() r:any,@Body() b:any){return this.service.updateGlobalAdminSettings(r.admin,b)}
 @Get('final-qa') finalQa(@Req() r:any){return this.service.finalQa(r.admin)}
}
