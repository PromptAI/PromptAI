import { deleteUser, pageUsers } from '@/api/settings/users';
import PageContainer from '@/components/PageContainer';
import useSearch from '@/hooks/useSearch';
import useTable from '@/hooks/useTable';
import {
  Button,
  Form,
  Input,
  Space,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import {
  IconSearch,
  IconSync,
  IconUserGroup,
} from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import TimeColumn from '@/components/TimeColumn';
import DeleteColumn from '@/components/DeleteColumn';
// import EditUserColumn from './components/EditUserColumn';
import CreateUser from './components/CreateUser';
import useLocale from '@/utils/useLocale';
import i18n from './components/locale';
import UpdatePwd from './components/EditUserColumn/UpdatePwd';

const SettingsUsers = () => {
  const {
    loading,
    data,
    total,
    params,
    setParams,
    onPageChange,
    reset,
    refresh,
  } = useTable(pageUsers);
  const { form, onSubmit } = useSearch(setParams, reset);
  const t = useLocale(i18n);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      { title: t['user.name'], dataIndex: 'username' },
      { title: t['user.email'], dataIndex: 'email' },
      { title: t['user.mobile'], dataIndex: 'mobile' },
      {
        title: t['user.create.time'],
        dataIndex: 'createTime',
        render: (_, row) => <TimeColumn row={row} dataIndex="createTime" />,
      },
      {
        title: t['user.options'],
        key: 'option',
        align: 'center',
        width: 300,
        render: (_, row) => (
          <Space wrap>
            {/* <EditUserColumn row={row} onSuccess={refresh} /> */}
            <UpdatePwd userId={row.id} size="small" type="text" />
            <DeleteColumn
              title={t['user.delete.confirm']}
              row={row}
              promise={deleteUser}
              onSuccess={refresh}
            >
              {t['user.delete']}
            </DeleteColumn>
          </Space>
        ),
      },
    ],
    [refresh, t]
  );
  return (
    <PageContainer
      title={
        <Space>
          <IconUserGroup />
          {t['user.manage']}
        </Space>
      }
    >
      <Form
        layout="inline"
        form={form}
        onSubmit={onSubmit}
        style={{ justifyContent: 'flex-end', marginBottom: 16 }}
      >
        <Form.Item label={t['user.name']} field="username">
          <Input placeholder={t['user.name.placeholder']} />
        </Form.Item>
        <Form.Item label={t['user.email']} field="email">
          <Input placeholder={t['user.email.placeholder']} />
        </Form.Item>
        <Form.Item label={t['user.mobile']} field="mobile">
          <Input placeholder={t['user.mobile.placeholder']} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button icon={<IconSync />} onClick={refresh}>
              {t['user.search.refresh']}
            </Button>
            <Button htmlType="submit" type="primary" icon={<IconSearch />}>
              {t['user.search']}
            </Button>
            <CreateUser onSuccess={refresh}>{t['user.create']}</CreateUser>
          </Space>
        </Form.Item>
      </Form>
      <Table
        loading={loading}
        data={data}
        rowKey="id"
        columns={columns}
        pagination={{
          total,
          current: params.page + 1,
          pageSize: params.size,
          onChange: onPageChange,
          size: 'mini',
          showTotal: true,
          sizeCanChange: true,
        }}
      />
    </PageContainer>
  );
};

export default SettingsUsers;
