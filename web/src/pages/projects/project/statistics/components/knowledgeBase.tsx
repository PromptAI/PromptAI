import * as React from 'react';
import { SummaryWithDateRangeProps } from '../types';
import useUrlParams from '../../hooks/useUrlParams';
import { useRequest } from 'ahooks';
import { IParamsWithPage, knowledgeBase } from '@/api/statistics';
import { Button, Card, Modal, Table, TableProps } from '@arco-design/web-react';
import dayjs from 'dayjs';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { useMemo } from 'react';

interface knowledgeBaseProps extends SummaryWithDateRangeProps {}

const KnowledgeBase: React.FC<knowledgeBaseProps> = ({
  startTime,
  endTime,
}) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [pageParams, setPageParams] = React.useState<
    Pick<IParamsWithPage, 'page' | 'size'>
  >({ page: 0, size: 5 });
  const { loading, data } = useRequest(
    () => knowledgeBase({ projectId, startTime, endTime, ...pageParams }),
    {
      refreshDeps: [pageParams, startTime, endTime, projectId],
    }
  );
  const KB_TYPE = useMemo(
    () => ({
      undefined: { text: t['knowledgeBase.component.type.unknown'] },
      kb_url: { text: t['knowledgeBase.component.type.url'] },
      kb_file: { text: t['knowledgeBase.component.type.file'] },
      kb_text: { text: t['knowledgeBase.component.type.text'] },
    }),
    [t]
  );
  const columns = React.useMemo<TableProps['columns']>(
    () => [
      {
        dataIndex: 'messageId',
        title: t['knowledgeBase.table.messageId'],
      },
      {
        dataIndex: 'chatId',
        title: t['knowledgeBase.table.chatId'],
      },
      { dataIndex: 'query', title: t['knowledgeBase.table.query'] },
      {
        dataIndex: 'name',
        title: t['knowledgeBase.table.name'],
        ellipsis: true,
        render: (_, row) => {
          if (row.type == 'kb-url') {
            return (
              <a
                href={row.name}
                target={'_blank'}
                rel="noreferrer"
                className={'truncate hover:underline'}
                title={row.name}
              >
                {row.name}
              </a>
            );
          }

          return row.name;
        },
      },
      {
        dataIndex: 'type',
        title: t['knowledgeBase.table.type'],
        render: (_, row) => {
          const type = row.type;
          switch (type) {
            case 'kb-url':
              return KB_TYPE.kb_url.text;
            case 'kb-file':
              return KB_TYPE.kb_url.text;
            case 'kb-text':
              return KB_TYPE.kb_url.text;
            default:
              return KB_TYPE.kb_url.text;
          }
        },
      },
      {
        dataIndex: 'time',
        title: t['knowledgeBase.table.time'],
        render: (_, row) =>
          dayjs(Number(row.time)).format('YYYY-MM-DD HH:mm:ss'),
      },
    ],
    [t, KB_TYPE]
  );
  const [visible, setVisible] = React.useState(false);
  React.useEffect(() => {
    if (visible) {
      setPageParams({ page: 0, size: 5 });
    }
  }, [visible]);
  return (
    <Card
      title={t['knowledgeBase.title']}
      size="small"
      extra={
        <Button size="small" type="text" onClick={() => setVisible(true)}>
          {t['more']}
        </Button>
      }
    >
      <Table
        size="small"
        border={false}
        loading={loading}
        rowKey="messageId"
        columns={columns}
        data={data?.data}
        pagination={false}
      />
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        title={t['more']}
        unmountOnExit
        footer={(cancel) => cancel}
        style={{ width: '50%' }}
      >
        <Table
          size="small"
          border={{
            cell: true,
            wrapper: true,
          }}
          loading={loading}
          data={data?.data}
          columns={columns}
          pagePosition="bl"
          pagination={{
            total: data?.totalCount || 0,
            current: pageParams.page + 1,
            pageSize: pageParams.size,
            onChange: (page, size) =>
              setPageParams({ page: page - 1 < 0 ? 0 : page - 1, size }),
            sizeCanChange: true,
            showTotal: true,
            size: 'mini',
            sizeOptions: [50, 100, 200, 300],
          }}
        />
      </Modal>
    </Card>
  );
};

export default KnowledgeBase;
