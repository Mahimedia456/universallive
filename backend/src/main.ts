import { ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

type CorsCallback = (error: Error | null, allow?: boolean) => void;

function allowedOrigins(): string[] {
  const configured = (process.env.CORS_ORIGINS || '')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean);

  return Array.from(
    new Set([
      'http://localhost:5173',
      'http://127.0.0.1:5173',
      'http://localhost:4173',
      'http://127.0.0.1:4173',
      'https://universallive.vercel.app',
      ...configured.filter((value) => value !== '*'),
    ]),
  );
}

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.setGlobalPrefix('api/v1');

  const configured = process.env.CORS_ORIGINS?.trim();
  const origins = allowedOrigins();

  app.enableCors({
    origin(
      origin: string | undefined,
      callback: CorsCallback,
    ) {
      if (!origin) {
        callback(null, true);
        return;
      }

      const localAdminOrigin = /^http:\/\/(localhost|127\.0\.0\.1):\d+$/.test(origin);
      const allowDevWildcard = configured === '*' && process.env.NODE_ENV !== 'production';
      if (allowDevWildcard || origins.includes(origin) || localAdminOrigin) {
        callback(null, true);
        return;
      }

      callback(
        new Error(`Origin not allowed by CORS: ${origin}`),
        false,
      );
    },
    credentials: true,
    methods: [
      'GET',
      'HEAD',
      'POST',
      'PUT',
      'PATCH',
      'DELETE',
      'OPTIONS',
    ],
    allowedHeaders: [
      'Origin',
      'Accept',
      'Content-Type',
      'Authorization',
      'X-Requested-With',
      'x-request-id',
      'X-UniversalLive-Client',
    ],
    exposedHeaders: [
      'Content-Length',
      'Content-Type',
    ],
    optionsSuccessStatus: 204,
    preflightContinue: false,
  });

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
      forbidUnknownValues: false,
    }),
  );

  const port = Number(process.env.PORT || 3000);

  await app.listen(port, '0.0.0.0');

  console.log(
    `UniversalLive API listening on http://0.0.0.0:${port}/api/v1`,
  );
}

void bootstrap();
