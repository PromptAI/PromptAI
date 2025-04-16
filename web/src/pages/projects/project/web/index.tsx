import { isHandling, pageWebLibs, stopHandling } from '@/api/text/web';
import PageContainer from '@/components/PageContainer';
import useSearch from '@/hooks/useSearch';
import useTable from '@/hooks/useTable';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  Select,
  Space,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import {
  IconFile,
  IconLoading,
  IconSearch,
  IconStop,
  IconStorage,
  IconSync,
} from '@arco-design/web-react/icon';
import React, { useMemo, useState } from 'react';
import i18n from './locale';
import trainStatusI18n from '../components/TrainStatusColumn/locale/index';
import TimeColumn from '@/components/TimeColumn';
import ExpandRow from './components/ExpandRow';
import useUrlParams from '../hooks/useUrlParams';
import CreateWebLib from './components/CreateWebLib';
import TrainStatusColumn from '../components/TrainStatusColumn';
import { useQuery } from 'react-query';
import EnableColumn from './components/EnableColumn';
import BatchDelete from './components/BatchDelete';
import DeleteComponentButton from '../components/DeleteComponentButton';
import BatchTrain from './components/BatchTrain';

const Web = () => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const {
    loading,
    data,
    total,
    params,
    setParams,
    onPageChange,
    reset,
    refresh,
  } = useTable((p) => pageWebLibs(projectId, p), { page: 0, size: 50 });
  const { form, onSubmit } = useSearch(setParams, reset);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['text.enable'],
        dataIndex: 'data.enable',
        key: 'enable',
        width: 60,
        align: 'center',
        render: (_, row) => <EnableColumn row={row} onSuccess={refresh} />,
      },
      {
        title: t['text.url'],
        dataIndex: 'data.url',
        ellipsis: true,
        width: 280,
        render: (value) => (
          <a
            title={value}
            href={value}
            target="_blank"
            rel="noreferrer"
            className={'hover:underline'}
          >
            {value}
          </a>
        ),
      },
      {
        title: t['text.status'],
        dataIndex: 'data.trainStatus',
        width: 140,
        render: (_, row) => (
          <TrainStatusColumn row={row} dataIndex="data.trainStatus" />
        ),
      },
      {
        title: t['text.content'],
        dataIndex: 'data.content',
        ellipsis: true,
        width: 200,
      },
      {
        title: t['text.remark'],
        dataIndex: 'data.description',
        ellipsis: true,
        width: 160,
        render: (value, row) => (row.data?.description ? value : '-'),
      },
      {
        title: t['text.createTime'],
        dataIndex: 'createTime',
        width: 180,
        render: (_, row) => <TimeColumn row={row} dataIndex="createTime" />,
      },
      {
        title: t['text.updateTime'],
        dataIndex: 'updateTime',
        width: 180,
        render: (_, row) => <TimeColumn row={row} dataIndex="updateTime" />,
      },
      {
        title: t['text.operator'],
        key: 'operater',
        align: 'center',
        width: 154,
        fixed: 'right',
        render: (_, row) => (
          <div className="flex justify-center items-center gap-2">
            <DeleteComponentButton
              ids={[row.id]}
              onSuccess={refresh}
              type="outline"
              size="mini"
            />
            <BatchTrain ids={[row.id]} type="primary" size="mini">
              {t['text.train']}
            </BatchTrain>
          </div>
        ),
      },
    ],
    [t, refresh]
  );

  async function stopHandle() {
    try {
      await stopHandling(projectId);
    } catch (e) {
      console.error('stop handling failed:', e);
    }
  }

  const promise = useMemo(
    () =>
      (() => {
        let originHandling = false; // 状态变更后再次刷新
        return async (projectId: string, callback: () => void) => {
          const info = await isHandling(projectId);
          if (info.handling !== originHandling || info.handling) {
            callback();
          }
          originHandling = info.handling;
          return info;
        };
      })(),
    []
  );
  const { data: info = { handling: false } } = useQuery(
    ['isHandling'],
    () => promise(projectId, refresh),
    {
      refetchInterval: 3000,
    }
  );

  const [selections, setSelections] = useState([]);
  const xWidth = useMemo(
    () =>
      columns.reduce(
        (p, c) => p + (!!Number(c.width) ? Number(c.width) : 0),
        0
      ),
    [columns]
  );
  const tTrainStatus = useLocale(trainStatusI18n);
  return (
    <PageContainer
      title={
        <Space>
          <IconFile />
          {t['text.title']}
        </Space>
      }
    >
      <Form
        layout="inline"
        form={form}
        onSubmit={onSubmit}
        className="mb-4 items-center"
      >
        {info.handling && (
          <div className="flex items-center w-fit gap-2 p-2 border border-sky-700 border-dashed rounded text-sky-700 shadow">
            <IconLoading className="animate-spin w-6 h-6" />
            <span>{info.count} tasks in progress...</span>

            <Button onClick={stopHandle} icon={<IconStop />}>
              {t['text.stop']}
            </Button>

          </div>
        )}
        <Form.Item
          label={t['text.trainStatus']}
          className="ml-auto"
          field="trainStatus"
        >
          <Select
            style={{ width: 165 }}
            placeholder={t['search.select.all']}
            allowClear
          >
            <Select.Option value="wait_parse">
              {tTrainStatus['train.status.wait_parse']}
            </Select.Option>
            <Select.Option value="parsing">
              {tTrainStatus['train.status.parsing']}
            </Select.Option>
            <Select.Option value="parse_fail">
              {tTrainStatus['train.status.parse_fail']}
            </Select.Option>
            <Select.Option value="wait_train">
              {tTrainStatus['train.status.wait']}
            </Select.Option>
            <Select.Option value="training">
              {tTrainStatus['train.status.training']}
            </Select.Option>
            <Select.Option value="finish_train">
              {tTrainStatus['train.status.finish']}
            </Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label={t['text.content']} field="keyword">
          <Input placeholder={t['text.content.placeholder']} />
        </Form.Item>
        <Form.Item>
          <Button icon={<IconSync />} onClick={refresh}>
            {t['text.search.refresh']}
          </Button>
        </Form.Item>
        <Form.Item>
          <Button htmlType="submit" type="primary" icon={<IconSearch />}>
            {t['text.search.search']}
          </Button>
        </Form.Item>
        <Form.Item>
          <CreateWebLib onSuccess={refresh} />
        </Form.Item>
        {selections.length > 0 && (
          <Form.Item>
            <BatchTrain
              ids={selections}
              type="primary"
              icon={<IconStorage />}
            />
          </Form.Item>
        )}
        {selections.length > 0 && (
          <Form.Item>
            <BatchDelete
              ids={selections}
              onSuccess={() => {
                refresh();
                setSelections([]);
              }}
            />
          </Form.Item>
        )}
      </Form>
      <Table
        loading={loading}
        data={data}
        rowKey="id"
        columns={columns}
        pagination={{
          total,
          current: params.page + 1,
          onChange: onPageChange,
          size: 'mini',
          showTotal: true,
          sizeCanChange: true,
          defaultPageSize: 50,
        }}
        expandedRowRender={(row) => <ExpandRow row={row} onSuccess={refresh} />}
        rowSelection={{
          type: 'checkbox',
          checkAll: true,
          onChange: setSelections,
        }}
        scroll={{ x: xWidth }}
      />
    </PageContainer>
  );
};

export default Web;
