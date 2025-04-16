import * as React from 'react';
import {
  Button,
  Card,
  Drawer,
  Message,
  Space,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import { IconClose, IconEdit, IconSave } from '@arco-design/web-react/icon';
import { updateFaq } from '@/api/faq';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import useUrlParams from '../hooks/useUrlParams';
import useModalForm from '@/hooks/useModalForm';
import { useRequest } from 'ahooks';
import GptForm from "@/pages/projects/project/gpt/GptFrom";
import {updateComponent} from "@/api/components";

const wrapValues = (values, initialValues) => {
  return {
    ...initialValues,
    data: {
      description: values.description,
      prompt: values.prompt,
      name: values.name ,
    },
  };
};

interface UpdateSampleProps {
  onSuccess?: (res: any) => void;
  row: any;
}
const UpdateGpt: React.FC<UpdateSampleProps> = (props) => {
  const { row, onSuccess } = props;
  const { projectId } = useUrlParams();
  const t = useLocale(i18n);

  const [visible, setVisible, formInstance] = useModalForm();

  const {loading, runAsync: submit} = useRequest(
      (params) => updateComponent(projectId, params.type, params.id,params),
      {
        manual: true,
        refreshDeps: [projectId],
        onSuccess,
      }
  );

  const onSubmit = async () => {
    const values = await formInstance.validate();

    const params = wrapValues(values, row);
    await submit(params);
    Message.success(t['sample.updateSuccess']);
    setVisible(false);
  };
  return (
    <>
      <Tooltip content={t['sample.edit']}>
        <Button
          size="small"
          onClick={() => setVisible(true)}
          type="text"
          icon={<IconEdit />}
        />
      </Tooltip>
      <Drawer
        width={540}
        height={'calc(100vh - 51px'}
        bodyStyle={{ padding: 0 }}
        headerStyle={{ display: 'none' }}
        visible={visible}
        maskStyle={{ opacity: 0.1, cursor: 'not-allowed' }}
        maskClosable={false}
        escToExit={false}
        closable={false}
        footer={null}
        unmountOnExit
      >
        <Card
          title={
            <Space>
              {t['sample.updateTitle']}
            </Space>
          }
          style={{ height: '100%', background: 'var(--color-bg-1)' }}
          bordered={false}
          headerStyle={{
            borderBottom: '1px solid #ccc',
            padding: '0 10px',
            height: 51,
          }}
          extra={
            <Space>
              <Button
                type="primary"
                icon={<IconSave />}
                loading={loading}
                onClick={onSubmit}
              >
                {t['gpt.save']}
              </Button>
              <Button icon={<IconClose />} onClick={() => setVisible(false)}>
                {t['gpt.close']}
              </Button>
            </Space>
          }
        >
          <div style={{ height: 'calc(100vh - 100px)', overflowY: 'auto' }}>
            <GptForm form={formInstance} initialValues={row} />
          </div>
        </Card>
      </Drawer>
    </>
  );
};

export default UpdateGpt;
