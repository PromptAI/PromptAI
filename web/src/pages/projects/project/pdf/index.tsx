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
import useUrlParams from '../hooks/useUrlParams';
import { pagePdfLibs } from '@/api/text/pdf';
import { CreatePdfLib, ExpandRow } from './components';
import ShrinkSizeColumn from '@/components/ShrinkSizeColumn';
import TrainStatusColumn from '../components/TrainStatusColumn';
import TrainLib from '../components/TrainLib';
import EnableColumn from './components/EnableColumn';
import BatchDelete from './components/BatchDelete';
import DeleteComponentButton from '../components/DeleteComponentButton';

const Pdf = () => {
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
  } = useTable((p) => pagePdfLibs(projectId, p), { page: 0, size: 50 });
  const { form, onSubmit } = useSearch(setParams, reset);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['pdf.enable'],
        dataIndex: 'data.enable',
        key: 'enable',
        render: (_, row) => <EnableColumn row={row} onSuccess={refresh} />,
        width: 60,
        align: 'center',
      },
      {
        title: t['pdf.name'],
        dataIndex: 'data.originFileName',
        ellipsis: true,
        width: 200,
      },
      {
        title: t['pdf.status'],
        dataIndex: 'data.trainStatus',
        width: 140,
        render: (_, row) => (
          <TrainStatusColumn row={row} dataIndex="data.trainStatus" />
        ),
      },
      {
        title: t['pdf.size'],
        dataIndex: 'data.fileSize',
        width: 80,
        render: (_, row) => (
          <ShrinkSizeColumn row={row} dataIndex="data.fileSize" />
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
        title: t['pdf.createTime'],
        dataIndex: 'createTime',
        width: 180,
        render: (_, row) => <TimeColumn row={row} dataIndex="createTime" />,
      },
      {
        title: t['pdf.updateTime'],
        dataIndex: 'updateTime',
        width: 180,
        render: (_, row) => <TimeColumn row={row} dataIndex="updateTime" />,
      },
      {
        title: t['pdf.operator'],
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
              size="mini"
              disabled={row.data.trainStatus === 'finish_train'}
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
          {t['pdf.title']}
        </Space>
      }
    >
      <Form
        layout="inline"
        form={form}
        onSubmit={onSubmit}
        style={{ justifyContent: 'flex-end', marginBottom: 16 }}
      >
        <Form.Item label={t['pdf.content']} field="keyword">
          <Input placeholder={t['pdf.content.placeholder']} />
        </Form.Item>
        <Form.Item>
          <Button icon={<IconSync />} onClick={refresh}>
            {t['pdf.search.refresh']}
          </Button>
        </Form.Item>
        <Form.Item>
          <Button htmlType="submit" type="primary" icon={<IconSearch />}>
            {t['pdf.search.search']}
          </Button>
        </Form.Item>
        <Form.Item>
          <CreatePdfLib onSuccess={refresh} />
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

export default Pdf;
