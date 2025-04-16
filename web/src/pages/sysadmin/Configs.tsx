import { getConfig, updateConfigItem } from '@/api/config';
import {
  Button,
  Card,
  Form,
  FormInstance,
  Message,
  RulesProps,
  Space,
  Spin,
  Switch,
  SwitchProps,
} from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React, { MutableRefObject, useMemo, useRef, useState } from 'react';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import useRules from '@/hooks/useRules';

type ConfigItemHelper = {
  key: string;
  formKey: string;
  triggerPropName?: string;
  label: string;
  type: 'switch';
  rules: RulesProps[];
  props: SwitchProps;
};

const components = {
  switch: <Switch />,
};

const UpdateButton = ({
  item,
  refresh,
  formRef,
}: {
  item: ConfigItemHelper;
  refresh: () => void;
  formRef: MutableRefObject<FormInstance>;
}) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  const update = () => {
    formRef.current.validate([item.formKey]).then((values) => {
      setLoading(true);
      updateConfigItem({
        name: item.key,
        value: JSON.stringify(values[item.formKey]),
      })
        .then(() => {
          Message.success('success');
          refresh();
        })
        .finally(() => setLoading(false));
    });
  };
  return (
    <Button type="primary" loading={loading} onClick={update}>
      {t['save']}
    </Button>
  );
};

const Configs = () => {
  const t = useLocale(i18n);
  const formRef = useRef<FormInstance>();
  const rules = useRules();
  const { loading, data, refresh } = useRequest(() => getConfig());
  const items: ConfigItemHelper[] = useMemo(
    () => [
      {
        key: 'trial.apply.need.audit',
        formKey: 'trialApply',
        triggerPropName: 'checked',
        label: t['sysadmin.config.trialApply'],
        type: 'switch',
        rules,
        props: {
          checkedText: t['sysadmin.config.trialApply.yes'],
          uncheckedText: t['sysadmin.config.trialApply.no'],
        },
      },
    ],
    [t, rules]
  );
  const configs = useMemo(
    () =>
      data
        ?.filter((d) => items.some((i) => i.key === d.name))
        .reduce(
          (p, c) => ({
            ...p,
            [items.find((i) => i.key === c.name).formKey]: JSON.parse(c.value),
          }),
          {}
        ),
    [data, items]
  );
  if (loading) return <Spin loading />;
  return (
    <Card size="small" bordered>
      <Form
        ref={formRef}
        layout="vertical"
        initialValues={configs}
        style={{ margin: '0 auto', maxWidth: '1024px' }}
      >
        {items.map((item) => (
          <Space
            key={item.key}
            className="w-full justify-between"
            align="start"
          >
            <Form.Item
              field={item.formKey}
              label={item.label}
              rules={item.rules}
              triggerPropName={item.triggerPropName || 'value'}
            >
              {React.cloneElement(components[item.type], item.props)}
            </Form.Item>
            <UpdateButton item={item} refresh={refresh} formRef={formRef} />
          </Space>
        ))}
      </Form>
    </Card>
  );
};

export default Configs;
