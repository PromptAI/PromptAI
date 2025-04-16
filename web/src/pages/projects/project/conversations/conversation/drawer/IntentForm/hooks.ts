import { IntentMapping, IntentNextData, Relations } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import { cloneDeep, isEmpty } from 'lodash';
import { useCallback, useMemo, useRef, useState } from 'react';

export enum MappingTypeEnum {
  /**
   * 文本输入值
   */
  FROM_TEXT = 'from_text',
  /**
   * 实体提取值
   */
  FROM_ENTITY = 'from_entity',
  /**
   * 自定义值
   */
  FROM_INTENT = 'from_intent',
}
// 是否可标注
export const useMarkEnable = (initialData: IntentNextData) => {
  const [enable, setEnable] = useState(
    () =>
      initialData?.mappingsEnable &&
      initialData?.mappings?.some((m) => m.type === MappingTypeEnum.FROM_ENTITY)
  );
  const onDataMappingChange = useCallback((mappings?: IntentMapping[]) => {
    setEnable(
      !!mappings?.some(
        (m) => m.type === MappingTypeEnum.FROM_ENTITY && !!m.slotId
      )
    );
  }, []);
  return [enable, onDataMappingChange, setEnable] as const;
};

/// 是否开启提取
export const useMappingsEnable = (
  initialData: IntentNextData,
  onEnabledMapping?: () => void
) => {
  const ref = useRef(onEnabledMapping);
  const [enable, setEnable] = useState(!!initialData?.mappingsEnable);
  const onEnableChange = useCallback((value: boolean) => {
    setEnable(value);
    if (value && ref.current) ref.current.call(undefined);
  }, []);
  return [enable, onEnableChange] as const;
};

export const useInitialIntentForm = (initialData: IntentNextData) => {
  const initialValues = useMemo<any>(() => {
    if (!initialData) return { display: 'user_input' };
    const clone = cloneDeep([...initialData?.examples]);
    const mainExample = clone.shift();
    const value = {
      ...initialData,
      display: initialData?.display || 'user_input',
      examples: clone,
      mainExample,
    };
    return value;
  }, [initialData]);

  const formRef = useFormRef(initialValues);
  return [initialValues, formRef] as const;
};

export const useInitialEntities = (initialData: IntentNextData) => {
  const [entities, setEntities] = useState(
    () =>
      initialData?.mappings?.filter(
        (m) => m.type === MappingTypeEnum.FROM_ENTITY
      ) || []
  );
  return [entities, setEntities] as const;
};

export const useIsGlobalIntent = (value: Relations['linkedFrom']) =>
  useMemo(() => !isEmpty(value), [value]);
