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
import FaqForm from './FaqFrom';
import useUrlParams from '../hooks/useUrlParams';
import useModalForm from '@/hooks/useModalForm';
import { useRequest } from 'ahooks';

const wrapvValues = (values, initialValues) => {
  const user = {
    ...initialValues.user,
    type: 'user',
    data: {
      ...initialValues.user.data,
      examples: [
        { text: values.mainExample, marks: [] },
        ...(values.examples || []),
      ],
      description: values.description,
      labels: values.labels,
      name: values.name || values.mainExample,
    },
  };
  const bot = {
    ...initialValues.bot,
    type: 'bot',
    data: {
      responses: values.responses,
    },
  };
  return { user, bot };
};

interface UpdateSampleProps {
  onSuccess?: (res: any) => void;
  row: any;
}
const UpdateSample: React.FC<UpdateSampleProps> = (props) => {
  const { row, onSuccess } = props;

  const { projectId } = useUrlParams();
  const t = useLocale(i18n);

  const [visible, setVisible, formInstance] = useModalForm();

  const { loading, runAsync: submit } = useRequest(
    (params) => updateFaq({ projectId, ...params }),
    {
      manual: true,
      refreshDeps: [projectId],
      onSuccess,
    }
  );
  const initialValues = React.useMemo(
    () => ({
      ...row.user.data,
      responses: row.bot.data.responses || [],
    }),
    [row]
  );

  const error = React.useMemo(() => {
    const validatorError = row.user.validatorError;
    return {
      error:
        !!validatorError &&
        validatorError.errorCode !== undefined &&
        validatorError.errorCode !== 0,
      message: validatorError?.errorMessage,
    };
  }, [row]);

  const onSubmit = async () => {
    const values = await formInstance.validate();

    const params = wrapvValues(values, row);
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
              {error.error && (
                <Typography.Text
                  type="warning"
                  ellipsis={{ showTooltip: true }}
                  style={{ maxWidth: 220, margin: 0 }}
                >
                  {error.message}
                </Typography.Text>
              )}
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
    </>
  );
};

export default UpdateSample;
