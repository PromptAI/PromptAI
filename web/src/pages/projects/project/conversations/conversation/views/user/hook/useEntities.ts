import { IntentNextData } from '@/graph-next/type';
import { useState } from 'react';
import { MappingTypeEnum } from '../enums';

export default function useEntities(initialData: IntentNextData) {
  const [entities, setEntities] = useState(
    () =>
      initialData?.mappings?.filter(
        (m) => m.type === MappingTypeEnum.FROM_ENTITY
      ) || []
  );
  return [entities, setEntities] as const;
}
