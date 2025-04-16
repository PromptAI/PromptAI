import React, {useMemo, useState} from 'react';
import {
    Button,
    Form,
    Input,
    Message, Popconfirm,
    Space,
    Table,
    TableColumnProps,
    Tooltip,
    Typography,
} from '@arco-design/web-react';
import {IconDelete, IconPlus, IconSearch, IconSync,} from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import {isEmpty} from 'lodash';
import useTable from '@/hooks/useTable';
import {Tool, useTools} from '@/components/Layout/tools-context';
import useUrlParams from '../hooks/useUrlParams';
import BacthDelete from '../sample/components/BacthDelete';
import useSearch from '@/hooks/useSearch';
import PageContainer from '@/components/PageContainer';
import Liebiao from '@/assets/menu-icon/liebiao.svg';
import {useProjectType} from '@/layout/project-layout/context';
import CreateGpt from "@/pages/projects/project/gpt/CreateGpt";
import UpdateGpt from "@/pages/projects/project/gpt/UpdateGpt";
import {deleteComponent, getAgents} from "@/api/components";

const wrapGetAgentList = (params) => {
    const {filter, ...rest} = params;
    return new Promise((resolve, reject) => {
        getAgents({...filter, ...rest})
            .then((response) => {
                resolve({
                    contents: response.contents,
                    totalElements: response.totalElements,
                });
            })
            .catch(reject);
    });
};

const DeleteColumn = ({ row, onSuccess }) => {
    const t = useLocale(i18n);
    const { projectId } = useUrlParams();
    const [loading, setLoading] = useState(false);
    const deleteRow = (row) => {
        setLoading(true);
        deleteComponent(projectId, [row?.id])
            .then(() => {
                Message.success(t['sample.delete.success']);
                onSuccess();
            })
            .finally(() => setLoading(false));
    };
    return (
        <Tooltip content={t['gpt.delete.tooltip']}>
            <Popconfirm
                title={`${t['gpt.delete.title']}`}
                onOk={() => deleteRow(row)}
                position="lt"
            >
                <Button
                    loading={loading}
                    size="small"
                    type="text"
                    status="danger"
                    icon={<IconDelete />}
                />
            </Popconfirm>
        </Tooltip>
    );
};
const Gpt = () => {
    const t = useLocale(i18n);
    const {projectId} = useUrlParams();

    const type = useProjectType();

    const {
        loading,
        data,
        total,
        params,
        setParams,
        onPageChange,
        reset,
        refresh,
    } = useTable(wrapGetAgentList, {
        projectId,
        page: 0,
        size: 50,
    });
    const {form, onSubmit} = useSearch(setParams, reset);

    const [keys, setKeys] = useState([]);
    const columns: TableColumnProps[] = useMemo(() => {
        return [
            {
                title: (
                    <Space>
                        <Typography.Text>{t['gpt.name']}</Typography.Text>
                    </Space>
                ),
                ellipsis: true,
                width: 340,
                render: (_, row) => {
                   return  isEmpty(row?.data?.name) ? '-' : row?.data?.name
                } ,
            },
            {
                title: (
                    <Space>
                        <Typography.Text>{t['gpt.prompt']}</Typography.Text>
                    </Space>
                ),
                key: 'data.prompt',
                render: (_, row) =>  isEmpty(row?.data?.prompt) ? '-' : row?.data?.prompt ,
            },
            {
                title: (
                    <Space>
                        <Typography.Text>{t['gpt.description']}</Typography.Text>
                    </Space>
                ),
                key: 'data.description',
                width: 180,
                render: (_, row) =>
                    isEmpty(row?.data?.description) ? '-' : row?.data?.description ,
            },

            {
                title: (
                    <Space>
                        <Typography.Text>{t['gpt.option']}</Typography.Text>
                    </Space>
                ),
                align: 'center',
                key: 'options',
                width: 180,
                render: (_, row) => (
                    <Space>
                        <Tooltip content={t['gpt.edit']}>
                            <UpdateGpt row={row} onSuccess={refresh}/>
                        </Tooltip>
                        <DeleteColumn row={row} onSuccess={refresh}/>
                    </Space>
                ),
            },
        ];
    }, [t, refresh]);

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
        ];
    }, [refresh, keys, type]);
    useTools(tools);
    // const docs = useDocumentLinks();
    return (
        <PageContainer
            title={
                <Space>
                    <Liebiao className="icon-size"/>
                    {t['gpt.title']}
                </Space>
            }
        >
            <Form
                layout="inline"
                form={form}
                onSubmit={onSubmit}
                style={{justifyContent: 'flex-end', marginBottom: 16}}
            >
                <Form.Item label={t['gpt.filter']} field="query">
                    <Input
                        placeholder={t['gpt.filter.placeholder']}
                        style={{width: 320}}
                        allowClear
                    />
                </Form.Item>
                <Form.Item>
                    <Button icon={<IconSync/>} onClick={refresh}>
                        {t['gpt.refresh']}
                    </Button>
                </Form.Item>
                <Form.Item>
                    <Button
                        loading={loading}
                        htmlType="submit"
                        type="primary"
                        icon={<IconSearch/>}
                    >
                        {t['gpt.search']}
                    </Button>
                </Form.Item>
                <Form.Item>
                    <CreateGpt
                        trigger={
                            <Button type="primary" icon={<IconPlus/>}>
                                {t['gpt.add']}
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
                rowKey={(record) => record.id}
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

            />
        </PageContainer>
    );
};
export default Gpt;
