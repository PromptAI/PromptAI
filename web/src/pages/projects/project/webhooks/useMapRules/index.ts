import useLocale from '@/utils/useLocale';
import { RulesProps } from '@arco-design/web-react';
import { useMemo } from 'react';
import i18n from './locale';

export default function useMapRules() {
  const t = useLocale(i18n);
  return useMemo<RulesProps[]>(
    () => [
      {
        validator: (val, callback) => {
          // val 是一个数组，里面是[{key:xx,value:xx}]
          // 需要校验，key不能重复
          const keys = val.map((item: { key: any; }) => item.key);

          // 使用 Set 来检查是否有重复的 key
          const isDuplicate = keys.length !== new Set(keys).size;
          if (isDuplicate) {
            // 如果有重复的 key，返回错误
            callback(t['map.format.error']);
          } else {
            // 没有重复，校验通过
            callback();
          }
        },
      },
    ],
    [t]
  );
}
