import { getFavoritesFlowList } from '@/api/favorites';
import usePage from '@/hooks/usePage';
import useLocale from '@/utils/useLocale';
import {
  Card,
  FormInstance,
  Space,
  Table,
  TableColumnProps,
  Typography,
} from '@arco-design/web-react';
import { IconStarFill } from '@arco-design/web-react/icon';
import { keyBy } from 'lodash';
import moment from 'moment';
import React, { useCallback, useMemo, useRef } from 'react';
import { titleHandlerMapping } from '../../conversations/conversation/trash/context';
import UnFavorites from '../components/UnFavorites';
import Detail from './components/Detail';
import NodeCountColumn from './components/NodeCountColumn';
import RootTypeColumn from './components/RootTypeColumn';
import i18n from './locale';

const expandedRowRender = (record) => <Detail key={record.id} {...record} />;
const Flows = () => {
  const t = useLocale(i18n);
  const {
    loading,
    dataSource,
    params,
    total,
    refresh,
    onSearch,
    onPageChange,
  } = usePage(getFavoritesFlowList);

  const data = useMemo(() => {
    if (!dataSource) return [];
    return dataSource.map((d) => {
      const {
        id,
        properties: { data: nodes, createTime },
      } = d;
      const idMap = keyBy(nodes, 'id');
      const root = nodes.find((n) => !idMap[n.parentId]);
      return {
        id,
        nodes,
        createTime: moment(Number(createTime)).format('yyyy-MM-DD HH:mm:ss'),
        rootLabel: titleHandlerMapping[root?.type]?.(root?.data) || '-',
        rootType: root?.type,
      };
    });
  }, [dataSource]);

  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['favorites.flow.rootLabel'],
        dataIndex: 'rootLabel',
        render: (value, item) => (
          <Space>
            <RootTypeColumn item={item} />
            <Typography.Text>{value}</Typography.Text>
          </Space>
        ),
      },
      {
        title: t['favorites.flow.nodeCount'],
        key: 'nodeCount',
        align: 'center',
        width: 120,
        render: (_, item) => <NodeCountColumn item={item} />,
      },
      { title: t['favorites.flow.createTime'], dataIndex: 'createTime' },
      {
        title: t['favorites.flow.optional'],
        key: 'op',
        align: 'center',
        width: 100,
        render: (_, item) => (
          <Space>
            <UnFavorites item={item} onSucces={refresh} />
          </Space>
        ),
      },
    ],
    [refresh, t]
  );

  const formRef = useRef<FormInstance>();
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const onReset = useCallback(() => {
    formRef.current.resetFields();
    onSearch();
  }, [onSearch]);

  return (
    <Card
      title={
        <Space>
          <IconStarFill className="favorites-icon" />
          <Typography.Text>{t['favorites.flow.table.title']}</Typography.Text>
        </Space>
      }
      headerStyle={{ height: 54 }}
      // extra={
      //   <Form layout="inline" ref={formRef}>
      //     <Form.Item
      //       label={t['favorites.flow.name']}
      //       field="name"
      //       style={{ marginBottom: 0 }}
      //     >
      //       <Input placeholder={t['favorites.flow.name.placeholder']} />
      //     </Form.Item>
      //     <Form.Item
      //       label={t['favorites.flow.content']}
      //       field="content"
      //       style={{ marginBottom: 0 }}
      //     >
      //       <Input placeholder={t['favorites.flow.content.placeholder']} />
      //     </Form.Item>
      //     <Form.Item style={{ marginBottom: 0 }}>
      //       <Space>
      //         <Button type="secondary" icon={<IconClose />} onClick={onReset}>
      //           {t['favorites.flow.reset']}
      //         </Button>
      //         <Button
      //           loading={loading}
      //           htmlType="submit"
      //           type="primary"
      //           icon={<IconSearch />}
      //           onClick={() => onSearch(formRef.current.getFieldsValue())}
      //         >
      //           {t['favorites.flow.search']}
      //         </Button>
      //       </Space>
      //     </Form.Item>
      //   </Form>
      // }
    >
      <Table
        size="small"
        border={{ cell: true }}
        rowKey="id"
        loading={loading}
        data={data}
        columns={columns}
        expandProps={{ expandRowByClick: true }}
        expandedRowRender={expandedRowRender}
        pagination={{
          total,
          current: params?.page + 1,
          pageSize: params?.size,
          onChange: onPageChange,
          sizeCanChange: true,
          showTotal: true,
          size: 'mini',
        }}
      />
    </Card>
  );
};

export default Flows;
