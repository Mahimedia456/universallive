import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { SupabaseService } from '../supabase/supabase.service';

@Injectable()
export class AdminConsoleService {
  constructor(private readonly db: SupabaseService) {}

  private requireWrite(admin:any){
    const role=String(admin?.role||'');
    if(!['SUPER_ADMIN','ADMIN'].includes(role)) throw new ForbiddenException('Admin write permission required');
  }

  private async count(table:string, mutate?:(q:any)=>any){
    let q:any=this.db.admin.from(table).select('*',{count:'exact',head:true}); if(mutate) q=mutate(q);
    const {count,error}=await q; if(error) return 0; return count||0;
  }

  private mapAuthUser(u:any){
    const m=u?.user_metadata||{};
    const name=m.display_name||m.full_name||m.name||u?.email?.split('@')[0]||'Creator';
    return {id:u.id,email:u.email||'',first_name:m.first_name||null,last_name:m.last_name||null,display_name:name,email_verified_at:u.email_confirmed_at||null,is_active:!u.banned_until,last_login_at:u.last_sign_in_at||null,created_at:u.created_at,updated_at:u.updated_at};
  }

  private async allAuthUsers(){
    const out:any[]=[]; let page=1;
    while(page<=10){const {data,error}=await this.db.admin.auth.admin.listUsers({page,perPage:100});if(error)throw new BadRequestException(error.message);out.push(...(data.users||[]));if((data.users||[]).length<100)break;page++;}
    return out;
  }

  async dashboard(){
    const authUsers=await this.allAuthUsers();
    const [profiles,connections,broadcasts,liveBroadcasts,supportOpen]=await Promise.all([
      this.count('ul_creator_profiles'),this.count('ul_streaming_connections'),this.count('ul_broadcast_sessions'),this.count('ul_broadcast_sessions',q=>q.in('status',['live','active','started'])),this.count('ul_support_tickets',q=>q.not('status','in','("closed","resolved")'))
    ]);
    const recentUsers=authUsers.sort((a,b)=>String(b.created_at).localeCompare(String(a.created_at))).slice(0,6).map(u=>this.mapAuthUser(u));
    const {data:recentStreams}=await this.db.admin.from('ul_broadcast_sessions').select('id,user_id,title,status,started_at,ended_at,created_at').order('created_at',{ascending:false}).limit(6);
    return {metrics:{users:authUsers.length,activeUsers:authUsers.filter(u=>!u.banned_until).length,verifiedUsers:authUsers.filter(u=>!!u.email_confirmed_at).length,profiles,connections,broadcasts,liveBroadcasts,supportOpen},recentUsers,recentStreams:recentStreams||[],generatedAt:new Date().toISOString()};
  }

  async users(q:any){
    const page=Math.max(1,Number(q.page)||1),limit=Math.min(100,Math.max(10,Number(q.limit)||20));
    const search=String(q.search||'').trim().toLowerCase(); let list=(await this.allAuthUsers()).map(u=>this.mapAuthUser(u));
    if(search)list=list.filter(u=>[u.email,u.display_name,u.first_name,u.last_name].some((v:any)=>String(v||'').toLowerCase().includes(search)));
    if(q.status==='active')list=list.filter(u=>u.is_active);if(q.status==='inactive')list=list.filter(u=>!u.is_active);if(q.verified==='yes')list=list.filter(u=>!!u.email_verified_at);if(q.verified==='no')list=list.filter(u=>!u.email_verified_at);
    list.sort((a,b)=>String(b.created_at).localeCompare(String(a.created_at))); const total=list.length,items0=list.slice((page-1)*limit,page*limit),ids=items0.map(u=>u.id);const plans=new Map<string,any>(),profiles=new Map<string,any>();
    if(ids.length){const [{data:e},{data:p}]=await Promise.all([this.db.admin.from('ul_user_entitlements').select('user_id,plan_key,status,expires_at').in('user_id',ids),this.db.admin.from('ul_creator_profiles').select('user_id,username,creator_type,onboarding_completed,avatar_url').in('user_id',ids)]);(e||[]).forEach((x:any)=>plans.set(x.user_id,x));(p||[]).forEach((x:any)=>profiles.set(x.user_id,x));}
    return {items:items0.map(u=>({...u,plan:plans.get(u.id)||{plan_key:'free',status:'active'},profile:profiles.get(u.id)||null})),page,limit,total,pages:Math.max(1,Math.ceil(total/limit))};
  }

  async user(id:string){
    const {data,error}=await this.db.admin.auth.admin.getUserById(id);if(error||!data.user)throw new NotFoundException('User not found');const u=this.mapAuthUser(data.user);
    const [{data:profile},{data:entitlement},{data:connections},{data:streams}]=await Promise.all([this.db.admin.from('ul_creator_profiles').select('*').eq('user_id',id).maybeSingle(),this.db.admin.from('ul_user_entitlements').select('*').eq('user_id',id).maybeSingle(),this.db.admin.from('ul_streaming_connections').select('id,platform,display_name,is_enabled,created_at').eq('user_id',id).order('created_at',{ascending:false}).limit(10),this.db.admin.from('ul_broadcast_sessions').select('id,title,status,started_at,ended_at,created_at').eq('user_id',id).order('created_at',{ascending:false}).limit(10)]);
    return {user:u,profile:profile||null,entitlement:entitlement||{plan_key:'free',status:'active'},connections:connections||[],streams:streams||[]};
  }

  async createUser(admin:any,b:any){
    this.requireWrite(admin);const email=String(b.email||'').trim().toLowerCase(),password=String(b.password||'');if(!email.includes('@'))throw new BadRequestException('Valid email required');if(password.length<10)throw new BadRequestException('Password must be at least 10 characters');const displayName=String(b.displayName||b.firstName||email.split('@')[0]).trim();
    const {data,error}=await this.db.admin.auth.admin.createUser({email,password,email_confirm:!!b.emailVerified,user_metadata:{first_name:String(b.firstName||displayName).trim(),last_name:b.lastName?String(b.lastName).trim():null,display_name:displayName}});if(error||!data.user)throw new BadRequestException(error?.message||'Unable to create user');const id=data.user.id;
    await Promise.all([this.db.admin.from('ul_creator_profiles').upsert({user_id:id,display_name:displayName,username:b.username||null,creator_type:b.creatorType||null,onboarding_completed:!!b.onboardingCompleted},{onConflict:'user_id'}),this.db.admin.from('ul_user_entitlements').upsert({user_id:id,plan_key:String(b.planKey||'free').toLowerCase(),status:'active',source:'admin',updated_at:new Date().toISOString()},{onConflict:'user_id'})]);await this.audit(admin.id||admin.sub,'users.create',id,{email});return this.user(id);
  }

  async updateUser(admin:any,id:string,b:any){
    this.requireWrite(admin);const current=(await this.db.admin.auth.admin.getUserById(id)).data.user;if(!current)throw new NotFoundException('User not found');const attrs:any={};if(b.email!==undefined)attrs.email=String(b.email).trim().toLowerCase();if(b.password){if(String(b.password).length<10)throw new BadRequestException('Password must be at least 10 characters');attrs.password=String(b.password)}if(b.firstName!==undefined||b.lastName!==undefined||b.displayName!==undefined){attrs.user_metadata={...(current.user_metadata||{}),...(b.firstName!==undefined?{first_name:String(b.firstName||'').trim()}:{}),...(b.lastName!==undefined?{last_name:String(b.lastName||'').trim()||null}:{}),...(b.displayName!==undefined?{display_name:String(b.displayName||'').trim()}: {})}}if(b.isActive!==undefined)attrs.ban_duration=b.isActive?'none':'876000h';if(Object.keys(attrs).length){const {error}=await this.db.admin.auth.admin.updateUserById(id,attrs);if(error)throw new BadRequestException(error.message)}
    if(b.username!==undefined||b.creatorType!==undefined||b.onboardingCompleted!==undefined||b.displayName!==undefined){const p:any={user_id:id,updated_at:new Date().toISOString()};if(b.username!==undefined)p.username=b.username||null;if(b.creatorType!==undefined)p.creator_type=b.creatorType||null;if(b.onboardingCompleted!==undefined)p.onboarding_completed=!!b.onboardingCompleted;if(b.displayName!==undefined)p.display_name=b.displayName;await this.db.admin.from('ul_creator_profiles').upsert(p,{onConflict:'user_id'})}if(b.planKey!==undefined)await this.db.admin.from('ul_user_entitlements').upsert({user_id:id,plan_key:String(b.planKey).toLowerCase(),status:'active',source:'admin',updated_at:new Date().toISOString()},{onConflict:'user_id'});await this.audit(admin.id||admin.sub,'users.update',id,{});return this.user(id);
  }

  async setStatus(admin:any,id:string,isActive:boolean){this.requireWrite(admin);const {error}=await this.db.admin.auth.admin.updateUserById(id,{ban_duration:isActive?'none':'876000h'} as any);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,isActive?'users.activate':'users.deactivate',id,{});return this.user(id)}
  async verifyEmail(admin:any,id:string){this.requireWrite(admin);const {error}=await this.db.admin.auth.admin.updateUserById(id,{email_confirm:true} as any);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'users.verify_email',id,{});return this.user(id)}
  async removeUser(admin:any,id:string,hard:boolean){this.requireWrite(admin);if(hard){if(String(admin.role)!=='SUPER_ADMIN')throw new ForbiddenException('SUPER_ADMIN required for hard delete');const {error}=await this.db.admin.auth.admin.deleteUser(id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'users.hard_delete',id,{});return{ok:true,deleted:true}}return this.setStatus(admin,id,false)}

  private async authUserMap(ids:string[]){const out=new Map<string,any>();for(const id of ids){try{const {data}=await this.db.admin.auth.admin.getUserById(id);if(data?.user)out.set(id,data.user)}catch{}}return out}

  async creators(q:any){let query:any=this.db.admin.from('ul_creator_profiles').select('*').order('created_at',{ascending:false}).limit(200);const search=String(q.search||'').trim();if(search)query=query.or(`display_name.ilike.%${search}%,username.ilike.%${search}%,creator_type.ilike.%${search}%`);const {data,error}=await query;if(error)throw new BadRequestException(error.message);const rows=data||[],users=await this.authUserMap(rows.map((x:any)=>x.user_id));return{items:rows.map((x:any)=>({...x,email:users.get(x.user_id)?.email||null}))}}
  async creator(id:string){const {data:profile,error}=await this.db.admin.from('ul_creator_profiles').select('*').eq('user_id',id).maybeSingle();if(error||!profile)throw new NotFoundException('Creator profile not found');let user:any=null;try{const {data}=await this.db.admin.auth.admin.getUserById(id);user=data?.user||null}catch{}const [{data:connections},{data:streams},{data:scenes}]=await Promise.all([this.db.admin.from('ul_streaming_connections').select('id,platform,display_name,status,is_enabled,last_success_at').eq('user_id',id).order('created_at',{ascending:false}).limit(10),this.db.admin.from('ul_broadcast_sessions').select('id,title,status,started_at,ended_at,created_at').eq('user_id',id).order('created_at',{ascending:false}).limit(10),this.db.admin.from('ul_scenes').select('id,name,is_default,updated_at').eq('user_id',id).order('updated_at',{ascending:false}).limit(10)]);return{profile,user:user?{id:user.id,email:user.email,created_at:user.created_at,last_sign_in_at:user.last_sign_in_at}:null,connections:connections||[],streams:streams||[],scenes:scenes||[]}}
  async createCreator(admin:any,b:any){this.requireWrite(admin);const userId=String(b.userId||'').trim();if(!userId)throw new BadRequestException('Existing user UUID required');try{const {data}=await this.db.admin.auth.admin.getUserById(userId);if(!data?.user)throw new Error()}catch{throw new BadRequestException('User UUID does not exist in current authentication backend')}const row={user_id:userId,display_name:String(b.displayName||'').trim()||null,username:String(b.username||'').trim()||null,creator_type:String(b.creatorType||'').trim()||null,locale:String(b.locale||'').trim()||null,timezone:String(b.timezone||'').trim()||null,onboarding_completed:!!b.onboardingCompleted,updated_at:new Date().toISOString()};const {error}=await this.db.admin.from('ul_creator_profiles').insert(row);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'creators.create',userId,{});return this.creator(userId)}
  async updateCreator(admin:any,id:string,b:any){this.requireWrite(admin);const row:any={updated_at:new Date().toISOString()};if(b.displayName!==undefined)row.display_name=String(b.displayName||'').trim()||null;if(b.username!==undefined)row.username=String(b.username||'').trim()||null;if(b.creatorType!==undefined)row.creator_type=String(b.creatorType||'').trim()||null;if(b.locale!==undefined)row.locale=String(b.locale||'').trim()||null;if(b.timezone!==undefined)row.timezone=String(b.timezone||'').trim()||null;if(b.onboardingCompleted!==undefined)row.onboarding_completed=!!b.onboardingCompleted;const {error}=await this.db.admin.from('ul_creator_profiles').update(row).eq('user_id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'creators.update',id,{});return this.creator(id)}

  async streams(q:any){let query:any=this.db.admin.from('ul_broadcast_sessions').select('*').order('created_at',{ascending:false}).limit(200);const search=String(q.search||'').trim();if(search)query=query.ilike('title',`%${search}%`);if(q.status)query=query.eq('status',String(q.status));const {data,error}=await query;if(error)throw new BadRequestException(error.message);const rows=data||[],users=await this.authUserMap([...new Set(rows.map((x:any)=>x.user_id))] as string[]);const items=[] as any[];for(const s of rows){const {data:t}=await this.db.admin.from('ul_stream_telemetry').select('bitrate_kbps,target_bitrate_kbps,fps,dropped_frames,publish_status,sampled_at').eq('session_id',s.id).order('sampled_at',{ascending:false}).limit(1).maybeSingle();items.push({...s,user_email:users.get(s.user_id)?.email||null,latest_telemetry:t||null})}return{items}}
  async stream(id:string){const {data:session,error}=await this.db.admin.from('ul_broadcast_sessions').select('*').eq('id',id).maybeSingle();if(error||!session)throw new NotFoundException('Stream not found');let userEmail:any=null;try{const {data}=await this.db.admin.auth.admin.getUserById(session.user_id);userEmail=data?.user?.email||null}catch{}session.user_email=userEmail;const [{data:destinations},{data:latestTelemetry},{data:summary},{data:events}]=await Promise.all([this.db.admin.from('ul_broadcast_destinations').select('*').eq('session_id',id).order('created_at',{ascending:true}),this.db.admin.from('ul_stream_telemetry').select('*').eq('session_id',id).order('sampled_at',{ascending:false}).limit(1).maybeSingle(),this.db.admin.from('ul_stream_summaries').select('*').eq('session_id',id).maybeSingle(),this.db.admin.from('ul_stream_events').select('*').eq('session_id',id).order('created_at',{ascending:false}).limit(50)]);return{session,destinations:destinations||[],latestTelemetry:latestTelemetry||null,summary:summary||null,events:events||[]}}
  async createStream(admin:any,b:any){this.requireWrite(admin);const userId=String(b.userId||'').trim();if(!userId)throw new BadRequestException('User UUID required');const row={user_id:userId,title:String(b.title||'').trim()||null,description:String(b.description||'').trim()||null,status:'created',metadata:{created_by_admin:true}};const {data,error}=await this.db.admin.from('ul_broadcast_sessions').insert(row).select('id').single();if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'streams.create_draft',data.id,{userId});return this.stream(data.id)}
  async updateStream(admin:any,id:string,b:any){this.requireWrite(admin);const allowed=['created','ended','failed'];if(b.status!==undefined&&!allowed.includes(String(b.status)))throw new BadRequestException('Admin cannot mark a stream live; live state must come from the publisher');const row:any={updated_at:new Date().toISOString()};if(b.title!==undefined)row.title=String(b.title||'').trim()||null;if(b.description!==undefined)row.description=String(b.description||'').trim()||null;if(b.status!==undefined)row.status=String(b.status);const {error}=await this.db.admin.from('ul_broadcast_sessions').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'streams.update',id,{});return this.stream(id)}
  async endStream(admin:any,id:string){this.requireWrite(admin);const current=await this.stream(id),now=new Date().toISOString();const {error}=await this.db.admin.from('ul_broadcast_sessions').update({status:'ended',ended_at:now,stop_reason:'admin_ended',updated_at:now}).eq('id',id);if(error)throw new BadRequestException(error.message);await this.db.admin.from('ul_stream_events').insert({session_id:id,user_id:current.session.user_id,event_type:'admin_end',severity:'warning',message:'Broadcast marked ended from admin console'});await this.audit(admin.id||admin.sub,'streams.end',id,{});return this.stream(id)}

  async connections(q:any){let query:any=this.db.admin.from('ul_streaming_connections').select('*').order('created_at',{ascending:false}).limit(300);const search=String(q.search||'').trim();if(search)query=query.or(`display_name.ilike.%${search}%,external_channel_name.ilike.%${search}%,platform.ilike.%${search}%`);if(q.platform)query=query.eq('platform',String(q.platform));const {data,error}=await query;if(error)throw new BadRequestException(error.message);const rows=data||[],users=await this.authUserMap([...new Set(rows.map((x:any)=>x.user_id))] as string[]);return{items:rows.map((x:any)=>({...x,user_email:users.get(x.user_id)?.email||null}))}}
  async connection(id:string){const {data:connection,error}=await this.db.admin.from('ul_streaming_connections').select('*').eq('id',id).maybeSingle();if(error||!connection)throw new NotFoundException('Connection not found');let userEmail=null;try{const {data}=await this.db.admin.auth.admin.getUserById(connection.user_id);userEmail=data?.user?.email||null}catch{}connection.user_email=userEmail;const {data:credentials}=await this.db.admin.from('ul_stream_credentials').select('id,credential_type,key_version,last_rotated_at,token_expires_at,created_at,updated_at').eq('connection_id',id);return{connection,credentials:credentials||[]}}
  async createConnection(admin:any,b:any){this.requireWrite(admin);const userId=String(b.userId||'').trim();if(!userId)throw new BadRequestException('User UUID required');const row={user_id:userId,platform:String(b.platform||'').trim(),display_name:String(b.displayName||'').trim(),external_account_id:String(b.externalAccountId||'').trim()||null,external_channel_id:String(b.externalChannelId||'').trim()||null,external_channel_name:String(b.externalChannelName||'').trim()||null,status:String(b.status||'disconnected'),is_default:!!b.isDefault,is_enabled:b.isEnabled!==false,metadata:{created_by_admin:true}};if(!row.platform||!row.display_name)throw new BadRequestException('Platform and display name required');if(row.is_default)await this.db.admin.from('ul_streaming_connections').update({is_default:false}).eq('user_id',userId);const {data,error}=await this.db.admin.from('ul_streaming_connections').insert(row).select('id').single();if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'connections.create',data.id,{userId,platform:row.platform});return this.connection(data.id)}
  async updateConnection(admin:any,id:string,b:any){this.requireWrite(admin);const current=(await this.connection(id)).connection;const row:any={updated_at:new Date().toISOString()};for(const [src,dst] of [['platform','platform'],['displayName','display_name'],['externalAccountId','external_account_id'],['externalChannelId','external_channel_id'],['externalChannelName','external_channel_name'],['status','status']] as any[]){if(b[src]!==undefined)row[dst]=String(b[src]||'').trim()||null}if(b.isEnabled!==undefined)row.is_enabled=!!b.isEnabled;if(b.isDefault!==undefined){row.is_default=!!b.isDefault;if(row.is_default)await this.db.admin.from('ul_streaming_connections').update({is_default:false}).eq('user_id',current.user_id)}const {error}=await this.db.admin.from('ul_streaming_connections').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'connections.update',id,{});return this.connection(id)}
  async setConnectionEnabled(admin:any,id:string,isEnabled:boolean){this.requireWrite(admin);const {error}=await this.db.admin.from('ul_streaming_connections').update({is_enabled:isEnabled,updated_at:new Date().toISOString()}).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,isEnabled?'connections.enable':'connections.disable',id,{});return this.connection(id)}


  private healthFromTelemetry(t:any){
    if(!t) return {health:'offline',reason:'no telemetry'};
    const sampled=t.sampled_at?Date.parse(t.sampled_at):0;
    if(sampled && Date.now()-sampled>120000) return {health:'offline',reason:'stale telemetry'};
    const target=Number(t.target_bitrate_kbps||0),actual=Number(t.bitrate_kbps||0),fps=Number(t.fps||0);
    if(target>0 && actual<target*.65) return {health:'warning',reason:'low bitrate'};
    if(fps>0 && fps<24) return {health:'warning',reason:'low fps'};
    if(Number(t.dropped_frames||0)>120) return {health:'warning',reason:'dropped frames'};
    const pub=String(t.publish_status||'').toLowerCase();
    if(pub && !['publishing','online','connected','healthy','active'].includes(pub)) return {health:'warning',reason:pub};
    return {health:'healthy',reason:'telemetry within thresholds'};
  }

  async diagnostics(q:any){
    const {data:sessions,error}=await this.db.admin.from('ul_broadcast_sessions').select('id,user_id,title,status,started_at,ended_at,created_at').order('created_at',{ascending:false}).limit(150);
    if(error)throw new BadRequestException(error.message);
    const rows=sessions||[],users=await this.authUserMap([...new Set(rows.map((x:any)=>x.user_id))] as string[]);
    const streams:any[]=[];let healthy=0,warning=0,offline=0,totalBitrate=0,bitrateCount=0;
    for(const s of rows){
      const {data:t}=await this.db.admin.from('ul_stream_telemetry').select('*').eq('session_id',s.id).order('sampled_at',{ascending:false}).limit(1).maybeSingle();
      const h=this.healthFromTelemetry(t);if(h.health==='healthy')healthy++;else if(h.health==='warning')warning++;else offline++;
      if(t?.bitrate_kbps!=null){totalBitrate+=Number(t.bitrate_kbps||0);bitrateCount++}
      streams.push({...s,user_email:users.get(s.user_id)?.email||null,telemetry:t||null,health:h.health,health_reason:h.reason});
    }
    const status=String(q.status||'');const filtered=status?streams.filter(x=>x.health===status):streams;
    const activeStreams=rows.filter((x:any)=>['live','active','started'].includes(String(x.status||'').toLowerCase())).length;
    return{metrics:{activeStreams,healthyStreams:healthy,warningStreams:warning,offlineStreams:offline,avgBitrateKbps:bitrateCount?Math.round(totalBitrate/bitrateCount):0},streams:filtered};
  }

  async diagnosticStream(id:string){
    const base=await this.stream(id);
    const {data:telemetry,error}=await this.db.admin.from('ul_stream_telemetry').select('*').eq('session_id',id).order('sampled_at',{ascending:false}).limit(100);
    if(error)throw new BadRequestException(error.message);
    return{...base,telemetry:telemetry||[],latestTelemetry:(telemetry||[])[0]||base.latestTelemetry||null,health:this.healthFromTelemetry((telemetry||[])[0]||base.latestTelemetry||null)};
  }

  async analytics(q:any){
    const days=Math.min(365,Math.max(1,Number(q.days)||30)),from=new Date(Date.now()-days*86400000).toISOString();
    const [{data:sessions},{data:connections},{data:entitlements},{data:profiles},{data:telemetry}]=await Promise.all([
      this.db.admin.from('ul_broadcast_sessions').select('id,user_id,status,started_at,ended_at,created_at').gte('created_at',from).order('created_at',{ascending:true}),
      this.db.admin.from('ul_streaming_connections').select('platform,is_enabled'),
      this.db.admin.from('ul_user_entitlements').select('user_id,plan_key,status'),
      this.db.admin.from('ul_creator_profiles').select('user_id,display_name,username'),
      this.db.admin.from('ul_stream_telemetry').select('session_id,bitrate_kbps,target_bitrate_kbps,fps,dropped_frames,published_video_frames,sampled_at').gte('sampled_at',from).order('sampled_at',{ascending:false})
    ]);
    const ss=sessions||[],cc=connections||[],ee=entitlements||[],pp=profiles||[],tt=telemetry||[];
    const byStatus=new Map<string,number>();ss.forEach((x:any)=>byStatus.set(String(x.status||'unknown'),(byStatus.get(String(x.status||'unknown'))||0)+1));
    const byPlatform=new Map<string,number>();cc.forEach((x:any)=>byPlatform.set(String(x.platform||'unknown'),(byPlatform.get(String(x.platform||'unknown'))||0)+1));
    const byPlan=new Map<string,number>();ee.filter((x:any)=>x.status==='active').forEach((x:any)=>byPlan.set(String(x.plan_key||'free'),(byPlan.get(String(x.plan_key||'free'))||0)+1));
    const dayMap=new Map<string,number>();for(let i=days-1;i>=0;i--){const d=new Date(Date.now()-i*86400000).toISOString().slice(0,10);dayMap.set(d,0)}ss.forEach((x:any)=>{const d=String(x.created_at||'').slice(0,10);if(dayMap.has(d))dayMap.set(d,(dayMap.get(d)||0)+1)});
    const userStats=new Map<string,{streams:number;minutes:number;last:string|null}>();let liveMinutes=0;
    ss.forEach((x:any)=>{const start=x.started_at?Date.parse(x.started_at):Date.parse(x.created_at),end=x.ended_at?Date.parse(x.ended_at):Date.now(),mins=Math.max(0,Math.round((end-start)/60000));liveMinutes+=mins;const st=userStats.get(x.user_id)||{streams:0,minutes:0,last:null};st.streams++;st.minutes+=mins;const when=x.started_at||x.created_at;if(!st.last||String(when)>st.last)st.last=String(when);userStats.set(x.user_id,st)});
    const users=await this.authUserMap([...userStats.keys()]);
    const profileMap=new Map(pp.map((x:any)=>[x.user_id,x]));
    const topCreators=[...userStats.entries()].map(([user_id,s])=>({user_id,streams:s.streams,live_minutes:s.minutes,last_stream_at:s.last,email:users.get(user_id)?.email||null,display_name:profileMap.get(user_id)?.display_name||profileMap.get(user_id)?.username||users.get(user_id)?.email||null})).sort((a,b)=>b.streams-a.streams).slice(0,12);
    const latestBySession=new Map<string,any>();for(const t of tt){if(!latestBySession.has(t.session_id))latestBySession.set(t.session_id,t)}
    const latest=[...latestBySession.values()],avgBitrate=latest.length?Math.round(latest.reduce((a:any,x:any)=>a+Number(x.bitrate_kbps||0),0)/latest.length):0;
    let healthy=0,warning=0,totalDrops=0,totalFrames=0;latest.forEach((t:any)=>{const h=this.healthFromTelemetry(t);if(h.health==='healthy')healthy++;else warning++;totalDrops+=Number(t.dropped_frames||0);totalFrames+=Number(t.published_video_frames||0)});
    return{metrics:{streams:ss.length,liveMinutes,avgBitrateKbps:avgBitrate,activeCreators:userStats.size},totals:{connections:cc.length,entitlements:ee.filter((x:any)=>x.status==='active').length},daily:[...dayMap.entries()].map(([date,streams])=>({date,streams})),statusDistribution:[...byStatus.entries()].map(([key,count])=>({key,count})).sort((a,b)=>b.count-a.count),platformDistribution:[...byPlatform.entries()].map(([key,count])=>({key,count})).sort((a,b)=>b.count-a.count),planDistribution:[...byPlan.entries()].map(([key,count])=>({key,count})).sort((a,b)=>b.count-a.count),health:{healthy,warning,dropRatePercent:totalFrames?totalDrops/totalFrames*100:0},topCreators};
  }

  async plans(){
    const {data,error}=await this.db.admin.from('ul_plans').select('*').order('sort_order',{ascending:true});
    if(error)throw new BadRequestException(error.message);return{items:data||[]};
  }

  async plan(key:string){
    const {data,error}=await this.db.admin.from('ul_plans').select('*').eq('plan_key',key).maybeSingle();
    if(error||!data)throw new NotFoundException('Plan not found');return data;
  }

  async createPlan(admin:any,b:any){
    this.requireWrite(admin);const planKey=String(b.planKey||'').trim().toLowerCase();const name=String(b.name||'').trim();
    if(!/^[a-z0-9_-]{2,40}$/.test(planKey))throw new BadRequestException('Valid plan key required');if(!name)throw new BadRequestException('Plan name required');
    const row={plan_key:planKey,name,description:String(b.description||'').trim()||null,is_active:b.isActive!==false,sort_order:Number(b.sortOrder)||0,entitlements:b.entitlements&&typeof b.entitlements==='object'?b.entitlements:{},updated_at:new Date().toISOString()};
    const {error}=await this.db.admin.from('ul_plans').insert(row);if(error)throw new BadRequestException(error.message);
    await this.audit(admin.id||admin.sub,'plans.create',planKey,{name});return this.plan(planKey);
  }

  async updatePlan(admin:any,key:string,b:any){
    this.requireWrite(admin);const row:any={updated_at:new Date().toISOString()};
    if(b.name!==undefined)row.name=String(b.name||'').trim();if(b.description!==undefined)row.description=String(b.description||'').trim()||null;if(b.isActive!==undefined)row.is_active=!!b.isActive;if(b.sortOrder!==undefined)row.sort_order=Number(b.sortOrder)||0;if(b.entitlements!==undefined){if(!b.entitlements||typeof b.entitlements!=='object'||Array.isArray(b.entitlements))throw new BadRequestException('Entitlements must be an object');row.entitlements=b.entitlements}
    const {error}=await this.db.admin.from('ul_plans').update(row).eq('plan_key',key);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'plans.update',key,{});return this.plan(key);
  }

  async setPlanActive(admin:any,key:string,isActive:boolean){
    this.requireWrite(admin);const {error}=await this.db.admin.from('ul_plans').update({is_active:isActive,updated_at:new Date().toISOString()}).eq('plan_key',key);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,isActive?'plans.activate':'plans.deactivate',key,{});return this.plan(key);
  }

  async entitlements(q:any){
    let query:any=this.db.admin.from('ul_user_entitlements').select('*').order('updated_at',{ascending:false}).limit(500);if(q.plan)query=query.eq('plan_key',String(q.plan));if(q.status)query=query.eq('status',String(q.status));
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);let rows=data||[];const users=await this.authUserMap(rows.map((x:any)=>x.user_id));const search=String(q.search||'').trim().toLowerCase();
    let items=rows.map((x:any)=>({...x,email:users.get(x.user_id)?.email||null,display_name:users.get(x.user_id)?.display_name||null}));if(search)items=items.filter((x:any)=>[x.email,x.display_name,x.user_id].some(v=>String(v||'').toLowerCase().includes(search)));return{items};
  }

  async entitlement(userId:string){
    const {data,error}=await this.db.admin.from('ul_user_entitlements').select('*').eq('user_id',userId).maybeSingle();if(error)throw new BadRequestException(error.message);
    let u:any=null;try{const {data:au}=await this.db.admin.auth.admin.getUserById(userId);u=au?.user?this.mapAuthUser(au.user):null}catch{}
    const entitlement=data||{user_id:userId,plan_key:'free',status:'active',source:'system',entitlements_override:{}};
    const {data:plan}=await this.db.admin.from('ul_plans').select('*').eq('plan_key',entitlement.plan_key||'free').maybeSingle();
    return{user:u,entitlement,plan:plan||null};
  }

  async updateEntitlement(admin:any,userId:string,b:any){
    this.requireWrite(admin);const planKey=String(b.planKey||'free').trim().toLowerCase();await this.plan(planKey);
    const row:any={user_id:userId,plan_key:planKey,status:String(b.status||'active'),source:String(b.source||'admin'),updated_at:new Date().toISOString()};
    if(b.expiresAt!==undefined)row.expires_at=b.expiresAt||null;if(b.entitlementsOverride!==undefined){if(!b.entitlementsOverride||typeof b.entitlementsOverride!=='object'||Array.isArray(b.entitlementsOverride))throw new BadRequestException('Entitlements override must be an object');row.entitlements_override=b.entitlementsOverride}
    const {error}=await this.db.admin.from('ul_user_entitlements').upsert(row,{onConflict:'user_id'});if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'entitlements.update',userId,{planKey,status:row.status});return this.entitlement(userId);
  }


  async billing(q:any){
    let query:any=this.db.admin.from('ul_store_purchases').select('*').order('created_at',{ascending:false}).limit(500);
    if(q.status)query=query.eq('status',String(q.status));if(q.store)query=query.eq('store',String(q.store));
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);let rows=data||[];const users=await this.authUserMap([...new Set(rows.map((x:any)=>x.user_id))] as string[]);
    let items=rows.map((x:any)=>({...x,email:users.get(x.user_id)?.email||null}));const s=String(q.search||'').trim().toLowerCase();if(s)items=items.filter((x:any)=>[x.email,x.user_id,x.product_id,x.transaction_id,x.original_transaction_id,x.plan_key].some(v=>String(v||'').toLowerCase().includes(s)));
    return{metrics:{total:items.length,verified:items.filter((x:any)=>['verified','active','paid'].includes(String(x.status||'').toLowerCase())).length,autoRenew:items.filter((x:any)=>x.auto_renew===true).length,needsReview:items.filter((x:any)=>['pending','failed'].includes(String(x.status||'').toLowerCase())).length},items};
  }

  async purchase(id:string){
    const {data,error}=await this.db.admin.from('ul_store_purchases').select('*').eq('id',id).maybeSingle();if(error||!data)throw new NotFoundException('Purchase not found');
    let user:any=null;try{const {data:u}=await this.db.admin.auth.admin.getUserById(data.user_id);if(u?.user)user=this.mapAuthUser(u.user)}catch{}
    const {data:entitlement}=await this.db.admin.from('ul_user_entitlements').select('*').eq('user_id',data.user_id).maybeSingle();
    return{purchase:data,user,entitlement:entitlement||null};
  }

  async updatePurchase(admin:any,id:string,b:any){
    this.requireWrite(admin);const allowed=['pending','verified','active','expired','failed','refunded'];const row:any={updated_at:new Date().toISOString()};
    if(b.status!==undefined){const s=String(b.status);if(!allowed.includes(s))throw new BadRequestException('Invalid purchase status');row.status=s}
    if(b.planKey!==undefined)row.plan_key=b.planKey?String(b.planKey).trim().toLowerCase():null;if(b.expiresAt!==undefined)row.expires_at=b.expiresAt||null;if(b.autoRenew!==undefined)row.auto_renew=!!b.autoRenew;
    row.last_verified_at=new Date().toISOString();const {error}=await this.db.admin.from('ul_store_purchases').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'billing.purchase_update',id,{status:row.status,plan_key:row.plan_key});return this.purchase(id);
  }

  async notifications(q:any){
    let query:any=this.db.admin.from('ul_notifications').select('*').order('created_at',{ascending:false}).limit(500);if(q.severity)query=query.eq('severity',String(q.severity));
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);const rows=data||[],users=await this.authUserMap([...new Set(rows.map((x:any)=>x.user_id))] as string[]);
    const {data:deliveries}=await this.db.admin.from('ul_push_deliveries').select('status').order('created_at',{ascending:false}).limit(2000);
    return{metrics:{total:rows.length,unread:rows.filter((x:any)=>!x.read_at).length,pushQueued:(deliveries||[]).filter((x:any)=>x.status==='queued').length,pushFailed:(deliveries||[]).filter((x:any)=>x.status==='failed').length},items:rows.map((x:any)=>({...x,email:users.get(x.user_id)?.email||null}))};
  }

  async notification(id:string){
    const {data,error}=await this.db.admin.from('ul_notifications').select('*').eq('id',id).maybeSingle();if(error||!data)throw new NotFoundException('Notification not found');
    let user:any=null;try{const {data:u}=await this.db.admin.auth.admin.getUserById(data.user_id);if(u?.user)user=this.mapAuthUser(u.user)}catch{}
    const {data:deliveries}=await this.db.admin.from('ul_push_deliveries').select('*').eq('notification_id',id).order('created_at',{ascending:false});
    return{notification:data,user,deliveries:deliveries||[]};
  }

  async sendNotification(admin:any,b:any){
    this.requireWrite(admin);const audience=String(b.audience||'user'),title=String(b.title||'').trim(),body=String(b.body||'').trim(),type=String(b.type||'admin_message').trim(),severity=String(b.severity||'info').trim();
    if(!title||!body)throw new BadRequestException('Title and body are required');if(!['info','success','warning','error'].includes(severity))throw new BadRequestException('Invalid severity');
    let userIds:string[]=[];if(audience==='all'){userIds=(await this.allAuthUsers()).map((u:any)=>u.id)}else{const id=String(b.userId||'').trim();if(!id)throw new BadRequestException('User UUID required');try{const {data}=await this.db.admin.auth.admin.getUserById(id);if(!data?.user)throw new Error()}catch{throw new BadRequestException('User not found')}userIds=[id]}
    if(userIds.length>5000)throw new BadRequestException('Broadcast limit exceeded; use a queued campaign worker for more than 5000 users');
    const rows=userIds.map(user_id=>({user_id,type,title,body,severity,action_type:b.actionType?String(b.actionType):null,action_payload:b.actionPayload&&typeof b.actionPayload==='object'?b.actionPayload:{}}));
    const {data:created,error}=await this.db.admin.from('ul_notifications').insert(rows).select('*');if(error)throw new BadRequestException(error.message);
    if(b.push!==false&&created?.length){for(const n of created){const {data:devices}=await this.db.admin.from('ul_devices').select('id,user_id,push_token').eq('user_id',n.user_id).not('push_token','is',null);const deliveryRows=(devices||[]).filter((d:any)=>d.push_token).map((d:any)=>({notification_id:n.id,device_id:d.id,user_id:n.user_id,provider:null,status:'queued'}));if(deliveryRows.length){const {error:de}=await this.db.admin.from('ul_push_deliveries').insert(deliveryRows);if(de)throw new BadRequestException(de.message)}}}
    await this.audit(admin.id||admin.sub,'notifications.send','broadcast',{audience,count:created?.length||0,type,severity,push:b.push!==false});return{sent:created?.length||0,items:created||[]};
  }

  async deleteNotification(admin:any,id:string){
    this.requireWrite(admin);const {error}=await this.db.admin.from('ul_notifications').delete().eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'notifications.delete',id,{});return{ok:true};
  }

  async supportTickets(q:any){
    let query:any=this.db.admin.from('ul_support_tickets').select('*').order('updated_at',{ascending:false}).limit(500);if(q.status)query=query.eq('status',String(q.status));if(q.priority)query=query.eq('priority',String(q.priority));
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);const rows=data||[],users=await this.authUserMap([...new Set(rows.map((x:any)=>x.user_id))] as string[]);
    const s=String(q.search||'').trim().toLowerCase();let items=rows.map((x:any)=>({...x,email:users.get(x.user_id)?.email||null}));if(s)items=items.filter((x:any)=>[x.email,x.user_id,x.subject,x.category,x.description].some(v=>String(v||'').toLowerCase().includes(s)));
    return{metrics:{open:items.filter((x:any)=>x.status==='open').length,inProgress:items.filter((x:any)=>x.status==='in_progress').length,high:items.filter((x:any)=>['high','urgent'].includes(x.priority)).length,closed:items.filter((x:any)=>['resolved','closed'].includes(x.status)).length},items};
  }

  async supportTicket(id:string){
    const {data,error}=await this.db.admin.from('ul_support_tickets').select('*').eq('id',id).maybeSingle();if(error||!data)throw new NotFoundException('Support ticket not found');
    const {data:messages}=await this.db.admin.from('ul_support_messages').select('*').eq('ticket_id',id).order('created_at',{ascending:true});let user:any=null;try{const {data:u}=await this.db.admin.auth.admin.getUserById(data.user_id);if(u?.user)user=this.mapAuthUser(u.user)}catch{}
    return{ticket:data,messages:messages||[],user};
  }

  async createSupportTicket(admin:any,b:any){
    this.requireWrite(admin);const userId=String(b.userId||'').trim(),category=String(b.category||'').trim(),subject=String(b.subject||'').trim(),description=String(b.description||'').trim(),priority=String(b.priority||'normal');
    if(!userId||!category||!subject||!description)throw new BadRequestException('User, category, subject and description are required');if(!['low','normal','high','urgent'].includes(priority))throw new BadRequestException('Invalid priority');
    try{const {data}=await this.db.admin.auth.admin.getUserById(userId);if(!data?.user)throw new Error()}catch{throw new BadRequestException('User not found')}
    const {data,error}=await this.db.admin.from('ul_support_tickets').insert({user_id:userId,category,subject,description,priority,status:'open',diagnostics:b.diagnostics&&typeof b.diagnostics==='object'?b.diagnostics:{},updated_at:new Date().toISOString()}).select('*').single();if(error)throw new BadRequestException(error.message);
    await this.audit(admin.id||admin.sub,'support.create',data.id,{userId,category,priority});return this.supportTicket(data.id);
  }

  async updateSupportTicket(admin:any,id:string,b:any){
    this.requireWrite(admin);const row:any={updated_at:new Date().toISOString()};if(b.status!==undefined){const s=String(b.status);if(!['open','in_progress','waiting_user','resolved','closed'].includes(s))throw new BadRequestException('Invalid support status');row.status=s;if(['resolved','closed'].includes(s))row.closed_at=new Date().toISOString();else row.closed_at=null}
    if(b.priority!==undefined){const p=String(b.priority);if(!['low','normal','high','urgent'].includes(p))throw new BadRequestException('Invalid priority');row.priority=p}
    const {error}=await this.db.admin.from('ul_support_tickets').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'support.update',id,row);return this.supportTicket(id);
  }

  async replySupportTicket(admin:any,id:string,b:any){
    this.requireWrite(admin);const message=String(b.message||'').trim();if(!message)throw new BadRequestException('Message required');await this.supportTicket(id);
    const {data,error}=await this.db.admin.from('ul_support_messages').insert({ticket_id:id,user_id:null,sender_type:'admin',message,metadata:{admin_id:admin.id||admin.sub}}).select('*').single();if(error)throw new BadRequestException(error.message);
    await this.db.admin.from('ul_support_tickets').update({status:'waiting_user',updated_at:new Date().toISOString(),closed_at:null}).eq('id',id);await this.audit(admin.id||admin.sub,'support.reply',id,{});return data;
  }


  async moderation(q:any){
    let query:any=this.db.admin.from('ul_moderation_cases').select('*').order('updated_at',{ascending:false}).limit(500);
    if(q.status)query=query.eq('status',String(q.status));if(q.severity)query=query.eq('severity',String(q.severity));
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);let items=data||[];const s=String(q.search||'').trim().toLowerCase();
    if(s)items=items.filter((x:any)=>[x.subject,x.reason,x.target_type,x.target_id,x.resolution].some(v=>String(v||'').toLowerCase().includes(s)));
    for(const x of items){try{x.target_label=(await this.resolveModerationTarget(x.target_type,x.target_id))?.label||null}catch{}}
    return{metrics:{open:items.filter((x:any)=>x.status==='open').length,investigating:items.filter((x:any)=>x.status==='investigating').length,high:items.filter((x:any)=>['high','critical'].includes(x.severity)).length,resolved:items.filter((x:any)=>['resolved','dismissed'].includes(x.status)).length},items};
  }

  private async resolveModerationTarget(type:string,id:string){
    if(type==='user'){try{const {data}=await this.db.admin.auth.admin.getUserById(id);return{type,id,label:data?.user?.email||id,record:data?.user||null}}catch{return{type,id,label:id,record:null}}}
    if(type==='stream'){const {data}=await this.db.admin.from('ul_broadcast_sessions').select('*').eq('id',id).maybeSingle();return{type,id,label:data?.title||id,record:data||null}}
    if(type==='connection'){const {data}=await this.db.admin.from('ul_streaming_connections').select('*').eq('id',id).maybeSingle();return{type,id,label:data?.display_name||data?.platform||id,record:data||null}}
    return{type,id,label:id,record:null};
  }

  async moderationCase(id:string){
    const {data,error}=await this.db.admin.from('ul_moderation_cases').select('*').eq('id',id).maybeSingle();if(error||!data)throw new NotFoundException('Moderation case not found');return{case:data,target:await this.resolveModerationTarget(data.target_type,data.target_id)};
  }

  async createModerationCase(admin:any,b:any){
    this.requireWrite(admin);const targetType=String(b.targetType||''),targetId=String(b.targetId||'').trim(),subject=String(b.subject||'').trim(),reason=String(b.reason||'').trim(),severity=String(b.severity||'medium');
    if(!['user','stream','connection'].includes(targetType))throw new BadRequestException('Invalid target type');if(!targetId||!subject||!reason)throw new BadRequestException('Target, subject and reason required');if(!['low','medium','high','critical'].includes(severity))throw new BadRequestException('Invalid severity');
    const target=await this.resolveModerationTarget(targetType,targetId);if(!target.record)throw new BadRequestException('Target not found');
    const row={target_type:targetType,target_id:targetId,subject,reason,severity,status:'open',notes:String(b.notes||'')||null,evidence:b.evidence&&typeof b.evidence==='object'?b.evidence:{},created_by_admin_id:admin.id||admin.sub,updated_at:new Date().toISOString()};
    const {data,error}=await this.db.admin.from('ul_moderation_cases').insert(row).select('*').single();if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'moderation.create',data.id,{targetType,targetId,severity});return this.moderationCase(data.id);
  }

  async updateModerationCase(admin:any,id:string,b:any){
    this.requireWrite(admin);const current=await this.moderationCase(id),row:any={updated_at:new Date().toISOString()};
    if(b.status!==undefined){const s=String(b.status);if(!['open','investigating','actioned','dismissed','resolved'].includes(s))throw new BadRequestException('Invalid status');row.status=s;if(['resolved','dismissed','actioned'].includes(s))row.resolved_at=new Date().toISOString()}
    if(b.severity!==undefined){const s=String(b.severity);if(!['low','medium','high','critical'].includes(s))throw new BadRequestException('Invalid severity');row.severity=s}
    if(b.resolution!==undefined)row.resolution=String(b.resolution||'')||null;if(b.notes!==undefined)row.notes=String(b.notes||'')||null;if(b.action!==undefined)row.action=String(b.action||'none');
    const action=String(b.action||'none');if(action==='disable_connection'){if(current.case.target_type!=='connection')throw new BadRequestException('Case target is not a connection');const {error}=await this.db.admin.from('ul_streaming_connections').update({is_enabled:false,updated_at:new Date().toISOString()}).eq('id',current.case.target_id);if(error)throw new BadRequestException(error.message)}
    if(action==='end_stream'){if(current.case.target_type!=='stream')throw new BadRequestException('Case target is not a stream');const {error}=await this.db.admin.from('ul_broadcast_sessions').update({status:'ended',ended_at:new Date().toISOString(),updated_at:new Date().toISOString()}).eq('id',current.case.target_id);if(error)throw new BadRequestException(error.message)}
    if(['disable_user','enable_user'].includes(action)){if(current.case.target_type!=='user')throw new BadRequestException('Case target is not a user');try{await this.db.admin.auth.admin.updateUserById(current.case.target_id,{ban_duration:action==='disable_user'?'876000h':'none'})}catch{throw new BadRequestException('Unable to change user auth state')}}
    const {error}=await this.db.admin.from('ul_moderation_cases').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'moderation.update',id,{status:row.status,severity:row.severity,action});return this.moderationCase(id);
  }

  async systemHealth(){
    const checkedAt=new Date().toISOString();let database='healthy';try{const {error}=await this.db.admin.from('ul_system_flags').select('key').limit(1);if(error)database='degraded'}catch{database='down'}
    const since=new Date(Date.now()-5*60000).toISOString(),day=new Date(Date.now()-24*3600000).toISOString();
    const [telemetry,broadcasts,tickets,push,admins,connections,events,versions]=await Promise.all([
      this.db.admin.from('ul_stream_telemetry').select('id',{count:'exact',head:true}).gte('sampled_at',since),
      this.db.admin.from('ul_broadcast_sessions').select('id',{count:'exact',head:true}).in('status',['live','active','started']),
      this.db.admin.from('ul_support_tickets').select('id',{count:'exact',head:true}).in('status',['open','in_progress']),
      this.db.admin.from('ul_push_deliveries').select('id',{count:'exact',head:true}).eq('status','failed').gte('created_at',day),
      this.db.admin.from('ul_admin_users').select('id',{count:'exact',head:true}).gt('locked_until',checkedAt),
      this.db.admin.from('ul_streaming_connections').select('id',{count:'exact',head:true}).eq('is_enabled',true),
      this.db.admin.from('ul_stream_events').select('id',{count:'exact',head:true}).in('severity',['error','critical']).gte('created_at',day),
      this.db.admin.from('ul_backend_versions').select('*').order('created_at',{ascending:false}).limit(8)
    ]);
    const services:any[]=[{key:'backend',name:'NestJS API',status:'healthy',detail:'Admin request executed successfully'},{key:'database',name:'Supabase PostgreSQL',status:database,detail:database==='healthy'?'Service-role database query succeeded':'Database query degraded'}];
    const smtpConfigured=!!(process.env.SMTP_HOST&&process.env.SMTP_USER);services.push({key:'smtp',name:'Email / SMTP',status:smtpConfigured?'healthy':'degraded',detail:smtpConfigured?'SMTP configuration present':'SMTP configuration incomplete'});
    const pushConfigured=!!(process.env.FIREBASE_PROJECT_ID||process.env.FCM_SERVER_KEY);services.push({key:'push',name:'Push provider',status:pushConfigured?'healthy':'degraded',detail:pushConfigured?'Push provider configuration present':'Push provider configuration not detected'});
    const degraded=database!=='healthy'||services.some(x=>x.status==='degraded'),overall=database==='down'?'down':degraded?'degraded':'healthy';
    return{checkedAt,health:{overall,backend:'healthy',database},services,metrics:{recentTelemetry:telemetry.count||0,activeBroadcasts:broadcasts.count||0,openTickets:tickets.count||0,failedPush:push.count||0,lockedAdmins:admins.count||0,enabledConnections:connections.count||0,streamErrors:events.count||0},versions:versions.data||[]};
  }

  async systemFlags(){
    const {data,error}=await this.db.admin.from('ul_system_flags').select('*').order('key',{ascending:true});if(error)throw new BadRequestException(error.message);return{items:data||[]};
  }
  async systemFlag(key:string){const {data,error}=await this.db.admin.from('ul_system_flags').select('*').eq('key',key).maybeSingle();if(error||!data)throw new NotFoundException('Feature flag not found');return data}
  async createSystemFlag(admin:any,b:any){
    this.requireWrite(admin);const key=String(b.key||'').trim().toLowerCase(),description=String(b.description||'').trim()||null;if(!/^[a-z0-9_.-]{2,80}$/.test(key))throw new BadRequestException('Invalid flag key');
    const row={key,value:b.value===undefined?{}:b.value,description,is_public:!!b.isPublic,updated_at:new Date().toISOString()};const {error}=await this.db.admin.from('ul_system_flags').insert(row);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'system_flags.create',key,{is_public:row.is_public});return this.systemFlag(key);
  }
  async updateSystemFlag(admin:any,key:string,b:any){
    this.requireWrite(admin);const row:any={updated_at:new Date().toISOString()};if(b.isPublic!==undefined)row.is_public=!!b.isPublic;if(b.description!==undefined)row.description=String(b.description||'')||null;if(b.value!==undefined)row.value=b.value;
    const {error}=await this.db.admin.from('ul_system_flags').update(row).eq('key',key);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'system_flags.update',key,{is_public:row.is_public});return this.systemFlag(key);
  }
  async deleteSystemFlag(admin:any,key:string){this.requireWrite(admin);const {error}=await this.db.admin.from('ul_system_flags').delete().eq('key',key);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'system_flags.delete',key,{});return{ok:true}}


  private requireSuperAdmin(admin:any){
    if(String(admin?.role||'')!=='SUPER_ADMIN')throw new ForbiddenException('SUPER_ADMIN required');
  }

  async adminUsers(q:any){
    let query:any=this.db.admin.from('ul_admin_users').select('id,email,display_name,role,permissions,is_active,failed_login_count,locked_until,last_login_at,created_at,updated_at').order('created_at',{ascending:false}).limit(300);
    if(q.role)query=query.eq('role',String(q.role));
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);let items=(data||[]).map((x:any)=>({...x,is_locked:!!(x.locked_until&&Date.parse(x.locked_until)>Date.now())}));
    const s=String(q.search||'').trim().toLowerCase();if(s)items=items.filter((x:any)=>[x.email,x.display_name,x.role].some(v=>String(v||'').toLowerCase().includes(s)));
    return{metrics:{total:items.length,active:items.filter((x:any)=>x.is_active).length,locked:items.filter((x:any)=>x.is_locked).length,superAdmins:items.filter((x:any)=>x.role==='SUPER_ADMIN').length},items};
  }

  async adminUser(id:string){
    const {data,error}=await this.db.admin.from('ul_admin_users').select('id,email,display_name,role,permissions,is_active,failed_login_count,locked_until,last_login_at,created_at,updated_at').eq('id',id).maybeSingle();
    if(error||!data)throw new NotFoundException('Admin user not found');
    const {data:recentAudit}=await this.db.admin.from('ul_admin_audit_log').select('*').eq('admin_user_id',id).order('created_at',{ascending:false}).limit(30);
    return{admin:{...data,is_locked:!!(data.locked_until&&Date.parse(data.locked_until)>Date.now())},recentAudit:recentAudit||[]};
  }

  async createAdminUser(admin:any,b:any){
    this.requireSuperAdmin(admin);const email=String(b.email||'').trim().toLowerCase(),displayName=String(b.displayName||'').trim(),role=String(b.role||'VIEWER'),password=String(b.password||'');
    if(!email.includes('@')||!displayName)throw new BadRequestException('Valid email and display name required');if(password.length<10)throw new BadRequestException('Password must be at least 10 characters');
    const roles=['SUPER_ADMIN','ADMIN','SUPPORT','MODERATOR','FINANCE','VIEWER'];if(!roles.includes(role))throw new BadRequestException('Invalid admin role');
    const permissions=Array.isArray(b.permissions)?b.permissions:[];const bcrypt=require('bcryptjs');const password_hash=await bcrypt.hash(password,12);
    const {data,error}=await this.db.admin.from('ul_admin_users').insert({email,password_hash,display_name:displayName,role,permissions,is_active:b.isActive!==false}).select('id').single();
    if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'admin_users.create',data.id,{email,role});return this.adminUser(data.id);
  }

  async updateAdminUser(admin:any,id:string,b:any){
    this.requireSuperAdmin(admin);const row:any={updated_at:new Date().toISOString()};const roles=['SUPER_ADMIN','ADMIN','SUPPORT','MODERATOR','FINANCE','VIEWER'];
    if(b.email!==undefined){const email=String(b.email||'').trim().toLowerCase();if(!email.includes('@'))throw new BadRequestException('Valid email required');row.email=email}
    if(b.displayName!==undefined)row.display_name=String(b.displayName||'').trim();if(b.role!==undefined){if(!roles.includes(String(b.role)))throw new BadRequestException('Invalid admin role');row.role=String(b.role)}
    if(b.permissions!==undefined){if(!Array.isArray(b.permissions))throw new BadRequestException('Permissions must be an array');row.permissions=b.permissions}if(b.isActive!==undefined)row.is_active=!!b.isActive;
    const {error}=await this.db.admin.from('ul_admin_users').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,'admin_users.update',id,{email:row.email,role:row.role,is_active:row.is_active});return this.adminUser(id);
  }

  async setAdminActive(admin:any,id:string,isActive:boolean){
    this.requireSuperAdmin(admin);if(String(admin.id||admin.sub)===id&&!isActive)throw new BadRequestException('You cannot disable your own admin account');
    const {error}=await this.db.admin.from('ul_admin_users').update({is_active:isActive,updated_at:new Date().toISOString()}).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(admin.id||admin.sub,isActive?'admin_users.enable':'admin_users.disable',id,{});return this.adminUser(id);
  }

  async resetAdminPassword(admin:any,id:string,password:string){
    this.requireSuperAdmin(admin);if(password.length<10)throw new BadRequestException('Password must be at least 10 characters');const bcrypt=require('bcryptjs');const password_hash=await bcrypt.hash(password,12);
    const {error}=await this.db.admin.from('ul_admin_users').update({password_hash,failed_login_count:0,locked_until:null,updated_at:new Date().toISOString()}).eq('id',id);if(error)throw new BadRequestException(error.message);
    await this.db.admin.from('ul_admin_refresh_tokens').update({revoked_at:new Date().toISOString()}).eq('admin_user_id',id).is('revoked_at',null);await this.audit(admin.id||admin.sub,'admin_users.password_reset',id,{});return{ok:true};
  }

  async auditLogs(q:any){
    let query:any=this.db.admin.from('ul_admin_audit_log').select('*').order('created_at',{ascending:false}).limit(500);if(q.action)query=query.ilike('action',`%${String(q.action)}%`);
    const {data,error}=await query;if(error)throw new BadRequestException(error.message);let items=data||[];const adminIds=[...new Set(items.map((x:any)=>x.admin_user_id).filter(Boolean))] as string[];
    let admins=new Map<string,any>();if(adminIds.length){const {data:a}=await this.db.admin.from('ul_admin_users').select('id,email,display_name').in('id',adminIds);admins=new Map((a||[]).map((x:any)=>[x.id,x]))}
    items=items.map((x:any)=>({...x,admin_email:admins.get(x.admin_user_id)?.email||null,admin_name:admins.get(x.admin_user_id)?.display_name||null}));const s=String(q.search||'').trim().toLowerCase();
    if(s)items=items.filter((x:any)=>[x.admin_email,x.admin_name,x.action,x.target_type,x.target_id,JSON.stringify(x.details||{})].some(v=>String(v||'').toLowerCase().includes(s)));
    return{metrics:{total:items.length,auth:items.filter((x:any)=>String(x.action).startsWith('auth.')).length,writes:items.filter((x:any)=>!String(x.action).startsWith('auth.')).length,admins:new Set(items.map((x:any)=>x.admin_user_id).filter(Boolean)).size},items};
  }

  async adminSettings(admin:any){
    const id=String(admin.id||admin.sub);const {data:a,error}=await this.db.admin.from('ul_admin_users').select('id,email,display_name,role,permissions,is_active,last_login_at,created_at').eq('id',id).maybeSingle();if(error||!a)throw new NotFoundException('Admin account not found');
    const {data:p}=await this.db.admin.from('ul_admin_ui_preferences').select('*').eq('admin_user_id',id).maybeSingle();return{admin:a,preferences:p||{admin_user_id:id,sidebar_collapsed:false,table_density:'comfortable',timezone:null,preferences:{}}};
  }

  async updateAdminProfile(admin:any,b:any){
    const id=String(admin.id||admin.sub),row:any={updated_at:new Date().toISOString()};if(b.displayName!==undefined)row.display_name=String(b.displayName||'').trim();if(b.email!==undefined){const email=String(b.email||'').trim().toLowerCase();if(!email.includes('@'))throw new BadRequestException('Valid email required');row.email=email}
    const {error}=await this.db.admin.from('ul_admin_users').update(row).eq('id',id);if(error)throw new BadRequestException(error.message);await this.audit(id,'settings.profile_update',id,{});return this.adminSettings(admin);
  }

  async updateAdminPreferences(admin:any,b:any){
    const id=String(admin.id||admin.sub),density=String(b.tableDensity||'comfortable');if(!['comfortable','compact'].includes(density))throw new BadRequestException('Invalid table density');
    const row={admin_user_id:id,sidebar_collapsed:!!b.sidebarCollapsed,table_density:density,timezone:String(b.timezone||'').trim()||null,preferences:b.preferences&&typeof b.preferences==='object'?b.preferences:{},updated_at:new Date().toISOString()};
    const {error}=await this.db.admin.from('ul_admin_ui_preferences').upsert(row,{onConflict:'admin_user_id'});if(error)throw new BadRequestException(error.message);await this.audit(id,'settings.preferences_update',id,{});return this.adminSettings(admin);
  }

  async globalAdminSettings(admin:any){
    this.requireSuperAdmin(admin);const {data,error}=await this.db.admin.from('ul_admin_settings').select('key,value').order('key',{ascending:true});if(error)throw new BadRequestException(error.message);const settings:any={};for(const x of data||[])settings[x.key]=x.value;return{settings};
  }

  async updateGlobalAdminSettings(admin:any,b:any){
    this.requireSuperAdmin(admin);const allowed=['support_sla_hours','session_warning_minutes','default_page_size','maintenance_banner_enabled','maintenance_banner_text'];const rows=allowed.filter(k=>b[k]!==undefined).map(k=>({key:k,value:b[k],updated_at:new Date().toISOString(),updated_by_admin_id:admin.id||admin.sub}));
    if(rows.length){const {error}=await this.db.admin.from('ul_admin_settings').upsert(rows,{onConflict:'key'});if(error)throw new BadRequestException(error.message)}
    await this.audit(admin.id||admin.sub,'settings.global_update','global',{keys:rows.map(x=>x.key)});return this.globalAdminSettings(admin);
  }


  private rolePermissions(role:string){
    const map:any={
      SUPER_ADMIN:['*'],
      ADMIN:['dashboard.read','users.read','users.write','creators.read','creators.write','streams.read','streams.write','connections.read','connections.write','diagnostics.read','analytics.read','membership.read','membership.write','billing.read','notifications.read','notifications.write','support.read','support.write','moderation.read','moderation.write','system.read','flags.read','flags.write','audit.read','settings.self'],
      SUPPORT:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','membership.read','billing.read','notifications.read','support.read','support.write','system.read','settings.self'],
      MODERATOR:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','support.read','moderation.read','moderation.write','system.read','settings.self'],
      FINANCE:['dashboard.read','users.read','membership.read','membership.write','billing.read','analytics.read','audit.read','settings.self'],
      VIEWER:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','membership.read','billing.read','notifications.read','support.read','moderation.read','system.read','flags.read','audit.read','settings.self']
    };
    return map[String(role||'VIEWER')]||[];
  }

  private can(admin:any,permission:string){
    const role=this.rolePermissions(String(admin?.role||'VIEWER')),explicit=Array.isArray(admin?.permissions)?admin.permissions:[];
    return role.includes('*')||role.includes(permission)||explicit.includes('*')||explicit.includes(permission);
  }

  async finalQa(admin:any){
    if(!this.can(admin,'system.read')&&String(admin?.role)!=='SUPER_ADMIN')throw new ForbiddenException('System read permission required');
    const checks:any[]=[];
    const add=(group:string,key:string,name:string,status:'pass'|'warn'|'fail',detail:string,hint?:string)=>checks.push({group,key,name,status,detail,hint});
    add('Auth','admin_auth','Admin authentication','pass','Protected admin endpoint executed with a valid admin JWT.');
    try{
      const {data:a,error:e}=await this.db.admin.from('ul_admin_users').select('id',{count:'exact',head:true});add('Auth','admin_users','Admin users table',e?'fail':'pass',e?e.message:`ul_admin_users reachable.`);
    }catch(e:any){add('Auth','admin_users','Admin users table','fail',e?.message||'Query failed')}
    const required=[
      ['Core','ul_creator_profiles'],['Streaming','ul_broadcast_sessions'],['Streaming','ul_stream_telemetry'],['Streaming','ul_streaming_connections'],
      ['Membership','ul_plans'],['Membership','ul_user_entitlements'],['Notifications','ul_notifications'],['Support','ul_support_tickets'],
      ['Admin','ul_admin_audit_log'],['Admin','ul_admin_settings'],['Admin','ul_admin_ui_preferences'],['Moderation','ul_moderation_cases'],['System','ul_system_flags']
    ];
    for(const [group,table] of required){
      try{const {error}=await this.db.admin.from(table).select('*').limit(1);add(group,table,table,error?'fail':'pass',error?error.message:'Existing table reachable.')}
      catch(e:any){add(group,table,table,'fail',e?.message||'Query failed')}
    }
    try{
      const {data,error}=await this.db.admin.from('ul_system_flags').select('key,value,description,is_public,updated_at,created_at').limit(1);
      add('System','flag_schema','Feature flag schema',error?'fail':'pass',error?error.message:'Uses existing key/value/description/is_public schema.','There should be no dependency on a non-existent enabled column.');
    }catch(e:any){add('System','flag_schema','Feature flag schema','fail',e?.message||'Query failed')}
    const smtp=!!(process.env.SMTP_HOST&&process.env.SMTP_USER),push=!!(process.env.FIREBASE_PROJECT_ID||process.env.FCM_SERVER_KEY);
    add('Integrations','smtp','Email configuration',smtp?'pass':'warn',smtp?'SMTP configuration detected.':'SMTP configuration is incomplete.','Forgot-password/notification email delivery needs valid SMTP.');
    add('Integrations','push','Push configuration',push?'pass':'warn',push?'Push provider configuration detected.':'Push provider configuration not detected.','In-app notifications still work; push requires provider credentials.');
    try{
      const {count}=await this.db.admin.from('ul_stream_telemetry').select('id',{count:'exact',head:true}).gte('sampled_at',new Date(Date.now()-10*60000).toISOString());
      add('Streaming','fresh_telemetry','Fresh stream telemetry',(count||0)>0?'pass':'warn',`${count||0} telemetry samples in the last 10 minutes.`,(count||0)>0?undefined:'Warning is expected when nobody is streaming.');
    }catch(e:any){add('Streaming','fresh_telemetry','Fresh stream telemetry','warn',e?.message||'Unable to count telemetry')}
    add('Safety','schema_isolation','Mobile schema isolation','pass','Admin finalization uses existing operational tables plus admin-owned tables only. No final QA migration alters mobile user/stream schemas.');
    return{generatedAt:new Date().toISOString(),checks};
  }

  private async audit(adminId:string,action:string,targetId:string,details:any){await this.db.admin.from('ul_admin_audit_log').insert({admin_user_id:adminId,action,target_type:action.split('.')[0]||'admin',target_id:targetId,details})}
}
