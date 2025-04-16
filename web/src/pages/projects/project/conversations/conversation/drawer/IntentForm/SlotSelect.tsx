import { createSlotComponent } from '@/api/components';
import { Slot } from '@/graph-next/type';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Empty,
  Form,
  Input,
  Message,
  Modal,
  Select,
  Tag,
  Typography,
} from '@arco-design/web-react';
import { SelectHandle } from '@arco-design/web-react/es/Select/interface';
import { IconPlus } from '@arco-design/web-react/icon';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import i18n from './locale';

interface SlotSelectProps {
  disabled?: boolean;
  size?: 'small' | 'mini' | 'default' | 'large';
  value?: string;
  onChange?: (val: string) => void;
  projectId: string;
  options: Slot[];
  loading: boolean;
  refreshOptions?: () => void;
  onCreated?: (value: string, name: string, display: string) => void;
  error?: boolean;
  createDisabled?: boolean;
}

const SlotSelect = ({
  disabled,
  projectId,
  size,
  options,
  loading,
  refreshOptions,
  value,
  onChange,
  onCreated,
  error,
  createDisabled,
}: SlotSelectProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();

  const [resLoading, setResLoading] = useState(false);
  const onOk = () => {
    form.validate().then((values) => {
      setResLoading(true);
      createSlotComponent(projectId, {
        id: undefined,
        name: values.name,
        display: values.name,
        type: 'any',
        influenceConversation: false,
        mappings: [],
      })
        .then(([{ id, name, display }]) => {
          refreshOptions?.();
          setVisible(false);
          onCreated?.(id, name, display);
          Message.success(
            t['conversation.intentForm.mapping.create.slot.success']
          );
        })
        .finally(() => {
          setResLoading(false);
        });
    });
  };
  const ref = useRef<SelectHandle>();
  const valueRef = useRef(value);
  const onInputValueChange = useCallback((val) => {
    valueRef.current = val;
  }, []);
  const [popupVisible, setPopupVisible] = useState(false);
  useEffect(() => {
    const dom = ref.current.dom;
    const hanlder = (evt) => {
      if (evt.key === 'Enter') {
        if (!ref.current.activeOptionValue) {
          // not create slot
          if (valueRef.current && !resLoading) {
            const name = valueRef.current;
            setResLoading(true);
            createSlotComponent(projectId, {
              id: undefined,
              name,
              display: name,
              type: 'any',
              influenceConversation: false,
              mappings: [],
            })
              .then(([{ id, name, display }]) => {
                refreshOptions?.();
                setVisible(false);
                onCreated?.(id, name, display);
                setPopupVisible(false);
                Message.success(
                  t['conversation.intentForm.mapping.create.slot.success']
                );
              })
              .finally(() => {
                setResLoading(false);
              });
          }
        }
      }
    };
    dom.addEventListener('keydown', hanlder);
    return () => {
      dom.removeEventListener('keydown', hanlder);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t, resLoading]);

  return (
    <div className="flex justify-between">
      <Select
        ref={ref}
        prefix={`${t['conversation.slotSelect.prefix']}:`}
        disabled={disabled}
        loading={loading}
        value={value}
        size={size}
        onChange={onChange}
        placeholder={t['conversation.slotSelect.placeholder']}
        error={error}
        showSearch
        allowClear
        filterOption={(inputValue, option) =>
          option.props.extra.toLowerCase().indexOf(inputValue.toLowerCase()) >=
          0
        }
        onInputValueChange={onInputValueChange}
        popupVisible={popupVisible}
        onVisibleChange={setPopupVisible}
        notFoundContent={
          <Empty
            description={
              <Typography.Text>
                {t['conversation.slotSelect.empty.create.help']}
              </Typography.Text>
            }
          />
        }
      >
        {options?.map(({ id, display, name, blnInternal }) => (
          <Select.Option key={id} value={id} extra={display}>
            {display || name}{' '}
            {blnInternal && (
              <Tag color="blue">{t['conversation.slotSelect.builtin']}</Tag>
            )}
          </Select.Option>
        ))}
      </Select>
      {!disabled && !createDisabled && (
        <>
          <Button
            size={size}
            type="outline"
            status="success"
            onClick={() => setVisible(true)}
            style={{ marginLeft: 8 }}
          >
            <IconPlus />
          </Button>
          <Modal
            title={t['conversation.slotSelect.modalTitle']}
            visible={visible}
            onCancel={() => setVisible(false)}
            onOk={onOk}
            confirmLoading={resLoading}
            unmountOnExit
          >
            <Form form={form} layout="vertical">
              <Form.Item
                label={t['conversation.slotSelect.name']}
                field="name"
                rules={rules}
              >
                <Input
                  autoFocus
                  placeholder={t['conversation.slotSelect.name.placeholder']}
                  onPressEnter={onOk}
                />
              </Form.Item>
            </Form>
          </Modal>
        </>
      )}
    </div>
  );
};

export default SlotSelect;
