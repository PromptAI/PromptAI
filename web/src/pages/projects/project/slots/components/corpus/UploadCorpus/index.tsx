import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  FormInstance,
  Link,
  Message,
  Modal,
  Space,
  Typography,
  Upload,
} from '@arco-design/web-react';
import React, { useRef, useState } from 'react';
import i18n from './locale';
import useDocumentLinks from '@/hooks/useDocumentLinks';

const UploadCorpus = ({ slotId, name, onSuccess }) => {
  const formRef = useRef<FormInstance>();
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const handleCancel = () => {
    setVisible(false);
    onSuccess();
  };
  const docs = useDocumentLinks();
  return (
    <>
      <Button
        type="outline"
        status="warning"
        size="mini"
        onClick={() => setVisible(true)}
      >
        {t['slot.form.dictionary.upload']}
      </Button>
      <Modal
        title={t['slot.form.dictionary.upload.title'] + ' ' + name}
        visible={visible}
        onCancel={handleCancel}
        unmountOnExit
        okButtonProps={{ style: { display: 'none' } }}
        cancelText={t['slot.form.dictionary.upload.cancel']}
      >
        <Form layout="vertical" ref={formRef}>
          <Form.Item
            label={t['slot.form.dictionary']}
            field="data"
            extra={
              <Space direction="vertical">
                <Typography.Text>
                  {t['slot.form.dictionary.upload.extra.1']}{' '}
                  <Link href="/templates/areaName.txt" download="areaName.txt">
                    {t['slot.form.dictionary.upload.download']}
                  </Link>
                </Typography.Text>
                <Typography.Text>
                  {t['slot.form.dictionary.upload.extra.2']}
                  <Link href={docs.slotDictionary} target="_blank">
                    {t['slot.form.dictionary.upload.extra.2.link']}
                  </Link>
                </Typography.Text>
                <Typography.Text type="warning" bold>
                  {t['slot.form.dictionary.upload.extra.3']}
                </Typography.Text>
              </Space>
            }
          >
            <Upload
              accept=".txt"
              action={`/api/project/component/entity/dictionary/${slotId}`}
              limit={1}
              headers={{ Authorization: Token.get() }}
              multiple
              onExceedLimit={() => {
                Message.warning(t['slot.form.dictionary.upload.limit']);
              }}
            >
              <div className="app-upload-drag-trigger">
                <div>
                  {t['slot.form.dictionary.upload.trigger.prefix']}
                  <span style={{ color: '#3370FF', padding: '0 4px' }}>
                    {t['slot.form.dictionary.upload.trigger.suffix']}
                  </span>
                </div>
              </div>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default UploadCorpus;
