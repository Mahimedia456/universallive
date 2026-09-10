import { Module } from '@nestjs/common';

import { BackendSupabase } from '../common/backend-supabase';
import { AdminConsoleController } from './admin-console.controller';
import { AdminConsoleService } from './admin-console.service';

@Module({
  controllers: [AdminConsoleController],
  providers: [
    AdminConsoleService,
    BackendSupabase,
  ],
})
export class AdminConsoleModule {}
