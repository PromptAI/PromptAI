import { listOfGlobalBot } from '@/api/global-component';
import { IDName } from '@/graph-next/type';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import { useProjectContext } from '@/layout/project-layout/context';
import useLocale from '@/utils/useLocale';
import { Button, Form, Modal, Select } from '@arco-design/web-react';
import { IconThunderbolt } from '@arco-design/web-react/icon';
import { useUpdateEffect } from 'ahooks';
import { cloneDeep, keyBy } from 'lodash';
import React, { useState } from 'react';
import i18n from './i18n';

interface BotShareProps {
  share?: IDName;
  onChange?: (v: any, linkedFrom: IDName) => void;
}

const BotShare = ({ share, onChange }: BotShareProps) => {
  const t = useLocale(i18n);
  const { id: projectId } = useProjectContext();
  const [visible, setVisible, form] = useModalForm(share);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<any[]>([]);

  useUpdateEffect(() => {
    if (visible) {
      setLoading(true);
      listOfGlobalBot(projectId)
        .then((values) => {
          setData(values);
        })
        .finally(() => setLoading(false));
    }
  }, [visible, projectId]);
  const rules = useRules();

  const handleLink = async () => {
    const { id } = await form.validate();
    const botMap = keyBy(data, 'id');
    const target = botMap[id];
    const cloneData = cloneDeep(target.data);
    onChange(cloneData, { id, name: target.data.name });
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
        {t['modal.title']}
      </Button>
      <Modal
        title={t['modal.title']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleLink}
        unmountOnExit
      >
        <Form form={form} initialValues={share} layout="vertical">
          <Form.Item label={t['modal.name']} field="id" rules={rules}>
            <Select
              loading={loading}
              placeholder={t['modal.name.placeholder']}
              showSearch
              allowClear
              filterOption={(inputValue, option) =>
                option.props.extra
                  .toLowerCase()
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

export default BotShare;
