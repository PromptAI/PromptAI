import { getFavoritesFaqList } from '@/api/favorites';
import usePage from '@/hooks/usePage';
import useLocale from '@/utils/useLocale';
import {
  Card,
  Space,
  Table,
  TableColumnProps,
  Typography,
} from '@arco-design/web-react';
import { IconStarFill } from '@arco-design/web-react/icon';
import { keyBy } from 'lodash';
import React, { useMemo } from 'react';
import BotColumn from './components/BotColumn';
import Detail from './components/Detail';
import EnableColumn from './components/EnableColumn';
import LabelColumn from './components/LabelColumn';
import UnFavorites from '../components/UnFavorites';
import UserColumn from './components/UserColumn';
import i18n from './locale';

const Faqs = () => {
  const t = useLocale(i18n);
  const { loading, dataSource, total, params, refresh, onPageChange } =
    usePage(getFavoritesFaqList);

  const data = useMemo(() => {
    if (!dataSource) return [];
    return dataSource.map((d) => {
      const {
        id,
        properties: { data: items },
      } = d;
      const group = keyBy(items, 'type');
      return {
        id,
        user: group['user'],
        bot: group['bot'],
      };
    });
  }, [dataSource]);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['favorites.faq.enable'],
        key: 'enable',
        width: 80,
        render: (_, item) => <EnableColumn item={item} />,
      },
      {
        title: t['favorites.faq.user'],
        key: 'user',
        render: (_, item) => <UserColumn item={item} />,
      },
      {
        title: t['favorites.faq.bot'],
        key: 'bot',
        render: (_, item) => <BotColumn item={item} />,
      },
      {
        title: t['favorites.faq.labels'],
        key: 'labels',
        render: (_, item) => (
          <LabelColumn
            item={item}
            emptyTitle={t['favorites.faq.labelsEmpty']}
          />
        ),
      },
      {
        title: t['favorites.faq.optional'],
        key: 'op',
        align: 'center',
        width: 80,
        render: (_, item) => {
          return (
            <Space>
              <Detail row={item} />
              <UnFavorites item={item} onSucces={refresh} />
            </Space>
          );
        },
      },
    ],
    [refresh, t]
  );
  // const formRef = useRef<FormInstance>();
  // const onReset = useCallback(() => {
  //   formRef.current.resetFields();
  //   onSearch();
  // }, [onSearch]);
  return (
    <Card
      title={
        <Space>
          <IconStarFill className="favorites-icon" />
          <Typography.Text>{t['favorites.faq.table.title']}</Typography.Text>
        </Space>
      }
      headerStyle={{ height: 54 }}
      // extra={
      //   <Form layout="inline" ref={formRef}>
      //     <Form.Item
      //       label={t['favorites.faq.user']}
      //       field="example"
      //       style={{ marginBottom: 0 }}
      //     >
      //       <Input placeholder={t['favorites.faq.user.placeholder']} />
      //     </Form.Item>
      //     <Form.Item
      //       label={t['favorites.faq.labels']}
      //       field="label"
      //       style={{ marginBottom: 0 }}
      //     >
      //       <Input placeholder={t['favorites.faq.labels.placeholder']} />
      //     </Form.Item>
      //     <Form.Item
      //       label={t['favorites.faq.enable']}
      //       field="enable"
      //       style={{ marginBottom: 0 }}
      //     >
      //       <Select placeholder={t['favorites.faq.enable.all']} allowClear>
      //         <Select.Option value="true">
      //           {t['favorites.faq.enable.enable']}
      //         </Select.Option>
      //         <Select.Option value="false">
      //           {t['favorites.faq.enable.disable']}
      //         </Select.Option>
      //       </Select>
      //     </Form.Item>
      //     <Form.Item style={{ marginBottom: 0 }}>
      //       <Space>
      //         <Button type="secondary" icon={<IconClose />} onClick={onReset}>
      //           {t['favorites.faq.reset']}
      //         </Button>
      //         <Button
      //           loading={loading}
      //           htmlType="submit"
      //           type="primary"
      //           icon={<IconSearch />}
      //           onClick={() => onSearch(formRef.current.getFieldsValue())}
      //         >
      //           {t['favorites.faq.search']}
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

export default Faqs;
