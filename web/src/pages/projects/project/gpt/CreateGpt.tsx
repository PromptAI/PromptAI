import * as React from 'react';
import {Button, Card, Drawer, Message, Space} from '@arco-design/web-react';
import {IconClose, IconSave} from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import useUrlParams from '../hooks/useUrlParams';
import useModalForm from '@/hooks/useModalForm';
import {useRequest} from 'ahooks';
import GptForm from "./GptFrom";
import {createComponent} from "@/api/components";

const WrapValues = (values) => {
  const slots = values.mappings?.map((item) => ({id: item.slotId}))
  return {
    type: 'agent',
    data: {
      description: values.description,
      prompt: values.prompt,
      name: values.name,
      slots: slots,
    },
  };
};

const defaultValues = {
  name: '',
  prompt: '',
  description:''
};

interface CreateSampleProps {
  onSuccess?: (res: any) => void;
  trigger: React.ReactElement;
  row?: any;
}
const CreateGpt: React.FC<CreateSampleProps> = (props) => {
  const { trigger, onSuccess, row } = props;

  const t = useLocale(i18n);

  const { projectId } = useUrlParams();

  const [visible, setVisible, formInstance] = useModalForm();

  const {loading, runAsync: submit} = useRequest(
      (params) => createComponent(projectId,"agent",params),
      {
        manual: true,
        refreshDeps: [projectId],
        onSuccess,
      }
  );

  const initialValues = React.useMemo(
    () =>
      !row
        ? {data:defaultValues}
        : row,
    [row]
  );

  const onSubmit = async () => {
    const values = await formInstance.validate();
    console.log("submit:", JSON.stringify(values))
    const params = WrapValues(values);
    await submit(params);
    Message.success(t['gpt.create.success']);
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
          title={t['gpt.create.title']}
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
            <GptForm form={formInstance} initialValues={initialValues} />
          </div>
        </Card>
      </Drawer>
    </React.Fragment>
  );
};

export default CreateGpt;
