import * as React from 'react';
import { Button, Card, Drawer, Message, Space } from '@arco-design/web-react';
import { IconClose, IconSave } from '@arco-design/web-react/icon';
import { createFaq } from '@/api/faq';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import FaqForm from './FaqFrom';
import useUrlParams from '../hooks/useUrlParams';
import useModalForm from '@/hooks/useModalForm';
import { useRequest } from 'ahooks';

const wrapvValues = (values) => {
  const user = {
    type: 'user',
    data: {
      examples: [
        { text: values.mainExample, marks: [] },
        ...(values.examples || []),
      ],
      description: values.description,
      enable: values.enable,
      labels: values.labels,
      name: values.name || values.mainExample,
    },
  };

  const bot = {
    type: 'bot',
    data: {
      responses: values.responses,
    },
  };
  return { user, bot };
};

const defaultValues = {
  enable: true,
  responses: [],
  examples: [],
  labels: [],
  mainExample: '',
  name: '',
};

interface CreateSampleProps {
  onSuccess?: (res: any) => void;
  trigger: React.ReactElement;
  row?: any;
}
const CreateSample: React.FC<CreateSampleProps> = (props) => {
  const { trigger, onSuccess, row } = props;

  const t = useLocale(i18n);

  const { projectId } = useUrlParams();

  const [visible, setVisible, formInstance] = useModalForm();

  const { loading, runAsync: submit } = useRequest(
    (params) => createFaq({ projectId, ...params }),
    {
      manual: true,
      refreshDeps: [projectId],
      onSuccess,
    }
  );

  const initialValues = React.useMemo(
    () =>
      !row
        ? defaultValues
        : {
            ...row.user?.data,
            responses: row.bot?.data.responses || [],
          },
    [row]
  );

  const onSubmit = async () => {
    const values = await formInstance.validate();
    const params = wrapvValues(values);
    await submit(params);
    Message.success(t['sample.createSuccess']);
    setVisible(false);
  };

  const triggerWrapper = React.useMemo(
    () => React.cloneElement(trigger, { onClick: () => setVisible(true) }),
    [trigger, setVisible]
  );
  return (
    <React.Fragment>
      {triggerWrapper}
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
          title={t['sample.createTitle']}
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
                {t['sample.save']}
              </Button>
              <Button icon={<IconClose />} onClick={() => setVisible(false)}>
                {t['sample.close']}
              </Button>
            </Space>
          }
        >
          <div style={{ height: 'calc(100vh - 100px)', overflowY: 'auto' }}>
            <FaqForm form={formInstance} initialValues={initialValues} />
          </div>
        </Card>
      </Drawer>
    </React.Fragment>
  );
};

export default CreateSample;
