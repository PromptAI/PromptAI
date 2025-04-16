import { listToken } from '@/api/tokens';
import usePage from '@/hooks/usePage';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Divider,
  Table,
  TableColumnProps,
  Typography,
} from '@arco-design/web-react';
import React, { useMemo } from 'react';
import styled from 'styled-components';
import { IconSync } from '@arco-design/web-react/icon';
import BuyToken from '@/pages/tokens/BuyToken';
import dayjs from 'dayjs';
import formatMoney from '@/utils/CurrencyUtil';

const Container = styled.section`
  width: 100%;
  max-width: 1124px;
  margin: 0 auto;
  margin-top: 32px;
`;

const Tokens = () => {
  const t = useLocale(i18n);
  const { loading, dataSource, refresh } = usePage(listToken);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        dataIndex: 'properties.rechargeToken',
        title: t['token.recharge.number'],
        render: (_, row) => (
          <Typography.Text
            ellipsis={{ showTooltip: true }}
            style={{ margin: 0 }}
          >
            {Number(row.properties.rechargeToken).toLocaleString()}
          </Typography.Text>
        ),
      },
      {
        dataIndex: 'email',
        title: t['token.recharge.email'],
        align: 'center',
      },
      {
        dataIndex: 'time',
        title: t['token.recharge.time'],
        align: 'center',
        render: (_, row) =>
          dayjs(Number(row.time)).format('YYYY-MM-DD HH:mm:ss'),
      },
      {
        dataIndex: 'payAmount',
        title: t['token.recharge.amount'],
        render: (_, row) => formatMoney(row.payAmount / 100),
      },
      {
        dataIndex: 'totalAmount',
        title: t['token.recharge.total.amount'],
        align: 'center',
        render: (_, row) => formatMoney(row.payAmount / 100),
      },
    ],
    [t]
  );

  return (
    <Container>
      <Typography.Title heading={3}>{'Tokens'}</Typography.Title>
      <Typography.Paragraph>{t['Purchase tokens']}</Typography.Paragraph>
      <Divider />

      <div className={'flex flex-row gap-2 mb-2 items-center justify-center'}>
        <BuyToken type="primary" />
      </div>

      <Divider />

      <div className="flex flex-row gap-2 mb-2 items-center justify-end">
        <Button icon={<IconSync />} onClick={refresh}>
          {t['refresh']}
        </Button>
      </div>

      <Table
        columns={columns}
        data={dataSource}
        size="small"
        rowKey="id"
        loading={loading}
        pagination={false}
      />
    </Container>
  );
};

export default Tokens;
