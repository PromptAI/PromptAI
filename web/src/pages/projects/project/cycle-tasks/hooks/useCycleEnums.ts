import { useMemo } from 'react';
import i18n from '../locale';
import useLocale from '@/utils/useLocale';

export default function useCycleEnums() {
  const t = useLocale(i18n);
  const modeEnum = useMemo(
    () => ({
      'train-interval': t['cycle.filter.type.train-interval'],
      'train-cron': t['cycle.filter.type.train-cron'],
    }),
    [t]
  );
  const statusEnum = useMemo(
    () => ({
      0: t['cycle.filter.status.false'],
      1: t['cycle.filter.status.true'],
    }),
    [t]
  );
  const trianTypeEnum = useMemo(
    () => ({
      debug: t['cycle.filter.trainType.debug'],
      publish: t['cycle.filter.trainType.publish'],
    }),
    [t]
  );
  const recordStatusEnum = useMemo(
    () => ({
      1: t['cycle.filter.status.runing'],
      2: t['cycle.filter.status.error'],
      3: t['cycle.filter.status.completed'],
    }),
    [t]
  );
  return { modeEnum, statusEnum, trianTypeEnum, recordStatusEnum };
}
