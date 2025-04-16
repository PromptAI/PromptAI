import { createGlobalIntent } from '@/api/global-component';
import { IDName, IntentNextData } from '@/graph-next/type';
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
import React, { MutableRefObject, useRef, useState } from 'react';
import { useParams } from 'react-router';
import i18n from './i18n';

interface ShareLinkIntentProps {
  share?: IDName;
  onChange: (data: IntentNextData, share: IDName) => void;
  valueFormRef: MutableRefObject<FormInstance<any>>;
  nodeId: string;
}

const ShareLinkIntent = ({
  share,
  onChange,
  valueFormRef,
  nodeId,
}: ShareLinkIntentProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const rules = useRules();
  const formRef = useRef<FormInstance>();
  const { id: projectId, cId: rootComponentId } = useParams<{
    id: string;
    cId: string;
  }>();
  const handleOk = async () => {
    const { name } = await formRef.current.validate();
    const { mainExample, examples, ...ret } =
      await valueFormRef.current.validate();
    const data = {
      ...ret,
      name,
      examples: [mainExample, ...(examples || [])],
    };
    // create global intent, and global intent name is unique
    const [{ id }] = await createGlobalIntent(projectId, {
      id: null,
      type: 'user-global',
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
        {t['label']}
      </Button>
      <Modal
        title={t['label']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleOk}
        unmountOnExit
      >
        <Form ref={formRef} initialValues={share} layout="vertical">
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

export default ShareLinkIntent;
