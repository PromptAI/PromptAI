import {
  Button,
  Message,
  Space,
  Table,
  TableColumnProps,
  Typography,
} from '@arco-design/web-react';
import React, { useMemo, useState } from 'react';
import SearchFrom from './SearchFrom';
import usePage from '@/hooks/usePage';
import {
  cancelRegisteCode,
  createRegisteCode,
  registCodes,
} from '@/api/projects';
import UpdateUser from './UpdateUser';
import TimeColumn from '@/components/TimeColumn';
import { IconPlus, IconSync } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const CancelCode = ({ item, refresh }) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  function handleCancel() {
    setLoading(true);
    cancelRegisteCode(item.id)
      .then(() => {
        Message.success('susccess');
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
      status="danger"
      loading={loading}
      onClick={handleCancel}
      disabled={item.status !== 'not_use'}
    >
      {t['canceled']}
    </Button>
  );
};
const AddCode = ({ refresh }) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  const handleCode = () => {
    setLoading(true);
    createRegisteCode()
      .then(() => {
        Message.success('success');
        refresh();
      })
      .catch(() => {
        Message.error('error');
      })
      .finally(() => setLoading(false));
  };
  return (
    <Button
      key="create"
      type="primary"
      icon={<IconPlus />}
      onClick={handleCode}
      loading={loading}
    >
      {t['create']}
    </Button>
  );
};

const RegisterCodes = () => {
  const t = useLocale(i18n);
  const codeStatusMap = useMemo(
    () => ({
      not_use: t['sysadmin.code.status.not_use'],
      canceled: t['sysadmin.code.status.canceled'],
      used: t['sysadmin.code.status.used'],
    }),
    [t]
  );
  const {
    dataSource: codes,
    onPageChange: onCodePageChange,
    params: codeParams,
    total: codeTotal,
    loading: codeLoading,
    refresh: codeRefresh,
    onSearch: codeSearch,
    onReset: codeReset,
  } = usePage(registCodes);
  const codeSearchForm = useMemo(
    () => [
      {
        label: t['sysadmin.code.form.code'],
        formtype: 'Input',
        field: 'code',
        placeholder: t['sysadmin.code.form.code'],
      },
      {
        label: t['sysadmin.code.form.accountDbName'],
        formtype: 'Input',
        field: 'accountDbName',
        placeholder: t['sysadmin.code.form.accountDbName'],
      },
      {
        label: t['sysadmin.code.form.status'],
        formtype: 'Select',
        field: 'status',
        placeholder: t['sysadmin.form.all'],
        options: [
          {
            label: t['sysadmin.code.status.not_use'],
            value: 'not_use',
          },
          {
            label: t['sysadmin.code.status.canceled'],
            value: 'canceled',
          },
          {
            label: t['sysadmin.code.status.used'],
            value: 'used',
          },
        ],
      },
    ],
    [t]
  );
  const codeColumnsSource = useMemo<TableColumnProps[]>(() => {
    return [
      {
        title: t['sysadmin.code.form.code'],
        dataIndex: '',
        render: (_, item) => (
          <Typography.Text copyable>{item.code}</Typography.Text>
        ),
      },
      {
        title: t['sysadmin.code.form.accountDbName'],
        dataIndex: 'accountDbName',
        align: 'center',
        render: (_, item) => {
          if (item.accountDbName) {
            return (
              <UpdateUser
                readyOnly
                initialValues={item.account}
                triggerTitle={item.account?.name || '-'}
              />
            );
          }
          return <Typography.Text>-</Typography.Text>;
        },
      },
      {
        title: t['sysadmin.code.form.status'],
        dataIndex: 'status',
        render: (_, item) => (
          <Typography.Text>{codeStatusMap[item.status] || '-'}</Typography.Text>
        ),
      },
      {
        title: t['sysadmin.createAt'],
        dataIndex: 'createAt',
        render: (_, row) => (
          <TimeColumn row={row} dataIndex="properties.createAt" />
        ),
      },
      {
        title: t['sysadmin.code.form.useAt'],
        dataIndex: 'useAt',
        render: (_, row) => (
          <TimeColumn row={row} dataIndex="properties.useAt" />
        ),
      },
      {
        title: t['sysadmin.operations'],
        width: 80,
        align: 'center',
        render: (_, item) => (
          <Space size="mini">
            <CancelCode item={item} refresh={codeRefresh} />
          </Space>
        ),
      },
    ];
  }, [codeRefresh, codeStatusMap, t]);
  return (
    <Space direction="vertical" className="w-full">
      <SearchFrom
        callback={codeSearch}
        formItem={codeSearchForm}
        onReset={codeReset}
        actions={[
          <AddCode key="add" refresh={codeRefresh} />,
          <Button
            key="reload"
            icon={<IconSync />}
            onClick={() => codeRefresh()}
            loading={codeLoading}
          >
            {t['refresh']}
          </Button>,
        ]}
      />
      <Table
        columns={codeColumnsSource}
        size="small"
        data={codes}
        rowKey="id"
        loading={codeLoading}
        pagination={{
          total: codeTotal,
          current: codeParams?.page + 1,
          pageSize: codeParams?.size,
          onChange: onCodePageChange,
          sizeCanChange: true,
          showTotal: true,
          size: 'mini',
        }}
      />
    </Space>
  );
};

export default RegisterCodes;
