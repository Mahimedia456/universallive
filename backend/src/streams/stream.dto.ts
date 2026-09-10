import { IsIn, IsInt, IsOptional, IsString, Max, MaxLength, Min } from 'class-validator';

export class StartStreamDto {
  @IsOptional() @IsString() destinationId?: string;
  @IsOptional() @IsString() @MaxLength(120) title?: string;
  @IsOptional() @IsString() @MaxLength(40) platform?: string;
  @IsOptional() @IsInt() @Min(1) @Max(100000) targetBitrateKbps?: number;
  @IsOptional() @IsInt() @Min(1) @Max(240) fps?: number;
  @IsOptional() @IsInt() @Min(120) @Max(8192) width?: number;
  @IsOptional() @IsInt() @Min(120) @Max(8192) height?: number;
}

export class UpdateStreamStatusDto {
  @IsIn(['starting', 'connecting', 'live', 'reconnecting', 'stopping', 'ended', 'error'])
  status!: string;
  @IsOptional() @IsString() @MaxLength(500) message?: string;
}

export class StreamMetricDto {
  @IsOptional() @IsInt() @Min(0) bitrateKbps?: number;
  @IsOptional() @IsInt() @Min(0) publishedVideoFrames?: number;
  @IsOptional() @IsInt() @Min(0) publishedAudioFrames?: number;
  @IsOptional() @IsInt() @Min(0) droppedFrames?: number;
  @IsOptional() @IsInt() @Min(0) reconnectCount?: number;
  @IsOptional() @IsString() @MaxLength(40) health?: string;
}
