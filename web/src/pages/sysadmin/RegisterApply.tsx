import React, { useMemo, useState } from 'react';
import {
  Button,
  Message,
  Space,
  Table,
  TableColumnProps,
  Typography,
} from '@arco-design/web-react';
import SearchFrom from './SearchFrom';
import usePage from '@/hooks/usePage';
import { trialApplyList, trialApplyPass } from '@/api/projects';
import UseSceneColumn from './components/UseSceneColumn';
import TimeColumn from '@/components/TimeColumn';
import { IconSync } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const Apply = ({ type, item, refresh, status, children }) => {
  const [loading, setLoading] = useState(false);
  function handleApply(id, status) {
    setLoading(true);
    trialApplyPass({ id, status })
      .then(() => {
        Message.success('success');
        refresh();
      })
      .catch(() => {
        Message.error('error');
      })
      .finally(() => setLoading(false));
  }
  return (
    <Button
      type="text"
      status={status}
      loading={loading}
      onClick={() => handleApply(item.id, type)}
      disabled={item.status !== 0}
    >
      {children}
    </Button>
  );
};
const RegisterApply = () => {
  const t = useLocale(i18n);
  const aproveStatusMap = useMemo(
    () => ({
      0: t['sysadmin.apply.status.0'],
      1: t['sysadmin.apply.status.1'],
      2: t['sysadmin.apply.status.2'],
    }),
    [t]
  );
  const {
    dataSource: applies,
    onPageChange: onApplyPageChange,
    params: applyParams,
    total: applyTotal,
    loading: applyLoading,
    refresh: applyRefresh,
    onSearch: applySearch,
    onReset: applyReset,
  } = usePage(trialApplyList);
  const applyColumnsSource = useMemo<TableColumnProps[]>(() => {
    return [
      {
        title: t['sysadmin.apply.mobile'],
        dataIndex: 'mobile',
      },
      {
        title: t['sysadmin.apply.email'],
        dataIndex: 'email',
      },
      {
        title: t['sysadmin.apply.status'],
        dataIndex: 'status',
        render: (_, item) => (
          <Typography.Text>
            {aproveStatusMap[item.status] || '-'}
          </Typography.Text>
        ),
      },
      {
        title: t['sysadmin.apply.useScene'],
        dataIndex: 'properties.useScene',
        render: (_, row) => <UseSceneColumn row={row} />,
      },
      {
        title: t['sysadmin.createAt'],
        dataIndex: 'createTime',
        render: (_, row) => (
          <TimeColumn row={row} dataIndex="properties.createTime" />
        ),
      },
      {
        title: t['sysadmin.updateTime'],
        dataIndex: 'updateTime',
        render: (_, row) => (
          <TimeColumn row={row} dataIndex="properties.updateTime" />
        ),
      },
      {
        title: t['operations'],
        width: 180,
        align: 'center',
        render: (_, item) => (
          <Space size="mini">
            <Apply status="success" type="1" item={item} refresh={applyRefresh}>
              {t['sysadmin.apply.resolve']}
            </Apply>
            <Apply status="danger" type="2" item={item} refresh={applyRefresh}>
              {t['sysadmin.apply.reject']}
            </Apply>
          </Space>
        ),
      },
    ];
  }, [applyRefresh, aproveStatusMap, t]);
  const applySearchForm = useMemo(
    () => [
      {
        label: t['sysadmin.apply.mobile'],
        formtype: 'Input',
        field: 'mobile',
        placeholder: t['sysadmin.apply.mobile'],
      },
      {
        label: t['sysadmin.apply.email'],
        formtype: 'Input',
        field: 'email',
        placeholder: t['sysadmin.apply.email'],
      },
      {
        label: t['sysadmin.apply.status'],
        formtype: 'Select',
        field: 'status',
        placeholder: t['sysadmin.form.all'],
        options: [
          {
            label: t['sysadmin.apply.status.0'],
            value: 0,
          },
          {
            label: t['sysadmin.apply.status.1'],
            value: 1,
          },
          {
            label: t['sysadmin.apply.status.2'],
            value: 2,
          },
        ],
      },
    ],
    [t]
  );
  return (
    <Space direction="vertical" className="w-full">
      <SearchFrom
        callback={applySearch}
        formItem={applySearchForm}
        onReset={applyReset}
        actions={[
          <Button
            key="reload"
            icon={<IconSync />}
            onClick={() => applyRefresh()}
            loading={applyLoading}
          >
            {t['refresh']}
          </Button>,
        ]}
      />
      <Table
        columns={applyColumnsSource}
        size="small"
        data={applies}
        rowKey="id"
        loading={applyLoading}
        pagination={{
          total: applyTotal,
          current: applyParams?.page + 1,
          pageSize: applyParams?.size,
          onChange: onApplyPageChange,
          sizeCanChange: true,
          showTotal: true,
          size: 'mini',
        }}
      />
    </Space>
  );
};

export default RegisterApply;
