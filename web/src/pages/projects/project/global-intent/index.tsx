import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Message,
  Modal,
  Space,
  Table,
  TableColumnProps,
  Tooltip,
} from '@arco-design/web-react';
import React, { useMemo } from 'react';
import { useParams } from 'react-router';
import { Link } from 'react-router-dom';
import i18n from './locale';
import { IconDelete, IconSync } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import { delGlobalIntent, listOfGlobalIntent } from '@/api/global-component';
import { GlobalIntent } from '@/graph-next/type';
import { groupBy, isEmpty } from 'lodash';
import IntentModal from './components/IntentModal';
import { Tool, useTools } from '@/components/Layout/tools-context';
import UploadIntent from './components/UploadIntent';
import RefTag from '@/components/RefTag';

const Intents = () => {
  const t = useLocale(i18n);
  const { id: projectId } = useParams<{ id: string }>();
  const {
    loading,
    data = [],
    refresh,
  } = useRequest<GlobalIntent[], []>(() => listOfGlobalIntent(projectId), {
    refreshDeps: [projectId],
  });

  const tools = useMemo<Tool[]>(
    () => [
      {
        key: 'add',
        component: <IntentModal callback={refresh} />,
      },
      {
        key: 'upload',
        component: <UploadIntent onSuccess={refresh} />,
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
            {t['intents.table.refresh']}
          </Button>
        ),
      },
    ],
    [loading, refresh, t]
  );
  useTools(tools);

  const columns = useMemo<TableColumnProps[]>(() => {
    const handleDeleteItem = (id: string) => {
      Modal.confirm({
        title: t['intents.detele.modelTitle'],
        content: (
          <div className="flex justify-center">
            <span>{t['intents.detele.modelDescription']}</span>
          </div>
        ),
        onOk: async () => {
          await delGlobalIntent(projectId, [id]);
          Message.success(t['intents.detele.success']);
          refresh();
        },
        footer: (cancel, ok) => (
          <>
            {ok}
            {cancel}
          </>
        ),
      });
    };
    return [
      {
        title: t['intents.table.text'],
        key: 'text',
        width: 320,
        ellipsis: true,
        render(col, item) {
          return item?.data?.name;
        },
      },
      {
        title: t['intents.table.used'],
        key: 'used',
        ellipsis: true,
        render(_, item) {
          const group = Object.values(
            groupBy(
              item?.componentRelation?.usedByComponentRoots || [],
              'rootComponentId'
            )
          )
            .map((item) => ({
              rootComponentId: item?.[0]?.rootComponentId,
              rootComponentName: item?.[0]?.rootComponentName,
              refLength: item?.length,
            }))
            .filter((item) => !isEmpty(item.rootComponentId));

          return (
            <Space wrap>
              {group.map(
                ({ rootComponentId, rootComponentName, refLength }) => (
                  <Link
                    key={rootComponentId}
                    to={`/projects/${projectId}/overview/complexs/${rootComponentId}/branch/complex`}
                    className="no-underline"
                  >
                    <RefTag>
                      {`(${rootComponentName}) ${t['intents.table.use']} ${refLength}`}
                    </RefTag>
                  </Link>
                )
              )}
            </Space>
          );
        },
      },
      {
        title: t['intents.table.length'],
        key: 'length',
        width: 150,
        render(col, item) {
          return item?.data?.examples && item.data.examples.length;
        },
      },
      {
        title: t['intents.table.option'],
        key: 'option',
        align: 'center',
        width: 110,
        render(col, item) {
          return (
            <Space>
              <IntentModal value={item} callback={refresh} />
              <Tooltip content={t['intents.table.delete']}>
                <Button
                  type="text"
                  size="mini"
                  status="danger"
                  onClick={() => handleDeleteItem(item.id)}
                >
                  <IconDelete />
                </Button>
              </Tooltip>
            </Space>
          );
        },
      },
    ];
  }, [projectId, refresh, t]);

  return (
    <Card size="small" title={t['intents.title']}>
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

export default Intents;
