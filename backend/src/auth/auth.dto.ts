import { IsEmail, IsIn, IsOptional, IsString, Length, MaxLength, MinLength } from 'class-validator';

export class RegisterDto {
  @IsString() @Length(2, 80) firstName!: string;
  @IsOptional() @IsString() @MaxLength(80) lastName?: string;
  @IsEmail() @MaxLength(254) email!: string;
  @IsString() @MinLength(8) @MaxLength(128) password!: string;
}

export class LoginDto {
  @IsEmail() @MaxLength(254) email!: string;
  @IsString() @MinLength(8) @MaxLength(128) password!: string;
}

export class VerifyOtpDto {
  @IsEmail() @MaxLength(254) email!: string;
  @IsString() @Length(6, 6) code!: string;
}

export class ResendOtpDto {
  @IsEmail() @MaxLength(254) email!: string;
  @IsIn(['verify_email', 'password_reset']) purpose!: 'verify_email' | 'password_reset';
}

export class ForgotPasswordDto {
  @IsEmail() @MaxLength(254) email!: string;
}

export class VerifyResetOtpDto extends VerifyOtpDto {}

export class ResetPasswordDto {
  @IsString() @MinLength(20) resetToken!: string;
  @IsString() @MinLength(8) @MaxLength(128) newPassword!: string;
}

export class RefreshTokenDto {
  @IsString() @MinLength(32) refreshToken!: string;
}

export class LogoutDto {
  @IsOptional() @IsString() @MinLength(32) refreshToken?: string;
}
