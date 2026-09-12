export type AdminRole='SUPER_ADMIN'|'ADMIN'|'SUPPORT'|'MODERATOR'|'FINANCE'|'VIEWER';

export const ROLE_PERMISSIONS:Record<AdminRole,string[]>={
 SUPER_ADMIN:['*'],
 ADMIN:['dashboard.read','users.read','users.write','creators.read','creators.write','streams.read','streams.write','connections.read','connections.write','diagnostics.read','analytics.read','membership.read','membership.write','billing.read','notifications.read','notifications.write','support.read','support.write','moderation.read','moderation.write','system.read','flags.read','flags.write','audit.read','settings.self'],
 SUPPORT:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','membership.read','billing.read','notifications.read','support.read','support.write','system.read','settings.self'],
 MODERATOR:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','support.read','moderation.read','moderation.write','system.read','settings.self'],
 FINANCE:['dashboard.read','users.read','membership.read','membership.write','billing.read','analytics.read','audit.read','settings.self'],
 VIEWER:['dashboard.read','users.read','creators.read','streams.read','connections.read','diagnostics.read','analytics.read','membership.read','billing.read','notifications.read','support.read','moderation.read','system.read','flags.read','audit.read','settings.self']
};

export function hasPermission(admin:any,permission:string){
 const role=(admin?.role||'VIEWER') as AdminRole;
 const rolePerms=ROLE_PERMISSIONS[role]||[];
 const explicit=Array.isArray(admin?.permissions)?admin.permissions:[];
 return rolePerms.includes('*')||rolePerms.includes(permission)||explicit.includes('*')||explicit.includes(permission);
}

export const PAGE_PERMISSION:Record<string,string>={
 dashboard:'dashboard.read',users:'users.read',creators:'creators.read',streams:'streams.read',connections:'connections.read',
 diagnostics:'diagnostics.read',analytics:'analytics.read',membership:'membership.read',billing:'billing.read',notifications:'notifications.read',
 support:'support.read',moderation:'moderation.read',system:'system.read',flags:'flags.read',admins:'admin.manage',audit:'audit.read',settings:'settings.self'
};
