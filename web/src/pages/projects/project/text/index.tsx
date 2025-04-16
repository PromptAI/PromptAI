import { pageTextLibs } from '@/api/text/text';
import PageContainer from '@/components/PageContainer';
import useSearch from '@/hooks/useSearch';
import useTable from '@/hooks/useTable';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  Space,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import { IconFile, IconSearch, IconSync } from '@arco-design/web-react/icon';
import React, { useMemo, useState } from 'react';
import i18n from './locale';
import TimeColumn from '@/components/TimeColumn';
import ExpandRow from './components/ExpandRow';
import useUrlParams from '../hooks/useUrlParams';
import CreateTextLib from './components/CreateTextLib';
import TrainStatusColumn from '../components/TrainStatusColumn';
import TrainLib from '../components/TrainLib';
import EnableColumn from './components/EnableColumn';
import BatchDelete from './components/BatchDelete';
import DeleteComponentButton from '../components/DeleteComponentButton';

const Text = () => {
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
  } = useTable((p) => pageTextLibs(projectId, p), { page: 0, size: 50 });
  const { form, onSubmit } = useSearch(setParams, reset);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['text.enable'],
        dataIndex: 'data.enable',
        key: 'enable',
        render: (_, row) => <EnableColumn row={row} onSuccess={refresh} />,
        width: 60,
        align: 'center',
      },
      {
        title: t['text.content'],
        dataIndex: 'data.content',
        ellipsis: true,
        width: 200,
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
            <TrainLib
              componentId={row.id}
              onSuccess={refresh}
              disabled={row.data.trainStatus === 'finish_train'}
              size="mini"
            />
          </div>
        ),
      },
    ],
    [t, refresh]
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
        style={{ justifyContent: 'flex-end', marginBottom: 16 }}
      >
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
          <CreateTextLib onSuccess={refresh} />
        </Form.Item>
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

export default Text;
