import React, { useMemo } from 'react';
import { removeAccount, suspendedAccount, sysadmin } from '@/api/projects';
import usePage from '@/hooks/usePage';
import SearchFrom from './SearchFrom';
import {
  Button,
  Card,
  Divider,
  Link,
  Message,
  Popconfirm,
  Space,
  Table,
  TableColumnProps,
  Tag,
  Tooltip,
  Trigger,
  Typography,
} from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import moment from 'moment';
import UpdateUser from './UpdateUser';
import NoteModal from './NoteModal';
import {
  IconDelete,
  IconPauseCircle,
  IconPlayArrow,
  IconSync,
} from '@arco-design/web-react/icon';
import CreateUser from './createUser';
import i18n from './locale';

const Accounts = () => {
  const dt = useLocale(i18n);
  const {
    dataSource,
    onPageChange,
    params,
    total,
    loading,
    refresh,
    onSearch,
    onReset,
  } = usePage(sysadmin);
  const adminSearchForm = useMemo(
    () => [
      {
        label: dt['sysadmin.form.name'],
        formtype: 'Input',
        field: 'name',
        placeholder: dt['sysadmin.form.name.placeholder'],
      },
      {
        label: dt['sysadmin.form.accountType'],
        formtype: 'Select',
        field: 'type',
        placeholder: dt['sysadmin.form.all'],
        options: [
          {
            label: dt['sysadmin.form.accountType.normal'],
            value: 'normal',
          },
          {
            label: dt['sysadmin.form.accountType.trial'],
            value: 'trial',
          },
        ],
      },
      {
        label: dt['sysadmin.form.state'],
        formtype: 'Select',
        placeholder: dt['sysadmin.form.all'],
        options: [
          {
            label: dt['sysadmin.table.state.ready'],
            value: 'ready',
          },
          {
            label: dt['sysadmin.table.state.suspended'],
            value: 'suspended',
          },
          {
            label: dt['sysadmin.table.state.terminated'],
            value: 'terminated',
          },
          {
            label: dt['sysadmin.table.state.settingUp1'],
            value: 'settingUp1',
          },
        ],
        field: 'status',
      },
      {
        label: dt['sysadmin.form.active'],
        formtype: 'Select',
        field: 'active',
        placeholder: dt['sysadmin.form.all'],
        options: [
          {
            label: dt['sysadmin.form.actived'],
            value: true,
          },
          {
            label: dt['sysadmin.form.unactived'],
            value: false,
          },
        ],
      },
    ],
    [dt]
  );

  const columnsSource = useMemo<TableColumnProps[]>(() => {
    const suspended = (id, status) => {
      suspendedAccount({ status, id }).then(() => {
        Message.success(dt['message.suspended.success']);
        refresh();
      });
    };
    const deleteUser = (id: string) => {
      removeAccount(id)
        .then(() => {
          Message.success(dt['message.delete.success']);
          refresh();
        })
        .catch(() => null);
    };
    const statusEuem = {
      ready: dt['sysadmin.table.state.ready'],
      suspended: dt['sysadmin.table.state.suspended'],
      terminated: dt['sysadmin.table.state.terminated'],
      settingUp1: dt['sysadmin.table.state.settingUp1'],
    };
    const activeEnum = {
      true: dt['sysadmin.form.actived'],
      false: dt['sysadmin.form.unactived'],
    };
    return [
      {
        title: dt['sysadmin.table.name'],
        key: 'name',
        dataIndex: 'name',
        align: 'center',
        render(col, row) {
          return (
            <Space direction={'vertical'}>
              <Typography.Text>{row.name}</Typography.Text>
              <Typography.Text>{`( ${row.id || '-'} )`}</Typography.Text>
            </Space>
          );
        },
      },
      {
        title: dt['sysadmin.table.baseInfo'],
        key: 'baseInfo',
        render(col, row) {
          return (
            <Space>
              <Space direction={'vertical'}>
                <Typography.Text>{`${dt['sysadmin.table.admin']}:${
                  row?.admin
                } (${row?.adminName || '-'})`}</Typography.Text>
                <Typography.Text>{`${dt['sysadmin.table.timezone']}:${
                  row?.timezone || '-'
                }`}</Typography.Text>
                <Typography.Text>{`${dt['sysadmin.form.fullname']}:${
                  row?.fullName || '-'
                }`}</Typography.Text>
              </Space>
            </Space>
          );
        },
      },
      {
        title: dt['sysadmin.table.type'],
        key: 'type',
        dataIndex: 'type',
        render(col) {
          const types = {
            normal: dt['sysadmin.form.accountType.normal'],
            trial: dt['sysadmin.form.accountType.trial'],
          };
          return types[col] || '-';
        },
      },

      {
        title: dt['sysadmin.table.state'],
        key: 'terminated',
        dataIndex: 'status',
        render(col) {
          return (
            <Tag color={col === 'ready' ? 'green' : '#ffb400'}>
              {statusEuem[col]}
            </Tag>
          );
        },
      },
      {
        title: dt['sysadmin.form.active'],
        key: 'active',
        dataIndex: 'active',
        render(col) {
          return <Tag color={col ? 'green' : '#ffb400'}>{activeEnum[col]}</Tag>;
        },
      },
      {
        title: dt['sysadmin.table.events'],
        dataIndex: 'events',
        key: 'events',
        render(col) {
          const Popup = ({ content }) => {
            return (
              <Card>
                <Space direction="vertical">
                  {content.map((c) => {
                    return (
                      <Space
                        key={c.timeEpoch}
                        split={<Divider type="vertical" />}
                      >
                        <Typography.Text>
                          {moment(Number(c.timeEpoch)).format(
                            'YYYY-MM-DD HH:mm:ss'
                          )}
                        </Typography.Text>
                        <Typography.Text>{c.event}</Typography.Text>
                      </Space>
                    );
                  })}
                </Space>
              </Card>
            );
          };
          return col && col.length > 0 ? (
            <Trigger
              popup={() => <Popup content={col} />}
              mouseEnterDelay={400}
              mouseLeaveDelay={400}
              position="bottom"
            >
              <Button type="text">{`${col.length} ${dt['sysadmin.table.events']}`}</Button>
            </Trigger>
          ) : (
            dt['sysadmin.table.eventsNot']
          );
        },
      },
      {
        title: dt['sysadmin.table.notes'],
        dataIndex: 'notes',
        key: 'notes',
        render(col) {
          const Popup = ({ content }) => {
            return (
              <Card>
                {/* <Space direction="vertical">
                  {content.map((c) => {
                    return (
                      <Space
                        key={c.timeEpoch}
                        split={<Divider type="vertical" />}
                      >
                        <Typography.Text>
                          {moment(Number(c.timeEpoch)).format(
                            'YYYY-MM-DD HH:mm:ss'
                          )}
                        </Typography.Text>
                        <Typography.Text>{c.note}</Typography.Text>
                      </Space>
                    );
                  })}
                </Space> */}
                <Typography.Text>{content.join(',')}</Typography.Text>
              </Card>
            );
          };
          return col && col.length > 0 ? (
            <Trigger
              popup={() => <Popup content={col} />}
              mouseEnterDelay={400}
              mouseLeaveDelay={400}
              position="bottom"
            >
              <Button type="text">{`${col.length} ${dt['sysadmin.table.notes']}`}</Button>
            </Trigger>
          ) : (
            dt['sysadmin.table.notesNot']
          );
        },
      },
      {
        title: dt['sysadmin.form.registryCode'],
        dataIndex: 'registryCode',
        width: 220,
        render: (_, item) => (
          <Typography.Text
            style={{ width: 210 }}
            ellipsis
            copyable={!!item.registryCode}
          >
            {item.registryCode || '-'}
          </Typography.Text>
        ),
      },
      {
        title: dt['sysadmin.table.operation'],
        align: 'center',
        width: '100px',
        key: 'operation',
        render: (col, row) => {
          const { name, id, supportAccess, status } = row;
          return (
            <Space size={'small'}>
              <UpdateUser
                key={'update'}
                initialValues={row}
                callback={refresh}
              />
              <NoteModal initialValues={{ id }} callback={refresh} />
              <Tooltip content={dt['sysadmin.table.state.delete']}>
                <Popconfirm
                  position="tr"
                  icon={<IconDelete style={{ color: '#F53F3F' }} />}
                  title={`${dt['sysadmin.table.state.delete.title']}${name}`}
                  onOk={() => deleteUser(id)}
                  okButtonProps={{
                    status: 'danger',
                  }}
                  okText={dt['sysadmin.table.state.delete']}
                >
                  <Button
                    status={'danger'}
                    type="text"
                    icon={<IconDelete />}
                  ></Button>
                </Popconfirm>
              </Tooltip>
              <Tooltip
                content={
                  dt[
                    `sysadmin.table.state.${
                      status === 'ready' ? 'suspended' : 'ready'
                    }`
                  ]
                }
              >
                <Popconfirm
                  position="tr"
                  icon={<IconPauseCircle style={{ color: '#FF7D00' }} />}
                  title={`${
                    status === 'ready'
                      ? dt['sysadmin.table.state.suspended.before']
                      : dt['sysadmin.table.state.ready.before']
                  }${name}${dt['sysadmin.table.state.suspended.after']}?`}
                  onOk={() => {
                    suspended(id, status === 'ready' ? 'suspended' : 'ready');
                  }}
                  okButtonProps={{
                    status: 'warning',
                  }}
                  okText={
                    dt[
                      `sysadmin.table.state.${
                        status === 'ready' ? 'suspended' : 'ready'
                      }`
                    ]
                  }
                >
                  <Button
                    status={'warning'}
                    icon={
                      status === 'ready' ? (
                        <IconPauseCircle />
                      ) : (
                        <IconPlayArrow />
                      )
                    }
                    type="text"
                  />
                </Popconfirm>
              </Tooltip>
              <Tooltip content={dt['sysadmin.table.state.visit']}>
                <Link icon href={supportAccess}></Link>
              </Tooltip>
              {/* <Button icon={<IconLink />} type="text" > */}
              {/* </Button> */}
            </Space>
          );
        },
      },
    ];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dt]);
  return (
    <Space direction="vertical" className="w-full">
      <SearchFrom
        callback={onSearch}
        onReset={onReset}
        formItem={adminSearchForm}
        actions={[
          <CreateUser key={'createUser'} callback={refresh} />,
          <Button
            key="reload"
            icon={<IconSync />}
            onClick={() => refresh()}
            loading={loading}
          >
            {dt['refresh']}
          </Button>,
        ]}
      />
      <Table
        columns={columnsSource}
        size="small"
        data={dataSource}
        rowKey="id"
        loading={loading}
        pagination={{
          total: total,
          current: params?.page + 1,
          pageSize: params?.size,
          onChange: onPageChange,
          sizeCanChange: true,
          showTotal: true,
          size: 'mini',
        }}
      />
    </Space>
  );
};

export default Accounts;
