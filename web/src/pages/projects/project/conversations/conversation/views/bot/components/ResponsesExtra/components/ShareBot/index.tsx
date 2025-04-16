import { createGlobalBot } from '@/api/global-component';
import { IDName, IntentNextData } from '@/graph-next/type';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  FormInstance,
  Input,
  Modal,
} from '@arco-design/web-react';
import { IconFolderAdd } from '@arco-design/web-react/icon';
import React, { MutableRefObject } from 'react';
import { useParams } from 'react-router';
import i18n from './i18n';

interface ShareBotProps {
  share?: IDName;
  onChange: (data: IntentNextData, share: IDName) => void;
  valueFormRef: MutableRefObject<FormInstance<any>>;
  nodeId: string;
}

const ShareBot = ({ share, onChange, valueFormRef, nodeId }: ShareBotProps) => {
  const t = useLocale(i18n);
  const rules = useRules();
  const [visible, setVisible, form] = useModalForm();
  const { id: projectId, cId: rootComponentId } = useParams<{
    id: string;
    cId: string;
  }>();
  const handleOk = async () => {
    const { name } = await form.validate();
    const cloneData = await valueFormRef.current.validate();
    const data = {
      ...cloneData,
      name,
    };
    // create global bot, and global bot name is unique
    const [{ id }] = await createGlobalBot(projectId, {
      id: null,
      type: 'bot-global',
      data,
      componentRelation: {
        usedByComponentRoots: [
          { rootComponentId, componentId: nodeId, rootComponentType: 'flow' },
        ],
      },
    });
    onChange(data, { id, name });
    setVisible(false);
  };
  const openModal = async () => {
    await valueFormRef.current.validate();
    setVisible(true);
  };
  return (
    <>
      <Button
        size="mini"
        type="text"
        icon={<IconFolderAdd />}
        onClick={openModal}
      >
        {t['modal.title']}
      </Button>
      <Modal
        title={t['modal.title']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleOk}
        unmountOnExit
      >
        <Form form={form} initialValues={share} layout="vertical">
          <Form.Item
            label={t['modal.form.name']}
            field="name"
            rules={rules}
            labelCol={{ span: 6 }}
            wrapperCol={{ span: 18 }}
          >
            <Input autoFocus placeholder={t['modal.form.name.placeholder']} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default ShareBot;
