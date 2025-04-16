import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Space,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import React, { useMemo } from 'react';
import i18n from './locale';
import { IconSync } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import { listOfGlobalBot } from '@/api/global-component';
import { Tool, useTools } from '@/components/Layout/tools-context';
import BotForm from './components/BotForm';
import { nanoid } from 'nanoid';
import useUrlParams from '../hooks/useUrlParams';
import UsedColumn from './components/UsedColumn';
import DeleteColumn from './components/DeleteColumn';

const GlobalBots = () => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const {
    loading,
    data = [],
    refresh,
  } = useRequest<any[], []>(() => listOfGlobalBot(projectId), {
    refreshDeps: [projectId],
  });

  const tools = useMemo<Tool[]>(
    () => [
      {
        key: 'add',
        component: (
          <BotForm
            value={{
              data: {
                name: null,
                responses: [
                  {
                    id: nanoid(),
                    type: 'text',
                    content: { text: null },
                    delay: 500,
                  },
                ],
              },
            }}
            callback={refresh}
          />
        ),
      },
      {
        key: 'refresh',
        component: (
          <Button
            type="text"
            icon={<IconSync />}
            loading={loading}
            onClick={refresh}
          >
            {t['globalBots.table.refresh']}
          </Button>
        ),
      },
    ],
    [loading, refresh, t]
  );
  useTools(tools);

  const columns = useMemo<TableColumnProps[]>(() => {
    return [
      {
        title: t['globalBots.table.text'],
        dataIndex: 'data.name',
        width: 320,
        ellipsis: true,
      },
      {
        title: t['globalBots.table.used'],
        key: 'used',
        render: (_, item) => <UsedColumn item={item} />,
      },
      {
        title: t['globalBots.table.option'],
        key: 'option',
        align: 'center',
        width: 110,
        render(col, item) {
          return (
            <Space>
              <BotForm value={item} callback={refresh} />
              <DeleteColumn item={item} onSuccess={refresh} />
            </Space>
          );
        },
      },
    ];
  }, [refresh, t]);

  return (
    <Card size="small" title={t['globalBots.title']}>
      <Table
        borderCell
        size="small"
        loading={loading}
        rowKey="id"
        data={data}
        columns={columns}
        scroll={{
          y: 'calc(100vh - 248px)',
        }}
        pagination={{
          size: 'mini',
          sizeCanChange: true,
          showTotal: true,
          total: data.length,
          defaultPageSize: 20,
          pageSizeChangeResetCurrent: true,
        }}
      />
    </Card>
  );
};

export default GlobalBots;
