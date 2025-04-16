import useLocale from '@/utils/useLocale';
import {
  DatePicker,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  TableColumnProps,
  Tooltip,
} from '@arco-design/web-react';
import React, {
  Fragment,
  ReactNode,
  cloneElement,
  isValidElement,
  useEffect,
  useMemo,
} from 'react';
import i18n from './locale';
import useModalForm from '@/hooks/useModalForm';
import useCycleEnums from '../../hooks/useCycleEnums';
import { IconQuestionCircleFill } from '@arco-design/web-react/icon';
import dayjs from 'dayjs';
import { listConversations, listFaqs } from '@/api/components';
import { useRequest } from 'ahooks';
import useUrlParams from '../../../hooks/useUrlParams';
import moment from 'moment';
import { infoCycleTask } from '@/api/cycle-tasks';

const RangePicker: any = DatePicker.RangePicker;
interface CycleTaskInfoProps {
  id: string;
  trigger: ReactNode;
}

const fetchComponents = async (projectId: string) => {
  const faqs = await listFaqs(projectId);
  const flows = await listConversations(projectId);
  return [...faqs, ...flows]
    .filter((f) => f.data.isReady)
    .map((f) => ({ id: f.id, name: f.data.name }));
};

const unFormat = (values) => {
  if (!values) return {};
  const data: any = {
    ...values.properties?.data,
    ...values.properties?.param,
    records: values.properties?.records || [],
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
const CycleTaskInfo = ({ trigger, id }: CycleTaskInfoProps) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const {
    loading: dataLoading,
    data: initialValues,
    run: fetchData,
  } = useRequest(() => infoCycleTask(id), { manual: true });
  const formmatValues = useMemo(() => unFormat(initialValues), [initialValues]);
  const [visible, setVisible, form, rules] = useModalForm(formmatValues);
  const { modeEnum, statusEnum, trianTypeEnum, recordStatusEnum } =
    useCycleEnums();

  const {
    loading,
    data: options = [],
    run,
  } = useRequest(() => fetchComponents(projectId), {
    manual: true,
  });

  useEffect(() => {
    if (visible) {
      fetchData();
      run();
    }
  }, [fetchData, run, visible]);

  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['cycle.mutate.record.status'],
        dataIndex: 'status',
        render: (_, row) => recordStatusEnum[row.status],
      },
      {
        title: t['cycle.mutate.record.startTime'],
        dataIndex: 'startTime',
        render: (_, row) =>
          Number(row.startTime ?? null)
            ? moment(Number(row.startTime)).format('yyyy-MM-DD HH:mm:ss')
            : '-',
      },
      {
        title: t['cycle.mutate.record.endTime'],
        dataIndex: 'endTime',
        render: (_, row) =>
          Number(row.endTime ?? null)
            ? moment(Number(row.endTime)).format('yyyy-MM-DD HH:mm:ss')
            : '-',
      },
      {
        title: t['cycle.mutate.record.error'],
        dataIndex: 'error',
        ellipsis: true,
        render: (dom, row) => <Tooltip content={row.error}>{dom}</Tooltip>,
      },
    ],
    [recordStatusEnum, t]
  );
  return (
    <>
      {isValidElement(trigger) &&
        cloneElement(trigger, { onClick: () => setVisible(true) } as any)}
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        title={t[`cycle.mutate.title.info`]}
        style={{ width: 840 }}
        footer={(cancel) => cancel}
      >
        <Spin loading={dataLoading} className="w-full">
          <Form form={form} layout="vertical" disabled>
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
              {formmatValues.type === 'train-interval' && (
                <Form.Item
                  label={t['cycle.mutate.interval']}
                  field="interval"
                  rules={rules}
                >
                  <InputNumber min={0} style={{ width: 280 }} />
                </Form.Item>
              )}
            </Space>
            {formmatValues.type === 'train-cron' && (
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
            <Divider>{t['cycle.mutate.componentIds']}</Divider>
            <Form.Item shouldUpdate noStyle>
              {(values) => (
                <Form.Item
                  rules={[
                    { required: true, type: 'array', minLength: 1, min: 1 },
                  ]}
                >
                  <Form.List field="componentIds">
                    {(fields) => (
                      <div>
                        <Spin className="w-full" loading={loading}>
                          <div
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              flexWrap: 'wrap',
                              gap: 14,
                            }}
                          >
                            {fields.map((item, index) => {
                              return (
                                <Fragment key={item.key}>
                                  {options.length > 0 && (
                                    <Form.Item
                                      field={item.field}
                                      label={
                                        t['cycle.mutate.module'] + (index + 1)
                                      }
                                      rules={[{ required: true }]}
                                      layout="horizontal"
                                      style={{ width: 'max-content' }}
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
                                        style={{ width: 180 }}
                                      />
                                    </Form.Item>
                                  )}
                                </Fragment>
                              );
                            })}
                          </div>
                        </Spin>
                      </div>
                    )}
                  </Form.List>
                </Form.Item>
              )}
            </Form.Item>
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
          <Divider>{t['cycle.mutate.task.record']}</Divider>
          <Table
            size="small"
            border={{
              cell: true,
            }}
            rowKey="id"
            loading={loading}
            data={formmatValues.records}
            columns={columns}
            pagePosition="bl"
          />
        </Spin>
      </Modal>
    </>
  );
};

export default CycleTaskInfo;
