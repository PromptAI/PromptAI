import { useProjectContext } from '@/layout/project-layout/context';
import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import { Button, Grid, Message, Modal, Upload } from '@arco-design/web-react';
import { UploadItem } from '@arco-design/web-react/es/Upload';
import { IconUpload } from '@arco-design/web-react/icon';
import { useCreation } from 'ahooks';
import React, { useState } from 'react';
import i18n from '../../../locale';

interface UpdalodFaqProps {
  faqId: string;
  onSuccess: () => void;
}
const UploadFaq = ({ onSuccess, faqId }: UpdalodFaqProps) => {
  const t = useLocale(i18n);
  const project = useProjectContext();
  const [visible, setVisible] = useState(false);

  const headers = useCreation(() => ({ Authorization: Token.get() }), []);

  const onFileChange = (fileList: UploadItem[], upload: UploadItem) => {
    if (upload.status === 'done' && fileList?.length) {
      Message.success(t['sample.import.success']);
      onSuccess();
    }
    if (upload.status === 'error') {
      Message.error(t['sample.import.error']);
      upload.response = (upload.response as any)?.message || 'error';
    }
  };

  return (
    <div>
      <Button
        type="text"
        icon={<IconUpload />}
        onClick={() => setVisible(true)}
      >
        {t['sample.import']}
      </Button>
      <Modal
        style={{ width: '50%' }}
        title={t['sample.import']}
        maskClosable={false}
        unmountOnExit
        escToExit={false}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={() => setVisible(false)}
      >
        <Grid.Row>
          <Grid.Col span={24}>
            <Upload
              action="/api/project/component/faq/upload"
              headers={headers}
              accept=".csv,.xls,.xlsx"
              drag
              multiple={false}
              limit={1}
              tip="csv,xls,xlsx"
              data={{ projectId: project.id, faqId, relations: [faqId] }}
              onChange={onFileChange}
            />
          </Grid.Col>
        </Grid.Row>
      </Modal>
    </div>
  );
};

export default UploadFaq;
