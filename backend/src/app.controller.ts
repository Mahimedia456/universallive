import { Controller, Get } from '@nestjs/common';

@Controller()
export class AppController {
  @Get()
  getApiRoot() {
    return {
      ok: true,
      service: 'UniversalLive Backend',
      name: 'Universal Live API',
      status: 'online',
      apiVersion: 'v1',
      mobileContractVersion: '2026.09',
      baseUrl: '/api/v1',
      endpoints: {
        health: '/api/v1/health',
        foundation: '/api/v1/foundation',
        plans: '/api/v1/billing/plans',
        security: '/api/v1/security/status',
        finalStatus: '/api/v1/system/final-status',
      },
    };
  }
}
