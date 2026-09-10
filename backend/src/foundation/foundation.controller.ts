import {
  Controller,
  Get,
  Headers,
} from '@nestjs/common';

import {
  success,
} from '../common/api-response';

import {
  FoundationService,
} from './foundation.service';

@Controller('foundation')
export class FoundationController {
  constructor(
    private readonly foundationService:
      FoundationService,
  ) {}

  @Get()
  info(
    @Headers('x-request-id')
    requestId?: string,
  ) {
    return success(
      this.foundationService.info(),
      requestId,
    );
  }

  @Get('readiness')
  readiness(
    @Headers('x-request-id')
    requestId?: string,
  ) {
    return success(
      this.foundationService.readiness(),
      requestId,
    );
  }
}
