import i18n from './locale';
import useLocale from '@/utils/useLocale';
import {
    Button,
    Card,
    Form,
    Input,
    List,
    Message,
    Space,
    Tag,
    Tooltip,
    Trigger,
    Typography,
} from '@arco-design/web-react';
import React, {useEffect, useRef, useState} from 'react';
import {IconSync} from '@arco-design/web-react/icon';
import useRules from '@/hooks/useRules';
import useForm from '@arco-design/web-react/es/Form/useForm';
import {RefInputType} from '@arco-design/web-react/es/Input/interface';
import useUrlParams from '../../../hooks/useUrlParams';
import {syncWebLib, updateContent, updateWebLib} from '@/api/text/web';
import TimeColumn from '@/components/TimeColumn';
import {updateLibDesc} from '@/api/text/text';
import {isEmpty} from 'lodash';

interface ExpandProps {
    row: any;
    onSuccess: () => void;
}

const status = {
    ok: 'green',
    fail: 'red',
};
const ExpandRow = ({row, onSuccess}: ExpandProps) => {
    const t = useLocale(i18n);
    const {projectId} = useUrlParams();
    const rules = useRules();
    const [editable, setEditable] = useState(false);

    const [content, setContent] = useState(row.data.content)
    const [saving, setSaving] = useState(false);

    const [form] = useForm();
    useEffect(() => {
        form.setFieldsValue(row);
    }, [form, row]);
    const ref = useRef<RefInputType>();
    const [, setLoading] = useState(false);
    const onSubmit = (values) => {
        setLoading(true);
        updateWebLib(projectId, row.id, {...row.data, ...values.data})
            .then(() => {
                Message.success(t['expand.update.success']);
                onSuccess();
                setEditable(false);
            })
            .finally(() => setLoading(false));
    };
    const [syncing, setSyncing] = useState(false);
    const onSync = () => {
        setSyncing(true);
        syncWebLib(projectId, row.id)
            .then(() => {
                Message.success(t['expand.sync.success']);
                onSuccess();
            })
            .finally(() => setSyncing(false));
    };

    const cancelEdit = () => {
        setEditable(false);
        setContent(row.data.content);
    }
    const onSave = () => {
        if (content.length > 0) {
            const data = {
                "id": row.id,
                content: content,
            }

            setSaving(true);
            updateContent(projectId, data).then(() => {
                Message.success(t['expand.sync.success']);
                onSuccess();
            }).finally(() => setSaving(false))

        }
    }
    return (
        <Form layout="vertical" initialValues={row} form={form} onSubmit={onSubmit}>
            <Card
                title={t['expand.title']}
                extra={
                    <Space>
                        <Tag color={status[row.data.status]}>
                            {t['expand.sync.status']}
                            {row.data.status || '-'}
                        </Tag>
                        <Tag>
                            {t['expand.sync.time']}
                            <TimeColumn
                                row={row}
                                dataIndex="data.timestamp"
                                style={{marginLeft: 4}}
                            />
                        </Tag>
                        <Trigger
                            popup={() => (
                                <Card size="small" bodyStyle={{padding: 4}}>
                                    <List
                                        size="small"
                                        dataSource={row.data.asyncRecord || []}
                                        bordered={false}
                                        virtualListProps={{height: '400px'}}
                                        render={(item, index) => (
                                            <List.Item key={index}>
                                                <Space>
                                                    <Tooltip
                                                        disabled={item.status === 'ok'}
                                                        content={item.reason?.responseBody}
                                                    >
                                                        <Tag
                                                            color={status[item.status]}
                                                            style={{cursor: 'pointer'}}
                                                        >
                                                            {t['expand.sync.status']}
                                                            {item.status || '-'}
                                                        </Tag>
                                                    </Tooltip>
                                                    <Tag>
                                                        {t['expand.sync.time']}
                                                        <TimeColumn
                                                            row={item}
                                                            dataIndex="timestamp"
                                                            style={{marginLeft: 4}}
                                                        />
                                                    </Tag>
                                                </Space>
                                            </List.Item>
                                        )}
                                    />
                                </Card>
                            )}
                            trigger="hover"
                            position="bottom"
                        >
                            <Tag color="arcoblue">{t['expand.sync.history']}</Tag>
                        </Trigger>
                        <Button
                            loading={syncing}
                            type="primary"
                            status="success"
                            icon={<IconSync/>}
                            onClick={onSync}
                            disabled={row.data.status === 'ok'}
                        >
                            {t['expand.sync']}
                        </Button>
                    </Space>
                }
            >
                <Form.Item
                    label={t['data.remark']}
                    field="data.description"
                    //  rules={rules}
                >
                    <EditableRemarkRow
                        projectId={projectId}
                        componentId={row.id}
                        refresh={onSuccess}
                    />
                </Form.Item>
                <Form.Item label={t['expand.url']} field="data.url" rules={rules}>
                    <Input
                        ref={ref}
                        onClick={() => {
                            window.open(row.data.url, '_blank');
                        }}
                        readOnly={!editable}
                        placeholder={t['expand.url.placeholder']}
                        className={
                            !editable && 'cursor-pointer !border-transparent hover:underline'
                        }
                    />
                </Form.Item>
                <Form.Item
                    label={t['expand.content']}
                    field="data.content"
                    rules={rules}
                >
                    <div className={'flex justify-end  pb-2'}>
                        <Button
                            type={'primary'}
                            loading={saving}
                            style={{marginRight: 8}} onClick={() => {onSave()}}>
                            {t['expand.save']}
                        </Button>
                        <Button type={'secondary'} onClick={cancelEdit}> {t['expand.cancel']}</Button>
                    </div>
                    <Input.TextArea
                        value={content}
                        onChange={(value) => {
                            setContent(value)
                        }}
                        autoSize={{minRows: 6, maxRows: 12}}
                        placeholder={t['expand.content.placeholder']}
                    />
                </Form.Item>
            </Card>
        </Form>
    );
};

interface EditableRemarkRowProps {
    value?: string;
    onChange?: (val: string) => void;
    projectId: string;
    componentId: string;
    refresh?: () => void;
}

const EditableRemarkRow = ({
                               value = '',
                               onChange,
                               projectId,
                               componentId,
                               refresh,
                           }: EditableRemarkRowProps) => {
    const initValueRef = useRef(value);
    const onEnd = async (text: string) => {
        if (isEmpty(text.trim())) {
            onChange?.('');
            return;
        }
        if (initValueRef.current !== text.trim()) {
            await updateLibDesc({desc: text.trim(), projectId, componentId});
            initValueRef.current = text.trim();
            refresh();
        }
    };
    return (
        <Typography.Paragraph style={{margin: 0}} editable={{onChange, onEnd}}>
            {value}
        </Typography.Paragraph>
    );
};
export default ExpandRow;
