import React, { useEffect, useRef, useState } from 'react';
import {
  Button,
  Card,
  Drawer,
  FormInstance,
  Message,
  Space,
} from '@arco-design/web-react';
import { IconClose, IconSave } from '@arco-design/web-react/icon';
import { create, update } from '@/api/synonyms';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import Form from './Form';

const defaultData = {
  enable: true,
  original: '',
  synonyms: [],
  labels: [],
  description: '',
};

const transform: (data: any, initialValue: any) => any = (
  data,
  initialValue = {}
) => {
  return {
    id: void 0,
    ...initialValue,

    type: 'synonym',
    data: {
      ...(initialValue?.data || {}),
      labels: data.labels || [],
      original: data.original || '',
      synonyms: data.synonyms || [],
      enable: true,
      description: data.description || '',
    },
  };
};

const defaultInitialValue = {};

function DrawerForm({
  callback,
  Trigger,
  initialValue = defaultInitialValue,
  projectId,
}: {
  callback: () => void;
  Trigger: React.ReactElement;
  projectId: string;
  initialValue?: any;
}) {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const [data, setData] = useState<any>(defaultData);
  const synonymForm = useRef<FormInstance>();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialValue) {
      const synonym = initialValue;
      const enable = synonym?.data?.enable || true;
      const original = synonym?.data?.original || '';
      const labels = synonym?.data?.labels || [];
      const synonyms = synonym?.data?.synonyms || [];
      const description = synonym?.data?.description || '';

      setData({
        projectId,
        enable,
        original,
        synonyms,
        labels,
        description,
      });
    }
  }, [initialValue, projectId]);

  const submit = () => {
    let params = {};
    const isCreate = !initialValue.id;
    params = transform(data, initialValue);
    synonymForm.current.validate().then(() => {
      setLoading(true);
      const action = isCreate ? create : update;

      action({
        ...params,
        projectId,
      })
        .then(() => {
          setVisible(false);
          Message.success(
            isCreate ? t['synonyms.createSuccess'] : t['synonyms.updateSuccess']
          );
          synonymForm.current.resetFields();
          setData(defaultData);
          callback();
        })
        .finally(() => setLoading(false));
    });
  };

  const onClose = () => {
    if (initialValue) {
      setVisible(false);
      return;
    }
    synonymForm.current.resetFields();
    setData(defaultData);
    setVisible(false);
  };

  return (
    <>
      {Trigger &&
        React.cloneElement(Trigger, { onClick: () => setVisible(true) })}
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
            initialValue.id
              ? t['synonyms.form.update.title']
              : t['synonyms.form.add.title']
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
                onClick={submit}
              >
                {t['synonyms.save']}
              </Button>
              <Button icon={<IconClose />} onClick={onClose}>
                {t['synonyms.close']}
              </Button>
            </Space>
          }
        >
          <div style={{ height: 'calc(100vh - 100px)', overflowY: 'auto' }}>
            <Form formRef={synonymForm} data={data} setData={setData} />
          </div>
        </Card>
      </Drawer>
    </>
  );
}

export default DrawerForm;
