import useLocale from '@/utils/useLocale';
import {
  Button,
  DatePicker,
  Divider,
  Form,
  Input,
  InputNumber,
  Message,
  Modal,
  Select,
  Space,
  Spin,
  Switch,
  Typography,
} from '@arco-design/web-react';
import React, {
  ReactNode,
  cloneElement,
  isValidElement,
  useEffect,
  useMemo,
  useState,
} from 'react';
import i18n from './locale';
import { createCycleTask, updateCycleTask } from '@/api/cycle-tasks';
import useModalForm from '@/hooks/useModalForm';
import useCycleEnums from '../../hooks/useCycleEnums';
import {
  IconDelete,
  IconPlus,
  IconQuestionCircleFill,
} from '@arco-design/web-react/icon';
import dayjs from 'dayjs';
import { listConversations, listFaqs } from '@/api/components';
import { useRequest } from 'ahooks';
import useUrlParams from '../../../hooks/useUrlParams';

const RangePicker: any = DatePicker.RangePicker;
interface CycleTaskMutateProps {
  mode: 'create' | 'update';
  initialValues?: any;
  onSuccess: () => void;
  trigger: ReactNode;
}

const FormList: any = Form.List;
const fetchComponents = async (projectId: string) => {
  const faqs = await listFaqs(projectId);
  const flows = await listConversations(projectId);
  return [...faqs, ...flows]
    .filter((f) => f.data.isReady)
    .map((f) => ({ id: f.id, name: f.data.name }));
};

const format = (values) => {
  const data = {
    ...values,
    ...(values.dateTimeRange && values.dateTimeRange.length > 0
      ? {
          startTime: dayjs(values.dateTimeRange[0]).valueOf(),
          endTime: dayjs(values.dateTimeRange[1]).valueOf(),
        }
      : {}),
    status: Number(values.status),
    interval: values.interval,
  };
  delete data.dateTimeRange;
  return data;
};
const unFormat = (values) => {
  if (!values) return {};
  const data: any = {
    ...values.properties?.data,
    ...values.properties?.param,
    type: values.type,
    status: values.status,
  };
  return {
    ...data,
    dateTimeRange: [
      ...(data.startTime ? [Number(data.startTime)] : []),
      ...(data.endTime ? [Number(data.endTime)] : []),
    ],
    status: `${data.status}`,
    interval: data.interval ? Number(data.interval) : undefined,
  };
};
const CycleTaskMutate = ({
  mode,
  initialValues,
  onSuccess,
  trigger,
}: CycleTaskMutateProps) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const formmatValues = useMemo(() => unFormat(initialValues), [initialValues]);
  const [visible, setVisible, form, rules] = useModalForm(formmatValues);
  const { modeEnum, statusEnum, trianTypeEnum } = useCycleEnums();

  const [type, setType] = useState<'train-interval' | 'train-cron'>(
    formmatValues.type
  );
  const onValuesChange = (field) => {
    if (field.type !== undefined) {
      setType(field.type);
    }
  };
  const {
    loading,
    data: options = [],
    run,
  } = useRequest(() => fetchComponents(projectId), {
    manual: true,
  });

  const [enableAll, setEnableAll] = useState(false);
  useEffect(() => visible && run(), [run, visible]);

  const onOk = async () => {
    let values = await form.validate();
    if (enableAll) {
      values.componentIds = options.map((o) => o.id);
    }
    values = format(values);
    values.projectId = projectId;
    if (mode === 'create') {
      await createCycleTask(values);
    }
    if (mode === 'update') {
      await updateCycleTask({ ...initialValues, ...values });
    }
    Message.success(t['cycle.mutate.fetch.succss']);
    onSuccess();
    setVisible(false);
  };
  return (
    <>
      {isValidElement(trigger) &&
        cloneElement(trigger, { onClick: () => setVisible(true) } as any)}
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        title={t[`cycle.mutate.title.${mode}`]}
        style={{ width: 'max-content' }}
      >
        <Form form={form} layout="vertical" onValuesChange={onValuesChange}>
          <Space>
            <Form.Item
              label={t['cycle.mutate.status']}
              field="status"
              rules={rules}
            >
              <Select allowClear style={{ width: 280 }}>
                {Object.entries(statusEnum).map(([k, v]) => (
                  <Select.Option key={k} value={k}>
                    {v}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              label={t['cycle.mutate.trainType']}
              field="trainType"
              rules={rules}
            >
              <Select allowClear style={{ width: 280 }}>
                {Object.entries(trianTypeEnum).map(([k, v]) => (
                  <Select.Option key={k} value={k}>
                    {v}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Space>
          <Space>
            <Form.Item
              label={t['cycle.mutate.mode']}
              field="type"
              rules={rules}
            >
              <Select allowClear style={{ width: 280 }}>
                {Object.entries(modeEnum).map(([k, v]) => (
                  <Select.Option key={k} value={k}>
                    {v}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            {type === 'train-interval' && (
              <Form.Item
                label={t['cycle.mutate.interval']}
                field="interval"
                rules={[
                  { required: true, min: 100, message: rules[0]?.message },
                ]}
              >
                <InputNumber min={100} style={{ width: 280 }} />
              </Form.Item>
            )}
          </Space>
          {type === 'train-cron' && (
            <Form.Item
              label={
                <Space>
                  <span>{t['cycle.mutate.cronExpression']}</span>
                  <a
                    href="http://cron.ciding.cc/"
                    target="_blank"
                    rel="noreferrer"
                  >
                    <IconQuestionCircleFill />
                  </a>
                </Space>
              }
              field="cronExpression"
              rules={rules}
            >
              <Input
                placeholder={t['cycle.mutate.cronExpression.placeholder']}
              />
            </Form.Item>
          )}
          <Divider>
            {t['cycle.mutate.componentIds']} (
            <span style={{ fontWeight: 'normal', fontSize: 12 }}>
              {t['cycle.mutate.componentIds.all']}{' '}
              <Switch
                size="small"
                checked={enableAll}
                onChange={setEnableAll}
              />
            </span>
            )
          </Divider>
          {!enableAll && (
            <Form.Item shouldUpdate noStyle>
              {(values) => (
                <FormList
                  field="componentIds"
                  rules={[
                    {
                      required: true,
                      type: 'array',
                      minLength: 1,
                      message: t['cycle.mutate.componentId.rule'],
                    },
                  ]}
                >
                  {(fields, { remove, add }) => (
                    <div>
                      <Spin className="w-full" loading={loading}>
                        {fields.map((item, index) => {
                          return (
                            <div
                              key={item.key}
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 14,
                              }}
                            >
                              {options.length > 0 && (
                                <Form.Item
                                  field={item.field}
                                  label={t['cycle.mutate.module'] + (index + 1)}
                                  rules={[{ required: true }]}
                                  className="flex-1"
                                  layout="horizontal"
                                >
                                  <Select
                                    allowClear
                                    options={options
                                      .filter(
                                        (p) =>
                                          values.componentIds?.[index] ||
                                          !values.componentIds?.find(
                                            (c) => c === p.id
                                          )
                                      )
                                      .map(({ id, name }) => ({
                                        label: name,
                                        value: id,
                                      }))}
                                  />
                                </Form.Item>
                              )}
                              <Button
                                icon={<IconDelete />}
                                shape="circle"
                                status="danger"
                                style={{
                                  marginBottom: 20,
                                }}
                                onClick={() => remove(index)}
                              />
                            </div>
                          );
                        })}
                      </Spin>
                      <Button
                        icon={<IconPlus />}
                        onClick={() => add()}
                        type="outline"
                        status="success"
                        long
                        style={{
                          display:
                            values.componentIds?.length === options.length
                              ? 'none'
                              : 'block',
                        }}
                      >
                        {t['cycle.mutate.module.add']}
                      </Button>
                    </div>
                  )}
                </FormList>
              )}
            </Form.Item>
          )}
          {enableAll && (
            <Typography.Paragraph>
              {t['cycle.mutate.componentIds.all.placeholder']} (
              {t['cycle.mutate.componentIds.all.tatol']} {options.length}{' '}
              {
                t[
                  `cycle.mutate.componentIds.all.${
                    options.length > 1 ? 'units' : 'unit'
                  }`
                ]
              }
              )
            </Typography.Paragraph>
          )}
          <Divider>{t['cycle.mutate.optional']}</Divider>
          <Form.Item label={t['cycle.mutate.time']} field="dateTimeRange">
            <RangePicker
              showTime={{
                hideDisabledOptions: true,
              }}
              disabledDate={(current) => current.isBefore(dayjs())}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default CycleTaskMutate;
