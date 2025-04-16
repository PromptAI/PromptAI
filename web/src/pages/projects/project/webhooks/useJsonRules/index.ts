import useLocale from '@/utils/useLocale';
import { RulesProps } from '@arco-design/web-react';
import { useMemo } from 'react';
import i18n from './locale';

export default function useJsonRules() {
  const t = useLocale(i18n);
  return useMemo<RulesProps[]>(
    () => [
      {
        validator: (val, callback) => {
          try {
            JSON.parse(val);
          } catch (e) {
            callback(t['json.format.error']);
          }
        },
      },
    ],
    [t]
  );
}
