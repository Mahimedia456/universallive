import 'reflect-metadata';
import { ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import helmet from 'helmet';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.use(helmet());
  app.setGlobalPrefix('api/v1');
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true, forbidNonWhitelisted: true }));

  const corsOrigins = process.env.CORS_ORIGINS?.trim();
  app.enableCors({
    origin: !corsOrigins || corsOrigins === '*' ? true : corsOrigins.split(',').map((v) => v.trim()),
    credentials: true,
  });

  const port = Number(process.env.PORT || 3000);
  await app.listen(port, '0.0.0.0');
  console.log(`UniversalLive API listening on http://0.0.0.0:${port}/api/v1`);
}

void bootstrap();
