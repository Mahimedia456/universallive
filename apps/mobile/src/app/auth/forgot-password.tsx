import { router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';
import { BackButton } from '@/components/flow/BackButton';
import { AppButton } from '@/components/ui/AppButton';
import { AppInput } from '@/components/ui/AppInput';
import { AppText } from '@/components/ui/AppText';
import { AuthScaffold } from '@/features/auth/components/AuthScaffold';
import { useSessionStore } from '@/store/session.store';

export default function ForgotPasswordScreen() {
  const beginPasswordReset = useSessionStore((state) => state.beginPasswordReset);
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) return setError('Enter a valid email address.');
    setError(''); setLoading(true);
    try {
      await beginPasswordReset(email);
      router.push({ pathname: '/auth/verify-otp', params: { mode: 'recovery' } });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to send recovery code.');
    } finally { setLoading(false); }
  };

  return (
    <AuthScaffold eyebrow="ACCOUNT RECOVERY" title="Reset your password" subtitle="Enter your account email. SizeME will send a 6-digit password-reset code." topAction={<BackButton onPress={() => router.back()} />}>
      <View style={{ gap: 18 }}>
        <AppInput label="Email" value={email} onChangeText={setEmail} placeholder="you@example.com" keyboardType="email-address" autoCapitalize="none" error={error || undefined} />
        <AppButton label="Send recovery code" loading={loading} onPress={() => void submit()} />
        <AppText variant="caption" tone="muted" style={{ textAlign: 'center' }}>For privacy, SizeME shows the same response whether or not an email exists.</AppText>
      </View>
    </AuthScaffold>
  );
}
