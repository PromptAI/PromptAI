import {
  Button,
  Card,
  Form,
  FormInstance,
  Select,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import React, { useMemo, useRef, useState } from 'react';
import { Grid } from '@arco-design/web-react';
import Message from './components/Message';
import { IconSearch, IconSync } from '@arco-design/web-react/icon';
import { getMessageList } from '@/api/rasa';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { Tool, useTools } from '@/components/Layout/tools-context';
import useUrlParams from '../hooks/useUrlParams';
import usePage from '@/hooks/usePage';
import SourceColumn from './components/SourceColumn';
import TimeColumn from '@/components/TimeColumn';
import { useRequest } from 'ahooks';
import { listConversations } from '@/api/components';
import { nanoid } from 'nanoid';

const { Col, Row } = Grid;
const columnMinWidth = 180;

export default function Messagelist() {
  const [msgInfo, setMsgInfo] = useState({ id: '', locale: 'zh' });
  const { projectId } = useUrlParams();
  const t = useLocale(i18n);

  const {
    dataSource = [],
    onPageChange,
    params,
    total,
    loading,
    refresh,
    onSearch,
  } = usePage(getMessageList, { projectId, sort: '_visitTime,desc' });

  const tools = useMemo<Tool[]>(
    () => [
      {
        key: 'refresh',
        component: (
          <Button
            type="text"
            icon={<IconSync />}
            onClick={refresh}
            loading={loading}
          >
            {t['refresh']}
          </Button>
        ),
      },
    ],
    [loading, refresh, t]
  );
  useTools(tools);

  const columns = useMemo<TableColumnProps[]>(
    () => [
      {
        title: t['table.ip'],
        dataIndex: 'properties.ip',
        width: columnMinWidth,
        render: (value) => value || '-',
      },
      {
        title: t['table.sessionTime'],
        dataIndex: 'visitTime',
        width: columnMinWidth,
        render: (_, row) => <TimeColumn row={row} dataIndex="visitTime" />,
      },
      {
        title: t['table.scene'],
        key: 'scene',
        width: columnMinWidth,
        render: (_, row) => t[`table.scene.${row.properties.scene}`],
      },
      {
        title: t['table.variables.username'],
        width: columnMinWidth,
        dataIndex: 'properties.variables.username',
        key: 'variables',
        render: (value) => value || '-',
      },
      {
        title: t['table.evaluates'],
        key: 'evaluates',
        width: columnMinWidth,
        render: (_, row) => {
          if (!row.properties.evaluates) return '-';
          const helpful = row.properties.evaluates.filter(
            (i) => i === 1
          ).length;
          const noHelp = row.properties.evaluates.filter((i) => i === 2).length;
          return (
            <span>
              {!!helpful && <span style={{ color: '#10b981' }}>{helpful}</span>}
              {!!helpful && !!noHelp && ' / '}
              {!!noHelp && <span style={{ color: '#ea580c' }}>{noHelp}</span>}
            </span>
          );
        },
      },
      {
        title: t['table.source'],
        key: 'source',
        width: 3 * columnMinWidth,
        render: (_, item) => <SourceColumn item={item} />,
      },
    ],
    [t]
  );
  const formRef = useRef<FormInstance>();
  const { loading: flowLoading, data = [] } = useRequest(() =>
    listConversations(projectId)
  );
  return (
    <Card size="small" title={t['table.title']}>
      <Row gutter={8}>
        <Col span={16}>
          <Form ref={formRef} layout="inline" style={{ marginBottom: 8 }}>
            <Form.Item label={t['table.scene']} field="scene">
              <Select
                style={{ width: 80 }}
                placeholder={t['search.select.all']}
                allowClear
              >
                <Select.Option value="debug">
                  {t['table.scene.debug']}
                </Select.Option>
                <Select.Option value="publish_db">
                  {t['table.scene.publish_db']}
                </Select.Option>
                <Select.Option value="publish_snapshot">
                  {t['table.scene.publish_snapshot']}
                </Select.Option>
              </Select>
            </Form.Item>
            <Form.Item label={t['table.evaluates']} field="evaluates">
              <Select
                loading={flowLoading}
                style={{ width: 80 }}
                placeholder={t['search.select.all']}
                allowClear
              >
                <Select.Option value="1">
                  {t['search.evaluates.1']}
                </Select.Option>
                <Select.Option value="2">
                  {t['search.evaluates.2']}
                </Select.Option>
              </Select>
            </Form.Item>
            <Form.Item label={t['table.source']} field="rootComponentIds">
              <Select
                loading={flowLoading}
                style={{ width: 128 }}
                placeholder={t['search.select.all']}
                allowClear
              >
                <Select.Option
                  // value="fallback,fallback-talk2bits,fallback-action,fallback-text,fallback-webhook"
                  value="fallback,fallback-action,fallback-text,fallback-webhook"
                  key="fallback"
                >
                  {t['search.type.fallback']}
                </Select.Option>
                <Select.Option value="llm,kbqa" key="LLM">
                  {t['search.type.llm']}
                </Select.Option>
                {data.map(({ id, data: { name } }) => (
                  <Select.Option key={id} value={id}>
                    {name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item>
              <Button
                type="primary"
                icon={<IconSearch />}
                onClick={() => {
                  onSearch(formRef.current.getFieldsValue());
                }}
              >
                {t['search.search']}
              </Button>
            </Form.Item>
          </Form>
          <Table
            size="small"
            loading={loading}
            rowKey="id"
            data={dataSource}
            columns={columns}
            rowClassName={() => 'cursor-pointer'}
            onRow={(item) => ({
              onClick: () =>
                setMsgInfo({
                  id: item.id,
                  locale: item.properties.locale || 'zh',
                }),
            })}
            pagePosition="bl"
            pagination={{
              size: 'mini',
              sizeCanChange: true,
              showTotal: true,
              defaultPageSize: 20,
              pageSizeChangeResetCurrent: true,
              total: total,
              current: params?.page + 1,
              pageSize: params?.size,
              onChange: onPageChange,
            }}
            scroll={{ x: 5 * columnMinWidth }}
          />
        </Col>
        <Col span={8}>
          <Message
            height={'calc(100vh - 250px)'}
            msgInfo={msgInfo}
            key={msgInfo.id + nanoid()}
            background="rgb(var(--gray-2))"
            width={'100%'}
          />
        </Col>
      </Row>
    </Card>
  );
}
