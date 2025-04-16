import { UserState, useSelectorStore } from '@/store';

export default function useFeatureEnable() {
  const userInfo = useSelectorStore<UserState>('user');
  return userInfo?.featureEnable;
}
