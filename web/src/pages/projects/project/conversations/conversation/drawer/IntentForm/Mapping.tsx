import { IntentMapping } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import { Select, Space, Typography } from '@arco-design/web-react';
import React, { useState, useMemo } from 'react';
import { useParams } from 'react-router';
import i18n from '../locale';
import SlotSelect from './SlotSelect';
import useSlots from './useSlots';
import EnterBlurInput from '@/pages/projects/project/components/EnterBlurInput';
import { isEmpty } from 'lodash';

interface MappingProps {
  value?: IntentMapping;
  onChange?: (value: IntentMapping) => void;
  onSlotChange?: (id: string, name: string, display: string) => void;
  onTypeChange?: (type: string) => void;
  disabled?: boolean;
  slotDisabled?: boolean;
}

const Mapping = (props: MappingProps) => {
  const t = useLocale(i18n);
  const {
    value,
    onChange,
    disabled,
    slotDisabled,
    onTypeChange,
    onSlotChange,
  } = props;
  const { id: projectId } = useParams<{ id: string }>();
  const { loading, slotMap, slots, refresh } = useSlots(projectId);

  const [type, setType] = useState(value?.type);
  const haveDataset = useMemo(
    () => !isEmpty((slotMap[value?.slotId]?.data as any)?.dictionary),
    [slotMap, value?.slotId]
  );

  const onChangeSlotSelect = (val: string) => {
    onChange({
      ...value,
      slotId: val,
      slotName: slotMap[val].name,
      slotDisplay: slotMap[val].display,
    });
  };
  const onChangeType = (type) => {
    setType(type);
    onTypeChange(type);
    onChange({ ...value, type });
  };
  const onChangeValue = (val) => {
    onChange({ ...value, value: val });
  };
  const onSlotCreated = (id, name, display) => {
    onSlotChange(id, name, display);
  };
  return (
    <Space direction="vertical" className="w-full">
      <SlotSelect
        value={value.slotId}
        onChange={onChangeSlotSelect}
        onCreated={onSlotCreated}
        projectId={projectId}
        loading={loading}
        refreshOptions={refresh}
        options={slots}
        disabled={slotDisabled || disabled}
      />
      <Select
        prefix={`${t['Mapping.Select']}:`}
        disabled={disabled}
        placeholder={t['Mapping.Select.placeholder']}
        value={value.type}
        onChange={onChangeType}
      >
        <Select.Option value="from_text">
          {t['Mapping.Select.from_text']}
        </Select.Option>
        <Select.Option value="from_entity">
          {t['Mapping.Select.from_entity']}
        </Select.Option>
        <Select.Option value="from_intent">
          {t['Mapping.Select.from_intent']}
        </Select.Option>
      </Select>
      {type === 'from_intent' && (
        <EnterBlurInput
          prefix={t['Mapping.Select.input']}
          disabled={disabled}
          placeholder={t['Mapping.Select.input']}
          value={value.value}
          onChange={onChangeValue}
        />
      )}
      {type === 'from_entity' && haveDataset && (
        <Typography.Text type="warning" style={{ fontSize: 12 }}>
          {t['hotWords.tip']}
        </Typography.Text>
      )}
    </Space>
  );
};

export default Mapping;
