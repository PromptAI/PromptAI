import { uploadPdfLib } from '@/api/text/pdf';
import useModalForm from '@/hooks/useModalForm';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  List,
  Message,
  Modal,
  Space,
  Upload,
} from '@arco-design/web-react';
import { IconDelete, IconFilePdf, IconPlus } from '@arco-design/web-react/icon';
import React from 'react';
import useUrlParams from '../../../hooks/useUrlParams';
import i18n from './locale';

interface CreatePdfLibProps {
  onSuccess: () => void;
}
const CreatePdfLib = ({ onSuccess }: CreatePdfLibProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form, rules] = useModalForm();
  const { projectId } = useUrlParams();
  const onSubmit = async () => {
    const { files, description } = await form.validate();
    const { originFile } = files[0];
    await uploadPdfLib(projectId, originFile, { description });
    Message.success(t['create.success']);
    onSuccess();
    setVisible(false);
  };
  return (
    <div>
      <Button
        type="primary"
        icon={<IconPlus />}
        onClick={() => setVisible(true)}
      >
        {t['create.title']}
      </Button>
      <Modal
        style={{ width: '50%' }}
        title={t['create.title']}
        unmountOnExit
        visible={visible}
        onOk={onSubmit}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label={t['create.remark']} field="description">
            <Input placeholder={t['create.remark']} />
          </Form.Item>
          <Form.Item label={t['create.upload']} field="files" rules={rules}>
            <Upload
              drag
              limit={1}
              accept="*"
              tip={t['create.upload.accept']}
              autoUpload={false}
              beforeUpload={(file) => {
                if (file.size > 20 * 1024 * 1024) {
                  Message.warning(t['create.upload.filesize']);
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

export default CreatePdfLib;
