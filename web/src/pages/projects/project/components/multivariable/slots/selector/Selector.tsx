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
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import i18n from './locale';
import { useSlotsContext } from '../context';
import { SelecterProps } from './types';
import { keyBy } from 'lodash';

const Selector = ({
  value,
  onChange,
  onCreate,
  onRemove,
  error,
  disabled,
  disabledCreate,
  size = 'default',
  selectedKeys,
  hiddenCreator,
  hiddenKeys,
}: SelecterProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();

  const { loading, slots, operating, create } = useSlotsContext();

  const selectedMap = useMemo(
    () => keyBy(selectedKeys || [], (s) => s),
    [selectedKeys]
  );
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
          if (valueRef.current) {
            const name = valueRef.current;
            create(name).then((slot) => {
              Message.success(
                t['conversation.intentForm.mapping.create.slot.success']
              );
              setVisible(false);
              setPopupVisible(false);
              onCreate?.(
                { slotId: slot.id, slotName: name, slotDisplay: name },
                slot
              );
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
  }, [t]);

  const onOk = () => {
    form.validate().then(({ name }) => {
      create(name).then((slot) => {
        Message.success(
          t['conversation.intentForm.mapping.create.slot.success']
        );
        setVisible(false);
        onCreate?.(
          { slotId: slot.id, slotName: name, slotDisplay: name },
          slot
        );
      });
    });
  };
  const options = useMemo(
    () =>
      slots
        ?.filter((s) => !hiddenKeys?.includes(s.id))
        .map(({ id, display, name, blnInternal }) => ({
          id,
          name,
          display,
          blnInternal,
        })) || [],
    [slots, hiddenKeys]
  );

  return (
    <div className="flex justify-between">
      <Select
        ref={ref}
        // prefix={`${t['conversation.slotSelect.prefix']}:`}
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
        {options.map(({ id, display, name, blnInternal }) => (
          <Select.Option
            key={id}
            value={id}
            extra={display}
            disabled={!!selectedMap[id]}
          >
            {display || name}{' '}
            {blnInternal && (
              <Tag color="blue">{t['conversation.slotSelect.builtin']}</Tag>
            )}
          </Select.Option>
        ))}
      </Select>
      {!hiddenCreator && !disabled && !disabledCreate && (
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
          <Button
            size={size}
            type="outline"
            status="danger"
            onClick={() => onRemove()}
            style={{ marginLeft: 8 }}
          >
            <IconDelete />
          </Button>
          <Modal
            title={t['conversation.slotSelect.modalTitle']}
            visible={visible}
            onCancel={() => setVisible(false)}
            onOk={onOk}
            confirmLoading={operating}
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

export default Selector;
