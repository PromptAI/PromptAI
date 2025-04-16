import React, { useCallback, useMemo, useRef, useState } from 'react';
import {
  Button,
  Card,
  Checkbox,
  Form,
  FormInstance,
  Input,
  Message,
  Popconfirm,
  Select,
  Space,
  Table,
  TableColumnProps,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { getList, update, del } from '@/api/synonyms';
import usePage from '@/hooks/usePage';
import DrawerForm from './DrawerForm';
import {
  IconCopy,
  IconDelete,
  IconSearch,
  IconPlus,
  IconSync,
  IconEdit,
} from '@arco-design/web-react/icon';
import { useParams } from 'react-router';
import { type Tool, useTools } from '@/components/Layout/tools-context';

const defaultPageSearch = { page: 0, size: 10 };

function Synonyms() {
  const { id: projectId } = useParams<{ id: string }>();
  const t = useLocale(i18n);
  const [enableDisplay, setEnabled] = useState(false);

  const { dataSource, loading, refresh, onSearch } = usePage(getList, {
    ...defaultPageSearch,
    projectId,
  });

  const onEnable = useCallback(
    (c, item) => {
      setEnabled(true);
      update({
        ...item,
        data: {
          ...item.data,
          enable: c,
        },
      })
        .then(() => {
          refresh();
          Message.success(
            c ? t['sample.enable.success'] : t['sample.enable.delete']
          );
        })
        .finally(() => {
          setEnabled(false);
        });
    },
    [refresh, t]
  );
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleDelete = ({ id }) => del({ projectId, id }).then(refresh);

  const tools = useMemo<Tool[]>(() => {
    return [
      {
        key: 'add',
        component: (
          <DrawerForm
            projectId={projectId}
            Trigger={
              <Button type={'text'} icon={<IconPlus />}>
                {t['synonyms.add']}
              </Button>
            }
            callback={refresh}
          />
        ),
      },
      {
        key: 'refresh',
        component: (
          <Button type={'text'} icon={<IconSync />} onClick={refresh}>
            {t['synonyms.refresh']}
          </Button>
        ),
      },
    ];
  }, [projectId, t, refresh]);

  useTools(tools);

  const columns: TableColumnProps[] = useMemo(
    () => [
      {
        title: t['synonyms.filter.enable'],
        dataIndex: 'data.enable',
        key: 'enable',
        render(col, item) {
          return (
            <Checkbox
              disabled={enableDisplay}
              checked={col}
              onChange={(c) => {
                onEnable(c, item);
              }}
            />
          );
        },
        width: 80,
      },
      {
        title: <Typography.Text>{t['synonyms.original']}</Typography.Text>,
        ellipsis: true,
        dataIndex: 'data.original',
        key: 'data.original',
        render(text) {
          return (
            <Tooltip position={'tl'} content={text || '-'}>
              {text || '-'}
            </Tooltip>
          );
        },
      },
      {
        title: <Typography.Text>{t['synonyms.synonyms']}</Typography.Text>,
        dataIndex: 'data.synonyms',
        key: 'data.synonyms',
        render(synonyms) {
          return synonyms && synonyms.length
            ? synonyms.map((o) => <div key={o}>{o}</div>)
            : '-';
        },
      },
      {
        title: (
          <Space>
            <Typography.Text>{t['synonyms.option']}</Typography.Text>
          </Space>
        ),
        align: 'center',
        key: 'options',
        width: 140,
        render(clo, item) {
          return (
            <Space>
              <Tooltip content={t['synonyms.edit']}>
                <DrawerForm
                  projectId={projectId}
                  initialValue={item}
                  callback={refresh}
                  Trigger={
                    <Tooltip content={t['sample.edit']}>
                      <Button size="small" type="text" icon={<IconEdit />} />
                    </Tooltip>
                  }
                />
              </Tooltip>
              <Tooltip content={t['synonyms.copy']}>
                <DrawerForm
                  projectId={projectId}
                  Trigger={
                    <Button
                      type={'text'}
                      status={'success'}
                      icon={<IconCopy />}
                    />
                  }
                  initialValue={{ ...item, id: '' }}
                  callback={refresh}
                />
              </Tooltip>
              <Tooltip content={t['synonyms.delete']}>
                <Popconfirm
                  title={`${t['synonyms.delete.placeholder']}`}
                  onOk={() => handleDelete(item)}
                  position={'lt'}
                >
                  <Button
                    size="small"
                    type="text"
                    status="danger"
                    icon={<IconDelete />}
                  />
                </Popconfirm>
              </Tooltip>
            </Space>
          );
        },
      },
    ],
    [enableDisplay, handleDelete, onEnable, projectId, refresh, t]
  );
  const formRef = useRef<FormInstance>();

  return (
    <Card
      size="small"
      title={t['synonyms.title']}
      className="card-extra"
      extra={
        <Form layout="inline" ref={formRef}>
          <Form.Item label={t['synonyms.original']} field="original">
            <Input
              placeholder={t['synonyms.original.placeholder']}
              allowClear
            />
          </Form.Item>
          <Form.Item label={t['synonyms.filter.enable']} field="enable">
            <Select
              style={{ width: 100 }}
              placeholder={t['synonyms.filter.all']}
              allowClear
            >
              <Select.Option value="true">
                {t['synonyms.filter.enable']}
              </Select.Option>
              <Select.Option value="false">
                {t['synonyms.filter.disable']}
              </Select.Option>
            </Select>
          </Form.Item>
          <Form.Item>
            <Button
              loading={loading}
              htmlType="submit"
              type="primary"
              icon={<IconSearch />}
              onClick={() => onSearch(formRef.current.getFieldsValue())}
            >
              {t['synonyms.filter.search']}
            </Button>
          </Form.Item>
        </Form>
      }
    >
      <Table
        size="small"
        border={{
          cell: true,
        }}
        style={{ height: '100%', width: '100%' }}
        rowKey={(record) => record.id}
        loading={loading}
        data={dataSource}
        columns={columns}
        pagination={{
          size: 'mini',
          sizeCanChange: true,
          showTotal: true,
          total: dataSource?.length || 0,
          defaultPageSize: 20,
          pageSizeChangeResetCurrent: true,
        }}
      />
    </Card>
  );
}

export default Synonyms;
