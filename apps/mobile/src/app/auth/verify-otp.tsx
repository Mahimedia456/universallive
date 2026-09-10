import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';
import { BackButton } from '@/components/flow/BackButton';
import { AppButton } from '@/components/ui/AppButton';
import { AppInput } from '@/components/ui/AppInput';
import { AppText } from '@/components/ui/AppText';
import { AuthScaffold } from '@/features/auth/components/AuthScaffold';
import { loadCloudProfile } from '@/services/cloud-profile';
import { useSessionStore } from '@/store/session.store';

export default function VerifyOtpScreen() {
  const params = useLocalSearchParams<{ mode?: string }>();
  const mode = params.mode === 'recovery' ? 'recovery' : 'signup';
  const pendingEmail = useSessionStore((state) => state.pendingEmail);
  const recoveryEmail = useSessionStore((state) => state.recoveryEmail);
  const verifySignupOtp = useSessionStore((state) => state.verifySignupOtp);
  const verifyRecoveryOtp = useSessionStore((state) => state.verifyRecoveryOtp);
  const resendSignupOtp = useSessionStore((state) => state.resendSignupOtp);
  const resendRecoveryOtp = useSessionStore((state) => state.resendRecoveryOtp);
  const email = mode === 'recovery' ? recoveryEmail : pendingEmail;
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [message, setMessage] = useState('');

  const submit = async () => {
    if (!email) return setError('Email context is missing. Go back and request a new code.');
    if (!/^\d{6}$/.test(code)) return setError('Enter the 6-digit code from your SizeME email.');
    setError(''); setLoading(true);
    try {
      if (mode === 'recovery') {
        await verifyRecoveryOtp(email, code);
        router.replace('/auth/reset-password');
      } else {
        await verifySignupOtp(email, code);
        await loadCloudProfile();
        router.replace('/profile-setup');
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to verify the code.');
    } finally { setLoading(false); }
  };

  const resend = async () => {
    if (!email) return;
    setMessage(''); setError(''); setResending(true);
    try {
      if (mode === 'recovery') await resendRecoveryOtp(email);
      else await resendSignupOtp(email);
      setMessage('A new 6-digit SizeME code was sent.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to resend code.');
    } finally { setResending(false); }
  };

  return (
    <AuthScaffold
      eyebrow="VERIFY EMAIL"
      title={mode === 'recovery' ? 'Enter recovery code' : 'Confirm your account'}
      subtitle={email ? `Enter the 6-digit code sent to ${email}.` : 'Request a fresh code to continue.'}
      topAction={<BackButton onPress={() => router.back()} />}
    >
      <View style={{ gap: 18 }}>
        <AppInput
          label="Verification code"
          value={code}
          onChangeText={(value) => setCode(value.replace(/\D/g, '').slice(0, 6))}
          placeholder="000000"
          keyboardType="number-pad"
          maxLength={6}
          error={error || undefined}
          style={{ fontSize: 24, letterSpacing: 7, textAlign: 'center' }}
        />
        {message ? <AppText variant="caption" tone="primary" style={{ textAlign: 'center' }}>{message}</AppText> : null}
        <AppButton label="Verify code" loading={loading} onPress={() => void submit()} />
        <AppButton label="Resend code" variant="secondary" loading={resending} onPress={() => void resend()} />
        <AppText variant="caption" tone="muted" style={{ textAlign: 'center' }}>Codes expire in 10 minutes and are limited to 5 attempts.</AppText>
      </View>
    </AuthScaffold>
  );
}
