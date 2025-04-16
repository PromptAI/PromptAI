import { deleteComponent, listCompSlots } from '@/api/components';
import {
  Button,
  Card,
  Message,
  Modal,
  Space,
  Table,
  Tag,
  Tooltip,
} from '@arco-design/web-react';
import { IconDelete, IconSync } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import React, { useMemo } from 'react';
import { useParams } from 'react-router';
import { Slot } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { Tool, useTools } from '@/components/Layout/tools-context';
import SlotModal from './components/slotModal';
import { UploadCorpus, OperationMenuCorpus } from './components/corpus';
import { useProjectType } from '@/layout/project-layout/context';
import { Link } from 'react-router-dom';
import useUrlParams from '../hooks/useUrlParams';

const Relations = ({ row }: { row: any }) => {
  const {
    componentRelation: { usedByComponentRoots = [] },
  } = row;
  const { projectId } = useUrlParams();
  return (
    <div className="flex items-center gap-2 flex-wrap">
      {usedByComponentRoots.map((item) => (
        <Link
          key={item.componentId}
          to={`/projects/${projectId}/overview/complexs/${item.rootComponentId}/branch/complex`}
        >
          <Tag color="blue">{item.rootComponentName}</Tag>
        </Link>
      ))}
    </div>
  );
};

const Slots = () => {
  const t = useLocale(i18n);
  const { id: projectId } = useParams<{ id: string }>();
  const type = useProjectType();

  const {
    loading,
    data = [],
    refresh,
  } = useRequest<Slot[], []>(
    () =>
      listCompSlots({
        projectId,
        ...(type === 'llm' ? { blnInternal: false } : {}),
      }),
    {
      refreshDeps: [projectId, type],
    }
  );

  const columns = useMemo<any[]>(() => {
    function handleDeleteSlotItem(item) {
      Modal.confirm({
        title: t['slot.table.delete.title'],
        content: t['slot.table.delete.content'],
        onOk: async () => {
          await deleteComponent(projectId, [item.id]);
          Message.success(t['slot.success']);
          refresh();
        },
        footer: (cancel, ok) => (
          <>
            {ok}
            {cancel}
          </>
        ),
      });
    }
    return [
      {
        title: t['slot.table.name'],
        dataIndex: 'name',
      },
      {
        title: t['slot.table.relations'],
        key: 'relations',
        render: (_, row) => <Relations row={row} />,
      },
      ...(type === 'rasa'
        ? [
            {
              title: t['slot.table.dictionary'],
              dataIndex: 'dictionary',
              align: 'center',
              render(_, row) {
                if (!row.blnEdit) return '-';
                return row.data?.dictionary?.length ? (
                  <OperationMenuCorpus
                    row={row}
                    classifier={t['slot.table.dictionary.classifier']}
                    title={t['slot.table.dictionary.detail']}
                    onSuccess={refresh}
                    placeholder={t['slot.table.dictionary.detail.search']}
                  />
                ) : (
                  <UploadCorpus
                    name={row.display}
                    slotId={row.id}
                    onSuccess={refresh}
                  />
                );
              },
            },
            {
              title: t['slot.table.blnInternal'],
              dataIndex: 'blnInternal',
              width: 110,
              align: 'center',
              render: (col) => t[`slot.table.blnInternal.${col}`],
            },
          ]
        : [
            {
              title: t['slot.table.defaultValueEnable'],
              dataIndex: 'defaultValueEnable',
              width: 180,
              align: 'center',
              render: (col) => t[`slot.table.defaultValueEnable.${col}`],
            },
          ]),
      {
        title: t['slot.table.option'],
        key: 'option',
        width: 90,
        align: 'center',
        render(_, item) {
          return item.blnEdit ? (
            <Space>
              <Tooltip content={t['slot.table.edit']}>
                <div>
                  <SlotModal
                    mode="edit"
                    initialValues={item}
                    onSuccess={refresh}
                  />
                </div>
              </Tooltip>
              <Tooltip content={t['slot.table.delete']}>
                <Button
                  type="text"
                  size="mini"
                  status="danger"
                  icon={<IconDelete />}
                  disabled={!item.blnEdit}
                  onClick={() => handleDeleteSlotItem(item)}
                />
              </Tooltip>
            </Space>
          ) : (
            '-'
          );
        },
      },
    ];
  }, [projectId, refresh, t, type]);

  const tools = useMemo<Tool[]>(
    () => [
      {
        key: 'add',
        component: <SlotModal onSuccess={refresh} />,
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
            {t['slot.table.refresh']}
          </Button>
        ),
      },
    ],
    [loading, refresh, t]
  );
  useTools(tools);
  return (
    <Card size="small" title={t['slot.title']}>
      <Table
        size="small"
        borderCell
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
          total: data?.length,
          defaultPageSize: 20,
          pageSizeChangeResetCurrent: true,
        }}
      />
    </Card>
  );
};

export default Slots;
