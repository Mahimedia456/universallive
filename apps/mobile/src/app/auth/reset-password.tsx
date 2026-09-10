import { router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';
import { BackButton } from '@/components/flow/BackButton';
import { AppButton } from '@/components/ui/AppButton';
import { AppText } from '@/components/ui/AppText';
import { AuthScaffold } from '@/features/auth/components/AuthScaffold';
import { PasswordInput } from '@/features/auth/components/PasswordInput';
import { useSessionStore } from '@/store/session.store';

export default function ResetPasswordScreen() {
  const updatePassword = useSessionStore((state) => state.updatePassword);
  const resetToken = useSessionStore((state) => state.resetToken);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{ password?: string; confirm?: string; form?: string }>({});

  const submit = async () => {
    const next: typeof errors = {};
    if (password.length < 8) next.password = 'Use at least 8 characters.';
    if (password !== confirm) next.confirm = 'Passwords do not match.';
    setErrors(next);
    if (Object.keys(next).length) return;
    setLoading(true);
    try {
      await updatePassword(password);
      router.replace('/auth/login');
    } catch (e) {
      setErrors({ form: e instanceof Error ? e.message : 'Unable to update password.' });
    } finally { setLoading(false); }
  };

  return (
    <AuthScaffold eyebrow="NEW PASSWORD" title="Create a new password" subtitle="Your verified SizeME recovery code authorizes this password change." topAction={<BackButton onPress={() => router.back()} />}>
      <View style={{ gap: 16 }}>
        {!resetToken ? <AppText variant="caption" tone="danger">Verify your recovery code first.</AppText> : null}
        <PasswordInput value={password} onChangeText={setPassword} error={errors.password} placeholder="New password" />
        <PasswordInput label="Confirm password" value={confirm} onChangeText={setConfirm} error={errors.confirm} placeholder="Repeat new password" />
        {errors.form ? <AppText variant="caption" tone="danger">{errors.form}</AppText> : null}
        <AppButton label="Save new password" loading={loading} disabled={!resetToken} onPress={() => void submit()} style={{ marginTop: 4 }} />
      </View>
    </AuthScaffold>
  );
}
