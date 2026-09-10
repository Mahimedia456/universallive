import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, View } from 'react-native';
import { BackButton } from '@/components/flow/BackButton';
import { AppButton } from '@/components/ui/AppButton';
import { AppInput } from '@/components/ui/AppInput';
import { AppText } from '@/components/ui/AppText';
import { AuthScaffold } from '@/features/auth/components/AuthScaffold';
import { PasswordInput } from '@/features/auth/components/PasswordInput';
import { loadCloudProfile } from '@/services/cloud-profile';
import { useSessionStore } from '@/store/session.store';

export default function LoginScreen() {
  const signIn = useSessionStore((state) => state.signIn);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string; form?: string }>({});

  const submit = async () => {
    const nextErrors: typeof errors = {};
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) nextErrors.email = 'Enter a valid email address.';
    if (password.length < 8) nextErrors.password = 'Password must be at least 8 characters.';
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;

    setLoading(true);
    try {
      await signIn(email, password);
      const profile = await loadCloudProfile();
      router.replace(profile.onboarding_completed ? '/home' : '/profile-setup');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to sign in.';
      setErrors({ form: message });
      if (/verification.*required|code has been sent/i.test(message)) {
        useSessionStore.setState({ pendingEmail: email.trim().toLowerCase() });
        router.push({ pathname: '/auth/verify-otp', params: { mode: 'signup' } });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthScaffold eyebrow="WELCOME BACK" title="Sign in to SizeME" subtitle="Sign in with your SizeME email and password." topAction={<BackButton onPress={() => router.back()} />}>
      <View style={{ gap: 16 }}>
        <AppInput label="Email" value={email} onChangeText={setEmail} placeholder="you@example.com" keyboardType="email-address" autoCapitalize="none" autoComplete="email" error={errors.email} />
        <PasswordInput value={password} onChangeText={setPassword} error={errors.password} />
        <Pressable onPress={() => router.push('/auth/forgot-password')} style={{ alignSelf: 'flex-end', paddingVertical: 4 }}><AppText variant="caption" tone="primary">Forgot password?</AppText></Pressable>
        {errors.form ? <AppText variant="caption" tone="danger">{errors.form}</AppText> : null}
        <AppButton label="Sign in" loading={loading} onPress={() => void submit()} style={{ marginTop: 4 }} />
      </View>
      <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 26, gap: 4 }}><AppText variant="caption" tone="secondary">New to SizeME?</AppText><Pressable onPress={() => router.replace('/auth/register')}><AppText variant="caption" tone="primary">Create account</AppText></Pressable></View>
    </AuthScaffold>
  );
}
