import { createSlotComponent, updateSlotComponent } from '@/api/components';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import { useProjectContext } from '@/layout/project-layout/context';
import useLocale from '@/utils/useLocale';
import { Button, Form, Input, Message, Modal } from '@arco-design/web-react';
import { IconEdit, IconPlus } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from '../../locale';
import SlotTypeFormItem from '../../../components/SlotTypeFormItem';

interface SlotModalProps {
  initialValues?: any;
  mode?: 'add' | 'edit';
  disabled?: boolean;
  onSuccess?: () => void;
}
const SlotModal = ({
  initialValues,
  mode,
  disabled,
  onSuccess,
}: SlotModalProps) => {
  const { id: projectId } = useProjectContext();
  const t = useLocale(i18n);
  const [visible, setVisible, form] = useModalForm(initialValues);
  const rules = useRules();
  const handleOk = async () => {
    const values = await form.validate();
    const display = values.name;
    try {
      if (mode !== 'edit') {
        await createSlotComponent(projectId, {
          type: 'any',
          influenceConversation: false,
          display: display,
          ...values,
        });
      } else {
        await updateSlotComponent(projectId, initialValues?.id, {
          ...initialValues,
          display: display,
          ...values,
        });
      }
    } catch (e) {
      return Promise.reject();
    }
    Message.success(t['slot.success']);
    onSuccess?.();
    setVisible(false);
    return Promise.resolve();
  };
  return (
    <div>
      <Button
        type="text"
        size={mode === 'edit' ? 'mini' : 'default'}
        icon={mode === 'edit' ? <IconEdit /> : <IconPlus />}
        onClick={() => setVisible(true)}
        disabled={disabled}
      >
        {mode !== 'edit' && t['slot.table.add']}
      </Button>
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        title={t[`slot.dialog.${mode || 'add'}`]}
        onOk={handleOk}
        unmountOnExit
        style={{ width: '45%' }}
      >
        <Form form={form} layout="vertical" initialValues={initialValues}>
          <Form.Item label={t['slot.form.name']} field="name" rules={rules}>
            <Input placeholder={t['slot.form.name.placeholder']} />
          </Form.Item>
          <Form.Item label={t['slot.form.description']} field="description" rules={rules}>
            <Input placeholder={t['slot.form.description.placeholder']} />
          </Form.Item>
          {/* hide this
           <Form.Item
            label={t['slot.form.display']}
            field="display"
            rules={rules}
          >
            <Input placeholder={t['slot.form.display.placeholder']} />
          </Form.Item>
          */}
          <SlotTypeFormItem fieldType="slotType" />
        </Form>
      </Modal>
    </div>
  );
};

export default SlotModal;
