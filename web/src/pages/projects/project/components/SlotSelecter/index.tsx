import {
  Button,
  Empty,
  Form,
  Input,
  Message,
  Modal,
  Select,
  SelectProps,
  Tag,
} from '@arco-design/web-react';
import { SelectHandle } from '@arco-design/web-react/es/Select/interface';
import { IconPlus } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import * as React from 'react';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import useRules from '@/hooks/useRules';
import useModalForm from '@/hooks/useModalForm';
import { createSlotComponent, listSlotComponent } from '@/api/components';
import useUrlParams from '../../hooks/useUrlParams';
import { Slot } from '@/graph-next/type';
import { keyBy, uniqBy } from 'lodash';

interface SlotSelecterProps extends Omit<SelectProps, 'onChange'> {
  onChange?: (value: string | string[], options: Slot | Slot[]) => void;
  previewRes?: (res: Slot[]) => Slot & { disabled?: boolean };
}
const SlotSelecter: React.FC<SlotSelecterProps> = ({
  defaultValue,
  disabled,
  size,
  mode,
  value,
  onChange,
  previewRes,
  ...props
}) => {
  const { projectId } = useUrlParams();
  const [popupVisible, setPopupVisible] = React.useState(false);

  const t = useLocale(i18n);
  const rules = useRules();

  const [visible, setVisible, formInstance] = useModalForm();

  const ref = React.useRef<SelectHandle>();
  const valueRef = React.useRef('');
  const onInputValueChange = React.useCallback((val) => {
    valueRef.current = val;
  }, []);

  const { loading, data, refresh } = useRequest(
    () => listSlotComponent(projectId),
    {
      refreshDeps: [projectId],
    }
  );
  const slots = React.useMemo(
    () => (!data ? [] : previewRes ? previewRes(data) : data),
    [data, previewRes]
  );

  const slotMap = React.useMemo(() => keyBy(slots, 'id'), [slots]);

  const { loading: submiting, runAsync: submit } = useRequest(
    (params) => createSlotComponent(projectId, params),
    {
      manual: true,
      refreshDeps: [projectId],
    }
  );

  React.useEffect(() => {
    const dom = ref.current.dom;
    const hanlder = (evt) => {
      if (evt.key === 'Enter') {
        if (!ref.current.activeOptionValue) {
          // not create slot
          if (valueRef.current) {
            const name = valueRef.current;
            submit({ name, display: name })
              .then((response) => {
                const [result] = response;
                Message.success(t['create.slot.success']);
                refresh();
                setVisible(false);
                setPopupVisible(false);
                if (onChange) {
                  if (mode === 'multiple') {
                    const olds = (value || []) as string[];
                    const ops = uniqBy(
                      [...olds.map((s) => slotMap[s]).filter(Boolean), result],
                      'id'
                    );
                    onChange(olds, ops);
                    return;
                  }
                  if (mode == 'tags') return;
                  onChange(result.id, result);
                }
              })
              .catch(() => Message.success(t['create.slot.error']));
          }
        }
      }
    };
    dom.addEventListener('keydown', hanlder);
    return () => {
      dom.removeEventListener('keydown', hanlder);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t, onChange, mode, value, slotMap]);

  const onSubmit = async () => {
    try {
      const { name } = await formInstance.validate();
      const [result] = await submit({ name, display: name });
      Message.success(t['create.slot.success']);
      refresh();
      setVisible(false);
      if (onChange) {
        if (mode === 'multiple') {
          const olds = (value || []) as string[];
          const ops = uniqBy(
            [...olds.map((s) => slotMap[s]).filter(Boolean), result],
            'id'
          );
          onChange(olds, ops);
          return;
        }
        if (mode == 'tags') return;
        // single
        onChange(result.id, result);
      }
    } catch (error) {
      Message.success(t['create.slot.error']);
    }
  };

  const options = React.useMemo<SelectProps['options']>(
    () =>
      slots.map((props) => ({
        label: (
          <div className="flex items-center gap-2">
            <span>{props.display || props.name}</span>
            {props.blnInternal && (
              <Tag color="blue" className="!text-sm !h-fit">
                {t['builtin']}
              </Tag>
            )}
          </div>
        ),
        value: props.id,
        extra: props,
        disabled: props.disabled,
      })),
    [slots, t]
  );
  const onSelectChangeWrap = (val) => {
    if (onChange) {
      if (mode === 'multiple') {
        const selections = val as string[];
        const ops = selections.map((s) => slotMap[s]).filter(Boolean);
        onChange(selections, ops);
        return;
      }
      if (mode == 'tags') return;
      // single
      const selected = val as string;
      onChange(selected, slotMap[selected]);
    }
  };
  return (
    <div className="flex items-center gap-2 w-full">
      <Select
        {...props}
        loading={loading || submiting}
        showSearch
        allowClear
        filterOption={(inputValue, option) => {
          const label = option.props.extra.display || option.props.extra.name;
          return label.toLowerCase().indexOf(inputValue.toLowerCase()) >= 0;
        }}
        onInputValueChange={onInputValueChange}
        popupVisible={popupVisible}
        onVisibleChange={setPopupVisible}
        notFoundContent={<Empty description={t['empty.create.help']} />}
        options={options}
        defaultValue={defaultValue}
        disabled={disabled}
        size={size}
        ref={ref}
        mode={mode}
        onChange={onSelectChangeWrap}
        value={value}
      />
      <Button
        type="outline"
        status="success"
        icon={<IconPlus />}
        disabled={disabled}
        size={size}
        onClick={() => setVisible(true)}
      />
      <Modal
        title={t['form.title']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onSubmit}
        confirmLoading={submiting}
        unmountOnExit
      >
        <Form form={formInstance} layout="vertical">
          <Form.Item label={t['form.name']} field="name" rules={rules}>
            <Input
              autoFocus
              placeholder={t['form.name.placeholder']}
              onPressEnter={onSubmit}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SlotSelecter;
