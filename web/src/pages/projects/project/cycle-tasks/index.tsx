import useTable from '@/hooks/useTable';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Form,
  Select,
  Space,
  Table,
  TableColumnProps,
  Tooltip,
} from '@arco-design/web-react';
import React, { useMemo } from 'react';
import useUrlParams from '../hooks/useUrlParams';
import { pageCycleTasks } from '@/api/cycle-tasks';
import useSearch from '@/hooks/useSearch';
import {
  IconEdit,
  IconInfoCircle,
  IconPlus,
  IconSearch,
} from '@arco-design/web-react/icon';
import i18n from './locale';
import DeleteColumn from './components/DeleteColumn';
import useCycleEnums from './hooks/useCycleEnums';
import CycleTaskMutate from './components/CycleTaskMutate';
import CycleTaskInfo from './components/CycleTaskInfo';

function CycleTasks() {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { loading, data, setParams, reset, refresh } = useTable(
    pageCycleTasks,
    { projectId }
  );
  const { form, onSubmit } = useSearch(setParams, reset);
  const { modeEnum, statusEnum, trianTypeEnum } = useCycleEnums();
  const columns = useMemo<TableColumnProps[]>(
    () => [
      { title: 'ID', dataIndex: 'id' },
      {
        title: t['cycle.filter.type'],
        dataIndex: 'type',
        valueEnum: modeEnum,
        render: (_, row) =>
          row.type === 'train-interval'
            ? `${modeEnum[row.type]}: ${Number(
                row.properties.param.interval
              ).toLocaleString()} ${t['cycle.filter.type.train-interval.unit']}`
            : `${modeEnum[row.type]}: ${row.properties.param.cronExpression}`,
      },
      {
        title: t['cycle.filter.module.length'],
        dataIndex: 'properties.data.componentIds.length',
      },
      {
        title: t['cycle.filter.status'],
        dataIndex: 'status',
        render: (_, row) => statusEnum[row.status],
      },
      {
        title: t['cycle.filter.trainType'],
        dataIndex: 'properties.data.trainType',
        render: (_, row) => trianTypeEnum[row.properties.data.trainType],
      },
      {
        title: t['cycle.filter.option'],
        key: 'option',
        width: 80,
        align: 'center',
        render: (_, row) => (
          <Space>
            <CycleTaskInfo
              id={row.id}
              trigger={
                <Tooltip content={t['cycle.info']}>
                  <Button size="small" type="text" icon={<IconInfoCircle />} />
                </Tooltip>
              }
            />
            <CycleTaskMutate
              mode="update"
              trigger={
                <Tooltip content={t['cycle.update']}>
                  <Button size="small" type="text" icon={<IconEdit />} />
                </Tooltip>
              }
              initialValues={row}
              onSuccess={refresh}
            />
            <DeleteColumn row={row} onSuccess={refresh} />
          </Space>
        ),
      },
    ],
    [modeEnum, refresh, statusEnum, t, trianTypeEnum]
  );
  return (
    <Card size="small" title={t['cycle.title']}>
      <Form
        layout="inline"
        form={form}
        onSubmit={onSubmit}
        style={{ marginBottom: 16, justifyContent: 'flex-end' }}
      >
        <Form.Item label={t['cycle.filter.status']} field="status">
          <Select
            allowClear
            style={{ width: 100 }}
            placeholder={t['search.select.all']}
          >
            {Object.entries(statusEnum).map(([k, v]) => (
              <Select.Option value={k} key={k}>
                {v}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item label={t['cycle.filter.type']} field="type">
          <Select
            allowClear
            style={{ width: 220 }}
            placeholder={t['search.select.all']}
          >
            {Object.entries(modeEnum).map(([k, v]) => (
              <Select.Option value={k} key={k}>
                {v}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item>
          <Button
            loading={loading}
            htmlType="submit"
            type="primary"
            icon={<IconSearch />}
          >
            {t['cycle.filter.search']}
          </Button>
        </Form.Item>

        <CycleTaskMutate
          mode="create"
          trigger={
            <Button type="primary" icon={<IconPlus />}>
              {t['cycle.filter.create']}
            </Button>
          }
          initialValues={{
            mode: 'train-interval',
            status: '1',
          }}
          onSuccess={refresh}
        />
      </Form>
      <Table
        size="small"
        border={{
          cell: true,
        }}
        rowKey="id"
        loading={loading}
        data={data}
        columns={columns}
        pagePosition="bl"
      />
    </Card>
  );
}

export default CycleTasks;
