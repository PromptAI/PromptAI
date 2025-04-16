import i18n from './locale';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Form,
  RulesProps,
  Space,
  Spin,
  Typography,
} from '@arco-design/web-react';
import { isEmpty, uniq, uniqBy } from 'lodash';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import Mapping from '../Mapping';
import { MultiMappingsProps } from './types';
import { useSlotsContext } from '../slots';
import { IconClose, IconPlus } from '@arco-design/web-react/icon';
import { nanoid } from 'nanoid';
import { IntentMapping } from '@/graph-next/type';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { MappingType } from '../Mapping/types';
import { CreateMappingSlot } from '../slots/selector/types';

const useTranslateRules = () => {
  const t = useLocale(i18n);
  const mappingRules: RulesProps[] = useMemo(
    () => [
      {
        validator(value, callback) {
          if (isEmpty(value?.slotId)) {
            callback(t['conversation.intentForm.mappingRules.emptySlot']);
            return;
          }
          if (isEmpty(value?.type)) {
            callback(t['conversation.intentForm.mappingRules.emptyType']);
            return;
          }
          if (value?.type === 'from_intent' && isEmpty(value?.value)) {
            callback(t['conversation.intentForm.mappingRules.empty']);
            return;
          }
        },
      },
    ],
    [t]
  );
  return [t, mappingRules];
};
const createMapping = (): IntentMapping => ({
  id: nanoid(),
  slotId: null,
  slotName: null,
  slotDisplay: null,
  type: null,
  enable: false,
  multiInput: false,
});
const uniqMappings = (mappings: IntentMapping[]) =>
  uniqBy(mappings, 'slotId').filter(Boolean);

const MultiMappings: React.FC<MultiMappingsProps> = ({
  fields,
  operation: { add, remove },
  disabled,
  disabledSlot,
  onFromEntityMappingsChange,
  formRef,
  initialMappings,
  multiple,
  partType,
}) => {
  const [t, mappingsRules] = useTranslateRules();
  const { loading } = useSlotsContext();

  const [selectedKeys, setSelectedKeys] = useState(() =>
    initialMappings.map((m) => m.slotId)
  );

  const fromEntities = useRef<IntentMapping[]>(
    initialMappings.filter((m) => m.type === 'from_entity')
  );
  /// 创建时
  const onCreateMapping = useCallback(() => {
    const mapping = createMapping();
    if (mapping.type === 'from_entity' && onFromEntityMappingsChange) {
      fromEntities.current = uniqMappings(
        ObjectArrayHelper.add(fromEntities.current, mapping)
      );
      onFromEntityMappingsChange(fromEntities.current);
    }
    add(mapping);
  }, [add, onFromEntityMappingsChange]);
  // 移除时
  const onRemoveMapping = useCallback(
    (fieldKey: string, index: number) => {
      const removeMapping = formRef.current.getFieldValue(fieldKey);
      if (onFromEntityMappingsChange) {
        if (removeMapping && removeMapping.type === 'from_entity') {
          fromEntities.current = uniqMappings(
            ObjectArrayHelper.del(
              fromEntities.current,
              (item) => item.id === removeMapping.id
            )
          );
          onFromEntityMappingsChange(fromEntities.current);
        }
      }

      if (removeMapping && removeMapping.slotId) {
        setSelectedKeys(keys => keys.filter(key => key !== removeMapping.slotId));
      }

      remove(index);
    },
    [formRef, remove, onFromEntityMappingsChange]
  );
  // 类型改变时
  const onMappingTypeChange = useCallback(
    (type: MappingType, fieldKey: string) => {
      if (onFromEntityMappingsChange) {
        const originMapping = formRef.current.getFieldValue(fieldKey);
        if (originMapping) {
          if (type === 'from_entity' && originMapping.type !== 'from_entity') {
            fromEntities.current = uniqMappings(
              ObjectArrayHelper.add(fromEntities.current, {
                ...originMapping,
                type: 'from_entity',
              })
            );
            onFromEntityMappingsChange(fromEntities.current);
          }
          if (type !== 'from_entity' && originMapping.type === 'from_entity') {
            fromEntities.current = uniqMappings(
              ObjectArrayHelper.del(
                fromEntities.current,
                (item) => item.id === originMapping.id
              )
            );
            onFromEntityMappingsChange(fromEntities.current);
          }
        }
      }
    },
    [formRef, onFromEntityMappingsChange]
  );
  // slot 变换时
  const onMappingSlotChange = useCallback(
    (slot: CreateMappingSlot, fieldKey) => {
      const originMapping = formRef.current.getFieldValue(fieldKey);
      if (onFromEntityMappingsChange) {
        if (originMapping.type === 'from_entity') {
          fromEntities.current = uniqMappings(
            ObjectArrayHelper.update(
              fromEntities.current,
              {
                ...originMapping,
                ...slot,
              },
              (item) => item.id === originMapping.id
            )
          );
          onFromEntityMappingsChange(fromEntities.current);
        }
      }
      setSelectedKeys((s) => {
        const temp = ObjectArrayHelper.del(
          s,
          (i) => i === originMapping.slotId
        );
        return uniq(ObjectArrayHelper.add(temp, slot.slotId));
      });
    },
    [formRef, onFromEntityMappingsChange]
  );
  // 创建新的slot时
  const onCreateSlot = useCallback(
    (slot: CreateMappingSlot, fieldKey: string) => {
      const originMapping = formRef.current.getFieldValue(fieldKey);
      if (originMapping) {
        const updating = { ...originMapping, ...slot };
        formRef.current.setFieldValue(fieldKey, updating);
        if (updating.type === 'from_entity' && onFromEntityMappingsChange) {
          fromEntities.current = uniqMappings(
            ObjectArrayHelper.add(fromEntities.current, updating)
          );
          onFromEntityMappingsChange(fromEntities.current);
        }
      }
      setSelectedKeys((s) => {
        const temp = ObjectArrayHelper.del(
          s,
          (i) => i === originMapping.slotId
        );
        return uniq(ObjectArrayHelper.add(temp, slot.slotId));
      });
    },
    [formRef, onFromEntityMappingsChange]
  );
  return (
    <Spin loading={loading} className="w-full">
      <Space className="w-full" direction="vertical">
        {fields.map((field, index) => (
          <Card
            key={`${field.key}-${index}`}  // 修改这里，添加索引确保唯一性
            size="small"
            headerStyle={{ padding: '0 8px' }}
            bodyStyle={{ padding: 8 }}
          >
            <Form.Item
              key={field.key}
              field={field.field}
              rules={mappingsRules}
              className="!m-0"
            >
              <Mapping
                disabled={disabled}
                disabledSlot={disabledSlot}
                onCreate={(slot) => onCreateSlot(slot, field.field)}
                onRemove={() => {
                  onRemoveMapping(field.field, index)
                }}
                onTypeChange={(type) => onMappingTypeChange(type, field.field)}
                onSlotChange={(slot) => onMappingSlotChange(slot, field.field)}
                selectedKeys={selectedKeys}
                partType={partType}
              />
            </Form.Item>
          </Card>
        ))}
        {multiple && (
          <Button
            type="outline"
            status="success"
            long
            icon={<IconPlus />}
            onClick={onCreateMapping}
          >
            {t['conversation.intentForm.mapping.add']}
          </Button>
        )}
      </Space>
    </Spin>
  );
};
export default MultiMappings;
