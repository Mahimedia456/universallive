import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, View } from 'react-native';
import { BackButton } from '@/components/flow/BackButton';
import { AppButton } from '@/components/ui/AppButton';
import { AppInput } from '@/components/ui/AppInput';
import { AppText } from '@/components/ui/AppText';
import { AuthScaffold } from '@/features/auth/components/AuthScaffold';
import { PasswordInput } from '@/features/auth/components/PasswordInput';
import { useProfileStore } from '@/store/profile.store';
import { useSessionStore } from '@/store/session.store';

export default function RegisterScreen() {
  const signUp = useSessionStore((state) => state.signUp);
  const setBasics = useProfileStore((state) => state.setBasics);
  const sizingProfile = useProfileStore((state) => state.sizingProfile);
  const birthYear = useProfileStore((state) => state.birthYear);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const submit = async () => {
    const next: Record<string, string> = {};
    if (firstName.trim().length < 2) next.firstName = 'Enter your first name.';
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) next.email = 'Enter a valid email address.';
    if (password.length < 8) next.password = 'Use at least 8 characters.';
    if (confirm !== password) next.confirm = 'Passwords do not match.';
    setErrors(next);
    if (Object.keys(next).length) return;

    setLoading(true);
    try {
      await signUp(firstName, lastName, email, password);
      setBasics({ displayName: [firstName.trim(), lastName.trim()].filter(Boolean).join(' '), sizingProfile, birthYear });
      router.replace({ pathname: '/auth/verify-otp', params: { mode: 'signup' } });
    } catch (error) {
      setErrors({ form: error instanceof Error ? error.message : 'Unable to create account.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthScaffold
      eyebrow="CREATE YOUR PROFILE"
      title="Start with the essentials"
      subtitle="SizeME creates your account securely, then emails a 6-digit verification code. No browser confirmation link is required."
      topAction={<BackButton onPress={() => router.back()} />}
    >
      <View style={{ gap: 15 }}>
        <View style={{ flexDirection: 'row', gap: 10 }}>
          <View style={{ flex: 1 }}><AppInput label="First name" value={firstName} onChangeText={setFirstName} placeholder="First name" error={errors.firstName} /></View>
          <View style={{ flex: 1 }}><AppInput label="Last name" value={lastName} onChangeText={setLastName} placeholder="Last name" /></View>
        </View>
        <AppInput label="Email" value={email} onChangeText={setEmail} placeholder="you@example.com" keyboardType="email-address" autoCapitalize="none" error={errors.email} />
        <PasswordInput value={password} onChangeText={setPassword} error={errors.password} placeholder="At least 8 characters" />
        <PasswordInput label="Confirm password" value={confirm} onChangeText={setConfirm} error={errors.confirm} placeholder="Repeat password" />
        {errors.form ? <AppText variant="caption" tone="danger">{errors.form}</AppText> : null}
        <AppButton label="Create account" loading={loading} onPress={() => void submit()} />
        <AppText variant="caption" tone="muted" style={{ textAlign: 'center' }}>
          A 6-digit SizeME OTP will be sent to this email and expires automatically.
        </AppText>
      </View>
      <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 24, gap: 4 }}>
        <AppText variant="caption" tone="secondary">Already registered?</AppText>
        <Pressable onPress={() => router.replace('/auth/login')}><AppText variant="caption" tone="primary">Sign in</AppText></Pressable>
      </View>
    </AuthScaffold>
  );
}
