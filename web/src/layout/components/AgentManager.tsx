import { getAgentPage } from '@/api/agent';
import TimeColumn from '@/components/TimeColumn';
import {
  Button,
  Dropdown,
  Form,
  FormInstance,
  Link,
  Menu,
  Modal,
  Select,
  Space,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import { useRequest, useToggle } from 'ahooks';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import AgentAdd from './AgentAdd';
import AgentCommand from './AgentCommand';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import AgentDefaultSwitch from './AgentDefaultSwitch';
import AgentDelete from './AgentDelete';
import useAgentStore from './agent-store';
import useDocumentLinks from '@/hooks/useDocumentLinks';

const AgentManager = () => {
  const t = useLocale(i18n);
  const [visible, { toggle }] = useToggle(false);
  const [params, setParams] = useState({});
  const {
    loading,
    data = { totalCount: 0, data: [] },
    refresh,
  } = useRequest<any, []>(() => getAgentPage(params), {
    manual: !visible,
    refreshDeps: [visible, params],
  });
  // const BoolOptions = useMemo(
  //   () => Object.entries(BoolEnum).map(([k, v]) => ({ label: v, value: k })),
  //   [BoolEnum]
  // );
  const StatusEnum = useMemo(
    () => ({ '1': t['agent.status.1'], '0': t['agent.status.0'] }),
    [t]
  );
  const StatusOptions = useMemo(
    () => Object.entries(StatusEnum).map(([k, v]) => ({ label: v, value: k })),
    [StatusEnum]
  );
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['agent.name'],
        dataIndex: 'name',
      },
      {
        title: t['agent.ip'],
        dataIndex: 'ip',
      },
      {
        title: t['agent.version'],
        dataIndex: 'version',
      },
      {
        title: t['agent.status'],
        dataIndex: 'status',
        render: (value) => StatusEnum[value] || '-',
      },
      {
        title: t['agent.running.number'],
        dataIndex: 'running',
      },
      {
        title: t['agent.default'],
        dataIndex: 'default',
        render: (_, row) => (
          <AgentDefaultSwitch row={row} onSuccess={refresh} />
        ),
      },
      {
        title: t['agent.createTime'],
        dataIndex: 'createTime',
        render: (_, row) => <TimeColumn row={row} dataIndex="createTime" />,
      },
      {
        title: t['agent.optional'],
        key: 'op',
        align: 'center',
        width: 200,
        render: (_, row) => (
          <>
            {row.status === 0 && <AgentCommand row={row} />}
            <AgentDelete
              row={row}
              onSuccess={refresh}
              title={t['agent.delete.tip']}
            >
              {t['agent.delete']}
            </AgentDelete>
          </>
        ),
      },
    ],
    [StatusEnum, refresh, t]
  );
  const formRef = useRef<FormInstance>();
  const onSearch = useCallback(async () => {
    const values = await formRef.current.validate();
    setParams(values);
  }, []);
  const { available, toggleAgentTip } = useAgentStore((state) => ({
    toggleAgentTip: state.toggle,
    available: state.available,
  }));
  const triggerTip = useCallback(() => {
    Modal.confirm({
      title: t['agent.available.title'],
      content: t['agent.available.tip'],
      onOk: toggle,
      okText: t['agent.trigger'],
    });
  }, [t, toggle]);
  const docs = useDocumentLinks();
  return (
    <div>
      <Dropdown
        droplist={
          <Menu>
            <Menu.Item key="why">
              <Link target="_blank" href={docs.runBot}>
                {t['agent.trigger.agent.why']}
              </Link>
            </Menu.Item>
            <Menu.Item
              key="install"
              onClick={available ? triggerTip : toggleAgentTip}
            >
              <Link style={{ color: 'var(--color-text-1)' }}>
                {t['agent.trigger.agent.install']}
              </Link>
            </Menu.Item>
            <Menu.Item key="manage" onClick={toggle}>
              <Link style={{ color: 'var(--color-text-1)' }}>
                {t['agent.trigger']}
              </Link>
            </Menu.Item>
          </Menu>
        }
      >
        <Button type="text">{t['agent.trigger.title']}</Button>
      </Dropdown>
      <Modal
        visible={visible}
        style={{ width: '80%' }}
        title={<div style={{ textAlign: 'left' }}>{t['agent.trigger']}</div>}
        autoFocus={false}
        focusLock={true}
        footer={(cancel) => cancel}
        cancelText={t['agent.close']}
        onOk={toggle}
        onCancel={toggle}
      >
        <div className="flex">
          <Form ref={formRef} layout="inline" className="flex-1">
            {/* <Form.Item label="Name" field="name">
              <Input size="small" placeholder="Name" allowClear />
            </Form.Item>
            <Form.Item label="CUDA" field="cuda">
              <Select
                size="small"
                style={{ width: 80 }}
                options={BoolOptions}
                placeholder="全部"
                allowClear
              />
            </Form.Item> */}
            <Form.Item label={t['agent.status']} field="status">
              <Select
                size="small"
                style={{ width: 100 }}
                options={StatusOptions}
                placeholder={t['agent.placeholder.all']}
                allowClear
              />
            </Form.Item>
          </Form>
          <Space size="small">
            <Button type="outline" size="small" onClick={onSearch}>
              {t['agent.search']}
            </Button>
            <AgentAdd onSuccess={refresh} />
          </Space>
        </div>
        <Table
          style={{ marginTop: 16 }}
          size="small"
          border={{
            cell: true,
          }}
          rowKey="id"
          loading={loading}
          data={data.data}
          columns={columns}
          pagination={{
            total: data.totalCount,
            showTotal: true,
            size: 'mini',
          }}
        />
      </Modal>
    </div>
  );
};

export default AgentManager;
