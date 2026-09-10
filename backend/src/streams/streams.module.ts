import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { StreamsController } from './streams.controller';

@Module({ imports: [AuthModule], controllers: [StreamsController] })
export class StreamsModule {}
