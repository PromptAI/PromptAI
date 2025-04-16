import { uploadPdfLib } from '@/api/text/pdf';
import useModalForm from '@/hooks/useModalForm';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  List,
  Message,
  Modal,
  Space,
  Upload,
} from '@arco-design/web-react';
import { IconDelete, IconEdit, IconFilePdf } from '@arco-design/web-react/icon';
import React from 'react';
import useUrlParams from '../../../hooks/useUrlParams';
import i18n from './locale';

interface UploadPdfLibProps {
  row: any;
  onSuccess: () => void;
}
const UpdatePdfLib = ({ row, onSuccess }: UploadPdfLibProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form, rules] = useModalForm();
  const { projectId } = useUrlParams();
  const onSubmit = async () => {
    const { files } = await form.validate();
    const { originFile } = files[0];
    await uploadPdfLib(projectId, originFile, { id: row.id });
    Message.success(t['update.success']);
    onSuccess();
    setVisible(false);
  };
  return (
    <div>
      <Button
        size="small"
        type="text"
        icon={<IconEdit />}
        onClick={() => setVisible(true)}
      >
        {t['update.title']}
      </Button>
      <Modal
        style={{ width: '50%' }}
        title={t['update.title']}
        unmountOnExit
        visible={visible}
        onOk={onSubmit}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label={t['update.upload']} field="files" rules={rules}>
            <Upload
              drag
              limit={1}
              accept="*"
              tip={t['update.upload.accept']}
              autoUpload={false}
              beforeUpload={(file) => {
                if (file.size > 20 * 1024 * 1024) {
                  Message.warning(t['update.upload.filesize']);
                  return Promise.reject();
                }
                return Promise.resolve();
              }}
              renderUploadList={(list, { onRemove }) =>
                !!list?.length && (
                  <List
                    size="small"
                    dataSource={list}
                    render={(item) => (
                      <List.Item
                        key={item.name}
                        actions={[
                          <Button
                            key="delete"
                            size="mini"
                            onClick={() => onRemove(item)}
                            icon={<IconDelete />}
                          />,
                        ]}
                      >
                        <Space>
                          <IconFilePdf />
                          {item.name}
                        </Space>
                      </List.Item>
                    )}
                  />
                )
              }
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default UpdatePdfLib;
