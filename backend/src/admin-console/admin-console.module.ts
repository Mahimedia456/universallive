import { Module } from '@nestjs/common';
import { AdminConsoleController } from './admin-console.controller';
import { AdminConsoleService } from './admin-console.service';

@Module({
  controllers: [AdminConsoleController],
  providers: [AdminConsoleService],
})
export class AdminConsoleModule {}
