import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const ACCESS_KEY = 'sizeme.accessToken';
const REFRESH_KEY = 'sizeme.refreshToken';

export type StoredAuthTokens = {
  accessToken: string;
  refreshToken: string;
};

async function setItem(key: string, value: string) {
  if (Platform.OS === 'web') await AsyncStorage.setItem(key, value);
  else await SecureStore.setItemAsync(key, value);
}

async function getItem(key: string) {
  if (Platform.OS === 'web') return AsyncStorage.getItem(key);
  return SecureStore.getItemAsync(key);
}

async function deleteItem(key: string) {
  if (Platform.OS === 'web') await AsyncStorage.removeItem(key);
  else await SecureStore.deleteItemAsync(key);
}

export async function saveAuthTokens(tokens: StoredAuthTokens) {
  await Promise.all([setItem(ACCESS_KEY, tokens.accessToken), setItem(REFRESH_KEY, tokens.refreshToken)]);
}

export async function getAuthTokens(): Promise<StoredAuthTokens | null> {
  const [accessToken, refreshToken] = await Promise.all([getItem(ACCESS_KEY), getItem(REFRESH_KEY)]);
  return accessToken && refreshToken ? { accessToken, refreshToken } : null;
}

export async function getAccessToken() { return getItem(ACCESS_KEY); }
export async function getRefreshToken() { return getItem(REFRESH_KEY); }

export async function clearAuthTokens() {
  await Promise.all([deleteItem(ACCESS_KEY), deleteItem(REFRESH_KEY)]);
}
