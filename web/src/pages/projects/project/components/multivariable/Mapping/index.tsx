import useLocale from '@/utils/useLocale';
import { Checkbox, Select, Space, Typography } from '@arco-design/web-react';
import React, { useState, useMemo, useEffect } from 'react';
import i18n from './locale';
import { isEmpty } from 'lodash';
import { MappingProps, MappingType } from './types';
import BlurInput from '@/components/BlurInput';
import { Selector, useSlotsContext } from '../slots';

const MAPPING_TYPE_VALUES: MappingType[] = [
  'from_text',
  'from_entity',
  'from_intent'
];
const MAPPING_PART_TYPE_VALUES: MappingType[] = ['from_entity', 'from_intent'];

const Mapping = ({
                   value,
                   onChange,
                   disabled,
                   disabledSlot,
                   onTypeChange,
                   onSlotChange,
                   onCreate,
                   onRemove,
                   selectedKeys,
                   partType
                 }: MappingProps) => {
  const t = useLocale(i18n);

  // 默认值为 'from_entity'，如果 value.type 未定义则设置为默认值
  const [type, setType] = useState<MappingType>(value?.type || 'from_entity');

  const { map } = useSlotsContext();
  const haveDataset = useMemo(
    () => !isEmpty((map[value?.slotId]?.data as any)?.dictionary),
    [map, value?.slotId]
  );

  useEffect(() => {
    if (!value?.type) {
      onChange({ ...value, type: 'from_entity' });
    }
  }, [value, onChange]);

  const onChangeSlotSelect = (val: string) => {
    const slot = {
      slotId: val,
      slotName: map[val].name,
      slotDisplay: map[val].display
    };
    onSlotChange?.(slot);
    onChange({
      ...value,
      ...slot
    });
  };
  const onChangeType = (newType: MappingType) => {
    setType(newType);
    onTypeChange(newType);
    onChange({ ...value, type: newType });
  };
  const onChangeValue = (val: string) => {
    onChange({ ...value, value: val });
  };
  const onChangeMultiInput = (checked: boolean) => {
    onChange({ ...value, multiInput: checked });
  };
  const typeOptions = useMemo(
    () => (partType ? MAPPING_PART_TYPE_VALUES : MAPPING_TYPE_VALUES),
    [partType]
  );

  return (
    <Space direction="vertical" className="w-full">
      <Selector
        value={value.slotId}
        onChange={onChangeSlotSelect}
        onCreate={onCreate}
        onRemove={onRemove}
        disabled={disabledSlot || disabled}
        selectedKeys={selectedKeys}
      />
      {/*<Select*/}
      {/*  prefix={`${t['mapping.select']}:`}*/}
      {/*  disabled={disabled}*/}
      {/*  placeholder={t['mapping.select.placeholder']}*/}
      {/*  value={type}*/}
      {/*  onChange={onChangeType}*/}
      {/*>*/}
      {/*  {typeOptions.map((key) => (*/}
      {/*    <Select.Option key={key} value={key}>*/}
      {/*      {t[`mapping.select.${key}`]}*/}
      {/*    </Select.Option>*/}
      {/*  ))}*/}
      {/*</Select>*/}
      {/*{type === 'from_intent' && (*/}
      {/*  <BlurInput*/}
      {/*    prefix={t['mapping.from.intent.value']}*/}
      {/*    disabled={disabled}*/}
      {/*    placeholder={t['mapping.from.intent.value']}*/}
      {/*    value={value.value}*/}
      {/*    onChange={onChangeValue}*/}
      {/*  />*/}
      {/*)}*/}
      {/*{type === 'from_text' && (*/}
      {/*  <Space>*/}
      {/*    <Typography.Text>{t['mapping.from.text.multiInput']}</Typography.Text>*/}
      {/*    <Checkbox checked={value.multiInput} onChange={onChangeMultiInput} />*/}
      {/*  </Space>*/}
      {/*)}*/}
      {/*{type === 'from_entity' && haveDataset && (*/}
      {/*  <Typography.Text type="warning" style={{ fontSize: 12 }}>*/}
      {/*    {t['mapping.from.entity.hotwords.tip']}*/}
      {/*  </Typography.Text>*/}
      {/*)}*/}
    </Space>
  );
};

export default Mapping;
