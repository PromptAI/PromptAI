import DownloadTemplate from '@/components/DownloadTemplate';
import useModalForm from '@/hooks/useModalForm';
import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  Grid,
  Modal,
  Space,
  Typography,
  Upload,
} from '@arco-design/web-react';
import { UploadItem } from '@arco-design/web-react/es/Upload';
import { IconShareExternal } from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import i18n from './locale';
import Result from './Result';

const templates = [
  'text_ext.txt',
  'json_ext.json',
  'xls_ext.xls',
  'xlsx_ext.xlsx',
].map((name) => (
  <DownloadTemplate key={name} name={name} type="text" size="small">
    {name.split('.')[1]}
  </DownloadTemplate>
));
export interface UserExtProps {
  onFinish: (data: string[]) => void;
}
const UserExt = ({ onFinish }: UserExtProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form] = useModalForm();
  const headers = useMemo(() => ({ Authorization: Token.get() }), []);
  const onOk = () => {
    form.validate().then(({ result }) => {
      setVisible(false);
      onFinish(result);
    });
  };
  const onUploadFileChange = (fileList: UploadItem[], file: UploadItem) => {
    if (file.status === 'done' && fileList?.length) {
      const response = file.response as any;
      let result;
      if (response?.ext?.length) {
        result = response.ext;
      } else {
        result = [t['user.ext.upload.error']];
      }
      form.setFieldValue('result', result);
    }
    if (file.status === 'error') {
      file.response = (file.response as any)?.message || 'error';
    }
  };
  return (
    <div>
      <Button
        type="text"
        size="mini"
        icon={<IconShareExternal />}
        onClick={() => setVisible(true)}
      >
        {t['user.ext']}
      </Button>
      <Modal
        style={{ width: '50%' }}
        title={t['user.ext']}
        maskClosable={false}
        unmountOnExit
        escToExit={false}
        visible={visible}
        onOk={onOk}
        onCancel={() => setVisible(false)}
      >
        <Grid.Row gutter={12}>
          <Grid.Col span={12}>
            <Grid.Row>
              <Form name="import-user-ext" layout="vertical">
                <Form.Item
                  label={t['user.ext.file']}
                  style={{ minHeight: 241 }}
                >
                  <Upload
                    action="/api/project/component/user/ext/parse"
                    headers={headers}
                    drag
                    multiple={false}
                    limit={1}
                    accept=".txt,.json,.xls,.xlsx"
                    tip="txt,json,xls,xlsx"
                    onChange={onUploadFileChange}
                    onRemove={() => form.resetFields()}
                  />
                </Form.Item>
              </Form>
            </Grid.Row>
            <Grid.Row>
              <Space size="small">
                <Typography.Text>{t['user.ext.template']}</Typography.Text>
                {templates}
              </Space>
            </Grid.Row>
          </Grid.Col>
          <Grid.Col span={12}>
            <Form name="parse-result" layout="vertical" form={form}>
              <Form.Item
                label={t['user.ext.parse.result']}
                field="result"
                rules={[{ required: true, type: 'array', min: 1 }]}
              >
                <Result />
              </Form.Item>
            </Form>
          </Grid.Col>
        </Grid.Row>
      </Modal>
    </div>
  );
};

export default UserExt;
