import { IntentMapping, IntentNextData } from '@/graph-next/type';
import { useCallback, useRef, useState } from 'react';
import { MappingTypeEnum } from '../enums';
// 是否可标注
export default function useMarkEnable(initialData: IntentNextData) {
  const [enable, setEnable] = useState(
    () =>
      !!(
        initialData?.mappingsEnable &&
        initialData?.mappings?.some(
          (m) => m.type === MappingTypeEnum.FROM_ENTITY
        )
      )
  );
  const cache = useRef(enable);
  const onComputeChange = useCallback(
    (mappings?: IntentMapping[], mappingsEnable?: boolean) => {
      const current =
        !!mappingsEnable &&
        !!mappings?.some(
          (m) => m.type === MappingTypeEnum.FROM_ENTITY && !!m.slotId
        );
      if (cache.current !== current) {
        cache.current = current;
        setEnable(current);
      }
    },
    []
  );
  return [enable, onComputeChange, setEnable] as const;
}
