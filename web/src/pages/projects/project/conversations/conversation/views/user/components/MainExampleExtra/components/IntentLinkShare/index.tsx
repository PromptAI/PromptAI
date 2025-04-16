import { listOfGlobalIntent } from '@/api/global-component';
import { GlobalIntent, IDName, IntentNextData } from '@/graph-next/type';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import { Button, Form, Modal, Select } from '@arco-design/web-react';
import { IconThunderbolt } from '@arco-design/web-react/icon';
import { useUpdateEffect } from 'ahooks';
import { cloneDeep, keyBy } from 'lodash';
import React, { useState } from 'react';
import { useParams } from 'react-router';
import i18n from './i18n';

interface IntentShareProps {
  nodeId: string;
  share?: IDName;
  onChange?: (v: IntentNextData, linkedFrom: IDName) => void;
}

const IntentLinkShare = ({ share, onChange }: IntentShareProps) => {
  const t = useLocale(i18n);
  const { id: projectId } = useParams<{
    id: string;
  }>();
  const [visible, setVisible, form] = useModalForm(share);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<GlobalIntent[]>([]);
  useUpdateEffect(() => {
    if (visible) {
      setLoading(true);
      listOfGlobalIntent(projectId)
        .then((values) => {
          setData(values);
        })
        .finally(() => setLoading(false));
    }
  }, [visible, projectId]);
  const rules = useRules();

  const handleLink = async () => {
    const { id } = await form.validate();
    const intentMap = keyBy(data, 'id');
    const targetGlobalIntent = intentMap[id];
    const cloneData = cloneDeep(targetGlobalIntent.data);
    onChange(cloneData, { id, name: targetGlobalIntent.data.name });
    setVisible(false);
  };
  return (
    <>
      <Button
        size="mini"
        type="text"
        icon={<IconThunderbolt />}
        onClick={() => setVisible(true)}
      >
        {t['label']}
      </Button>
      <Modal
        title={t['label']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleLink}
        unmountOnExit
      >
        <Form form={form} initialValues={share} layout="vertical">
          <Form.Item label={t['modal.form.id']} field="id" rules={rules}>
            <Select
              loading={loading}
              placeholder={t['modal.form.id.placeholder']}
              showSearch
              allowClear
              filterOption={(inputValue, option) =>
                option.props.extra
                  ?.toLowerCase()
                  .indexOf(inputValue.toLowerCase()) >= 0
              }
            >
              {data?.map(({ data: { name }, id }) => (
                <Select.Option key={id} value={id} extra={name}>
                  {name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default IntentLinkShare;
