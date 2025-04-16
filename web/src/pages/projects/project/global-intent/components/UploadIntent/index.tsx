import DownloadTemplate from '@/components/DownloadTemplate';
import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Grid,
  List,
  Modal,
  Space,
  Typography,
  Upload,
} from '@arco-design/web-react';
import { UploadItem } from '@arco-design/web-react/es/Upload';
import { IconCheck, IconClose, IconUpload } from '@arco-design/web-react/icon';
import React, { useEffect, useMemo, useState } from 'react';
import useUrlParams from '../../../hooks/useUrlParams';
import i18n from './locale';

const templates = [
  'global-user.txt',
  'global-user.json',
  'global-user.xls',
  'global-user.xlsx',
].map((name) => (
  <DownloadTemplate key={name} name={name} type="text" size="small">
    {name.split('.')[1]}
  </DownloadTemplate>
));
const defaultResult = { error: false, msgs: [] };
const UploadIntent = ({ onSuccess }) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const params = useUrlParams();
  const headers = useMemo(() => ({ Authorization: Token.get() }), []);
  const [result, setResult] = useState(defaultResult);
  useEffect(() => {
    if (visible) {
      setResult(defaultResult);
    }
  }, [visible]);
  const onUploadFileChange = (fileList: UploadItem[], file: UploadItem) => {
    if (file.status === 'done' && fileList?.length) {
      const response = file.response as any;
      let re;
      if (response?.failCount && Number(response.failCount) > 0) {
        re = {
          error: true,
          msgs: Object.entries(response?.name2Detail || {}).map(([k, v]) => ({
            key: k,
            value: v,
          })),
        };
      } else {
        re = {
          error: false,
          msgs: [{ key: '', value: t['upload.intent.success'] }],
        };
        onSuccess();
      }
      setResult(re);
    }
    if (file.status === 'error') {
      file.response = (file.response as any)?.message || 'error';
    }
  };
  return (
    <div>
      <Button
        type="text"
        icon={<IconUpload />}
        onClick={() => setVisible(true)}
      >
        {t['upload.intent']}
      </Button>
      <Modal
        title={t['upload.intent']}
        style={{ width: '55%' }}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={() => setVisible(false)}
        unmountOnExit
      >
        <Grid.Row gutter={12}>
          <Grid.Col span={12}>
            <Grid.Row style={{ minHeight: 241 }}>
              <Upload
                drag
                action="/api/project/component/user/global/import"
                headers={headers}
                data={params}
                multiple={false}
                limit={1}
                accept=".txt,.json,.xls,.xlsx"
                tip="txt,json,xls,xlsx"
                onChange={onUploadFileChange}
                onRemove={() => setResult({ error: false, msgs: [] })}
              />
            </Grid.Row>
            <Grid.Row>
              <Space size="small">
                <Typography.Text>{t['upload.intent.template']}</Typography.Text>
                {templates}
              </Space>
            </Grid.Row>
          </Grid.Col>
          <Grid.Col span={12}>
            <Grid.Row style={{ marginBottom: 12 }}>
              <Typography.Text>{t['upload.intent.result']}</Typography.Text>
            </Grid.Row>
            <List
              style={{ width: '100%' }}
              size="small"
              bordered
              virtualListProps={{ height: 254 }}
            >
              {result.msgs?.map(({ key, value }) => (
                <List.Item key={key + value}>
                  <List.Item.Meta
                    avatar={
                      <Button
                        shape="square"
                        size="large"
                        icon={result.error ? <IconClose /> : <IconCheck />}
                        status={result.error ? 'danger' : 'success'}
                      />
                    }
                    title={key}
                    description={value}
                  />
                </List.Item>
              ))}
            </List>
          </Grid.Col>
        </Grid.Row>
      </Modal>
    </div>
  );
};

export default UploadIntent;
