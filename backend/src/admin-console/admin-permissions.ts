export const ADMIN_ROLE_PERMISSIONS:Record<string,string[]>={
 SUPER_ADMIN:['*'],
 ADMIN:['dashboard.read','users.read','users.write','creators.read','creators.write','streams.read','streams.write','connections.read','connections.write','diagnostics.read','analytics.read','membership.read','membership.write','billing.read','notifications.read','notifications.write','support.read','support.write','moderation.read','moderation.write','system.read','flags.read','flags.write','audit.read','settings.self'],
 SUPPORT:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','membership.read','billing.read','notifications.read','support.read','support.write','system.read','settings.self'],
 MODERATOR:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','support.read','moderation.read','moderation.write','system.read','settings.self'],
 FINANCE:['dashboard.read','users.read','membership.read','membership.write','billing.read','analytics.read','audit.read','settings.self'],
 VIEWER:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','membership.read','billing.read','notifications.read','support.read','moderation.read','system.read','flags.read','audit.read','settings.self']
};
export function adminHasPermission(admin:any,permission:string){
 const role=ADMIN_ROLE_PERMISSIONS[String(admin?.role||'VIEWER')]||[],explicit=Array.isArray(admin?.permissions)?admin.permissions:[];
 return role.includes('*')||role.includes(permission)||explicit.includes('*')||explicit.includes(permission);
}
