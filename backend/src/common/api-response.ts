export interface ApiMeta {
  requestId?: string;
  timestamp: string;
  apiVersion: string;
}

export interface ApiSuccess<T> {
  success: true;
  data: T;
  meta: ApiMeta;
}

export interface ApiFailure {
  success: false;
  error: {
    code: string;
    message: string;
    details?: unknown;
  };
  meta: ApiMeta;
}

export const API_VERSION = 'v1';

export function success<T>(
  data: T,
  requestId?: string,
): ApiSuccess<T> {
  return {
    success: true,
    data,
    meta: {
      requestId,
      timestamp: new Date().toISOString(),
      apiVersion: API_VERSION,
    },
  };
}
