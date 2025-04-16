import React, { useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Select,
  Space,
  Table,
  TableColumnProps,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import {
  IconCopy,
  IconPlus,
  IconSearch,
  IconSend,
  IconSync,
  IconTags,
  IconUser,
} from '@arco-design/web-react/icon';
import CreateSample from '@/pages/projects/project/sample/CreateSample';
import UpdateSample from '@/pages/projects/project/sample/UpdateSample';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { isEmpty } from 'lodash';
import { getFaqList } from '@/api/faq';
import useTable from '@/hooks/useTable';
import { Tool, useTools } from '@/components/Layout/tools-context';
import Favorites from './Favorites';
import FavoritesCenter from '../conversations/conversation/favorites';
import useUrlParams from '../hooks/useUrlParams';
import Paste from './components/Paste';
import Detail from './components/Detail';
import { Tool as DebugRunTool } from '../components/debug';
import BacthDelete from './components/BacthDelete';
import EnableColumn from './components/columns/EnableColumn';
import ExamplesColumn from './components/columns/ExamplesColumn';
import ResponsesColumn from './components/columns/ResponsesColumn';
import LabelsColumn from './components/columns/LabelsColumn';
import DeleteColumn from './components/columns/DeleteColumn';
import DownloadRasaFile from '../components/DownloadRasaFile';
import Other from './components/tools/Other';
import useSearch from '@/hooks/useSearch';
import DebugRobot from '../components/debug/robot';
import PageContainer from '@/components/PageContainer';
import Liebiao from '@/assets/menu-icon/liebiao.svg';
import { useProjectType } from '@/layout/project-layout/context';
// import useDocumentLinks from '@/hooks/useDocumentLinks';

const COLOR = 'rgb(var(--warning-6))';

const wrapGetFaqList = (params) => {
  const { filter, ...rest } = params;
  return new Promise((resolve, reject) => {
    getFaqList({ ...filter, ...rest })
      .then((response) => {
        resolve({
          contents: response.data.data,
          totalElements: response.data.totalCount,
          faqRoot: response.faqRoot,
        });
      })
      .catch(reject);
  });
};
const normalizeFilter = (value) => {
  if (!value) return undefined;
  const [k, v] = value.split('-');
  return { [k]: v };
};
const formatterFilter = (value) => {
  if (!value) return undefined;
  return Object.entries(value).map(([k, v]) => `${k}-${v}`)[0];
};
const Sample = () => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();

  const type = useProjectType();

  const {
    loading,
    data,
    origin,
    total,
    params,
    setParams,
    onPageChange,
    reset,
    refresh,
  } = useTable(wrapGetFaqList, {
    projectId,
    page: 0,
    size: 50,
  });
  const { form, onSubmit } = useSearch(setParams, reset);

  const faqRootId = useMemo(() => origin?.faqRoot?.id, [origin]);
  const faqErrorCount = useMemo(
    () => Number(origin?.faqRoot?.data?.errorCount ?? '0'),
    [origin]
  );

  const [keys, setKeys] = useState([]);
  const columns: TableColumnProps[] = useMemo(() => {
    return [
      {
        title: t['sample.enable'],
        dataIndex: 'user.data.enable',
        key: 'enable',
        render: (_, row) => <EnableColumn row={row} onSuccess={refresh} />,
        width: 80,
      },
      {
        title: (
          <Space>
            <IconUser />
            <Typography.Text>{t['sample.examples']}</Typography.Text>
          </Space>
        ),
        ellipsis: true,
        width: 340,
        render: (_, row) => <ExamplesColumn row={row} color={COLOR} />,
      },
      {
        title: (
          <Space>
            <IconSend />
            <Typography.Text>{t['sample.responses']}</Typography.Text>
          </Space>
        ),
        key: 'responses',
        render: (_, row) => <ResponsesColumn row={row} color={COLOR} />,
      },
      {
        title: (
          <Space>
            <IconTags />
            <Typography.Text>{t['sample.labels']}</Typography.Text>
          </Space>
        ),
        key: 'labels',
        width: 180,
        render: (_, row) =>
          isEmpty(row?.user?.data?.labels) ? '-' : <LabelsColumn row={row} />,
      },
      {
        title: (
          <Space>
            <Typography.Text>{t['sample.option']}</Typography.Text>
          </Space>
        ),
        align: 'center',
        key: 'options',
        width: 180,
        render: (_, row) => (
          <Space>
            <Tooltip content={t['sample.favorites']}>
              <div>
                <Favorites
                  rootComponentId={faqRootId}
                  componentIds={[row.user.id, row.bot.id]}
                />
              </div>
            </Tooltip>
            <Tooltip content={t['sample.edit']}>
              <UpdateSample row={row} onSuccess={refresh} />
            </Tooltip>
            <Tooltip content={t['sample.copy']}>
              <CreateSample
                trigger={
                  <Button type="text" status="success" icon={<IconCopy />} />
                }
                row={row}
                onSuccess={refresh}
              />
            </Tooltip>
            <DeleteColumn row={row} onSuccess={refresh} />
          </Space>
        ),
      },
    ];
  }, [t, refresh, faqRootId]);

  // const { isRunning, start, startLoading, seed } = useRunTask();
  const tools = useMemo<Tool[]>(() => {
    return [
      !!keys.length && {
        key: 'batch-del',
        component: (
          <BacthDelete
            keys={keys}
            onSuccess={() => {
              refresh();
              setKeys([]);
            }}
          />
        ),
      },
      faqRootId && {
        key: 'run',
        component: <DebugRunTool current={faqRootId} />,
      },
      faqRootId &&
        type === 'rasa' && {
          key: 'download-rasa',
          component: <DownloadRasaFile flowId={faqRootId} />,
        },
      faqRootId && {
        key: 'other',
        component: (
          <Other onSuccess={refresh} current={faqRootId} components={keys} />
        ),
      },
    ];
  }, [refresh, keys, faqRootId, type]);
  useTools(tools);
  // const docs = useDocumentLinks();
  return (
    <FavoritesCenter
      type="faq-root"
      contentStyle={{ top: 100 }}
      triggerStyle={{ right: 100 }}
      extraRender={(frame) => (
        <Paste
          frame={frame}
          projectId={projectId}
          rootComponentId={faqRootId}
          onSuccess={refresh}
        />
      )}
      detailRender={(frame) => <Detail frame={frame} />}
    >
      <PageContainer
        title={
          <Space>
            <Liebiao className="icon-size" />
            {t['sample.title']}
            {faqErrorCount > 0 && (
              <Typography.Text style={{ color: COLOR }}>
                {t['sample.error.count']}: {faqErrorCount}
              </Typography.Text>
            )}
          </Space>
        }
      >
        <Form
          layout="inline"
          form={form}
          onSubmit={onSubmit}
          style={{ justifyContent: 'flex-end', marginBottom: 16 }}
        >
          <Form.Item label={t['sample.q']} field="q">
            <Input
              placeholder={t['sample.q.placeholder']}
              style={{ width: 320 }}
              allowClear
            />
          </Form.Item>
          <Form.Item
            field="filter"
            formatter={formatterFilter}
            normalize={normalizeFilter}
          >
            <Select
              placeholder={t['sample.filter.all.faq']}
              allowClear
              style={{ width: 160 }}
            >
              <Select.Option value="enable-true">
                {t['sample.filter.enable']}
              </Select.Option>
              <Select.Option value="enable-false">
                {t['sample.filter.disable']}
              </Select.Option>
              <Select.Option value="hasError-true">
                {t['sample.filter.error.faq']}
              </Select.Option>
            </Select>
          </Form.Item>
          <Form.Item>
            <Button icon={<IconSync />} onClick={refresh}>
              {t['sample.refresh']}
            </Button>
          </Form.Item>
          <Form.Item>
            <Button
              loading={loading}
              htmlType="submit"
              type="primary"
              icon={<IconSearch />}
            >
              {t['sample.filter.search']}
            </Button>
          </Form.Item>
          <Form.Item>
            <CreateSample
              trigger={
                <Button type="primary" icon={<IconPlus />}>
                  {t['sample.buttonText']}
                </Button>
              }
              onSuccess={refresh}
            />
          </Form.Item>
        </Form>
        <Table
          size="small"
          border={{
            cell: true,
          }}
          rowKey={(record) => record.user.id}
          loading={loading}
          data={data}
          columns={columns}
          pagePosition="bl"
          pagination={{
            total,
            current: params?.page + 1,
            pageSize: params?.size,
            onChange: onPageChange,
            sizeCanChange: true,
            showTotal: true,
            size: 'mini',
            sizeOptions: [50, 100, 200, 300],
          }}
          rowSelection={{
            type: 'checkbox',
            checkAll: true,
            onChange: setKeys,
          }}
          // renderPagination={(dom) => (
          //   <div className="flex just-between align-center flex-wrap mt-16">
          //     {dom}
          //     <Typography.Text>
          //       <IconCheckCircle />
          //       {t['sample.filter.talk2bits.help']}
          //       <Link target="_blank" href={docs.knowledgeBase}>
          //         {t['sample.filter.talk2bits.help.document']}
          //       </Link>
          //     </Typography.Text>
          //   </div>
          // )}
        />
        {faqRootId && (
          <DebugRobot
            current={faqRootId}
            // openSeed={seed}
            projectId={projectId}
          />
        )}
      </PageContainer>
    </FavoritesCenter>
  );
};
export default Sample;
