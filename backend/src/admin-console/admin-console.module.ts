import { Module } from '@nestjs/common';

import { AuthV2Module } from '../auth-v2/auth-v2.module';
import { BackendSupabase } from '../common/backend-supabase';
import { AdminConsoleController } from './admin-console.controller';
import { AdminConsoleService } from './admin-console.service';

@Module({
  imports: [AuthV2Module],
  controllers: [AdminConsoleController],
  providers: [
    AdminConsoleService,
    BackendSupabase,
  ],
})
export class AdminConsoleModule {}
