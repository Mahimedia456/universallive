import { useMemo, useState } from 'react';
import { Pressable, View } from 'react-native';
import { router } from 'expo-router';
import { AppButton } from '@/components/ui/AppButton';
import { AppCard } from '@/components/ui/AppCard';
import { AppScreen } from '@/components/ui/AppScreen';
import { AppText } from '@/components/ui/AppText';
import { useAppTheme } from '@/hooks/useAppTheme';
import { runSystemChecks, systemCheckUrls, type SystemCheckResult } from '@/services/system-check';

const labels = [
  { key: 'database', label: 'SizeME Database', detail: 'users, profiles and auth session tables' },
  { key: 'backend', label: 'NestJS API', detail: 'SizeME backend and database connectivity' },
  { key: 'auth', label: 'SizeME Auth', detail: 'custom JWT + refresh session and profile link' },
  { key: 'ai', label: 'Python AI Worker', detail: 'pose and measurement service' },
] as const;

export default function SystemCheckScreen() {
  const { theme } = useAppTheme();
  const [running, setRunning] = useState(false);
  const [results, setResults] = useState<SystemCheckResult[]>([]);
  const resultMap = useMemo(() => new Map(results.map((item) => [item.key, item])), [results]);
  const allPassed = results.length === labels.length && results.every((item) => item.state === 'ok');

  const run = async () => {
    setRunning(true);
    setResults([]);
    try { setResults(await runSystemChecks()); }
    finally { setRunning(false); }
  };

  return (
    <AppScreen scroll contentStyle={{ paddingHorizontal: 20, paddingTop: 14, paddingBottom: 32 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
        <Pressable onPress={() => router.back()} style={({ pressed }) => ({ opacity: pressed ? 0.6 : 1, paddingVertical: 8, paddingRight: 8 })}>
          <AppText tone="primary">← Back</AppText>
        </Pressable>
      </View>

      <AppText variant="label" tone="accent" style={{ marginTop: 18 }}>DEVELOPMENT</AppText>
      <AppText variant="h1" style={{ marginTop: 6 }}>System check</AppText>
      <AppText tone="secondary" style={{ marginTop: 8 }}>
        Verify phone → SizeME API → database/auth and phone → Python AI worker before the cloud-sync phase.
      </AppText>

      <AppCard style={{ marginTop: 22 }}>
        <AppText variant="label" tone="secondary">PHONE ENDPOINTS</AppText>
        <AppText variant="caption" style={{ marginTop: 10 }}>Backend</AppText>
        <AppText variant="caption" tone="primary" selectable>{systemCheckUrls.backend}</AppText>
        <AppText variant="caption" style={{ marginTop: 10 }}>AI worker</AppText>
        <AppText variant="caption" tone="primary" selectable>{systemCheckUrls.ai}</AppText>
      </AppCard>

      <View style={{ marginTop: 16, gap: 10 }}>
        {labels.map((definition) => {
          const result = resultMap.get(definition.key);
          const ok = result?.state === 'ok';
          const failed = result?.state === 'error';
          return (
            <AppCard key={definition.key}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
                <View style={{ width: 36, height: 36, borderRadius: 12, alignItems: 'center', justifyContent: 'center', backgroundColor: ok ? theme.colors.success : failed ? theme.colors.danger : theme.colors.input }}>
                  <AppText style={{ color: ok || failed ? '#FFFFFF' : theme.colors.muted }}>{ok ? '✓' : failed ? '!' : '•'}</AppText>
                </View>
                <View style={{ flex: 1 }}>
                  <AppText variant="bodyMedium">{definition.label}</AppText>
                  <AppText variant="caption" tone={failed ? 'danger' : ok ? 'success' : 'secondary'} style={{ marginTop: 3 }}>
                    {result?.detail ?? definition.detail}
                  </AppText>
                </View>
              </View>
            </AppCard>
          );
        })}
      </View>

      {allPassed ? (
        <AppCard style={{ marginTop: 16, borderColor: theme.colors.success }}>
          <AppText variant="h3" tone="success">All systems connected</AppText>
          <AppText tone="secondary" style={{ marginTop: 6 }}>The app can reach the SizeME database/auth backend and the Python AI worker.</AppText>
        </AppCard>
      ) : null}

      <View style={{ marginTop: 22 }}>
        <AppButton label={running ? 'Checking SizeME stack…' : 'Run all checks'} loading={running} disabled={running} onPress={() => void run()} />
      </View>
    </AppScreen>
  );
}
