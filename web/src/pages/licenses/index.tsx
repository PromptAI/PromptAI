import { pageLicenses } from '@/api/licenses';
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
import moment from 'moment';
import {
  IconDelete,
  IconDownload,
  IconPlus,
  IconSync,
} from '@arco-design/web-react/icon';
import DeleteLicense from './components/DeleteLicense';
import CreateLicense from './components/CreateLicense';
import InstallCmd from '@/pages/licenses/components/InstallCmd/InstallCmd';

const Container = styled.section`
  width: 100%;
  max-width: 1124px;
  margin: 0 auto;
  margin-top: 32px;
`;

const Licenses = () => {
  const t = useLocale(i18n);
  const { loading, dataSource, refresh } = usePage(pageLicenses);
  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        dataIndex: 'name',
        title: t['table.name'],
      },
      {
        width: 200,
        dataIndex: 'license',
        title: t['table.license'],
        render: (_, row) => (
          <Typography.Text
            copyable
            ellipsis={{ showTooltip: true }}
            style={{ margin: 0 }}
          >
            {row.license}
          </Typography.Text>
        ),
      },
      {
        dataIndex: 'properties.project',
        title: t['table.project'],
        align: 'center',
        render: (_, row) =>
          row.properties.project <= 0 ? 'Unlimited' : row.properties.project,
      },
      {
        dataIndex: 'properties.flowInProject',
        title: t['table.flowInProject'],
        align: 'center',
        render: (_, row) =>
          row.properties.flowInProject <= 0
            ? 'Unlimited'
            : row.properties.flowInProject,
      },
      {
        width: 180,
        dataIndex: 'properties.expireAt',
        title: t['table.expireAt'],
        render: (_, row) => (
          <Typography.Text>
            {moment(Number(row.properties.expireAt)).format(
              'YYYY-MM-DD HH:mm:ss'
            )}
          </Typography.Text>
        ),
      },
      {
        dataIndex: 'used',
        title: t['table.used'],
        align: 'center',
        render: (val) => t[`table.used.${val || false}`],
      },
      {
        dataIndex: 'properties.validDay',
        title: t['table.validDay'],
        align: 'center',
      },
      {
        key: 'op',
        title: t['operation'],
        width: 100,
        align: 'center',
        render: (_, row) => (
          <div>
            <DeleteLicense
              row={row}
              onSuccess={refresh}
              status="danger"
              type="text"
              icon={<IconDelete />}
            />
            <InstallCmd
              disabled={!row.installCmd}
              row={row}
              status="default"
              type="text"
              icon={<IconDownload />}
            />
          </div>
        ),
      },
    ],
    [t, refresh]
  );
  return (
    <Container>
      <Typography.Title heading={3}>{t['title']}</Typography.Title>
      <Typography.Paragraph>{t['subtitle']}</Typography.Paragraph>
      <Divider />

      <div className="flex flex-row gap-2 mb-2 items-center justify-end">
        <CreateLicense type="primary" icon={<IconPlus />} onSuccess={refresh}>
          {t['create']}
        </CreateLicense>

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

export default Licenses;
