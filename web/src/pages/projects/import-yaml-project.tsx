import { importYamlProject, importZipProject } from '@/api/projects';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import useLocale, { useLocaleLang } from '@/utils/useLocale';
import { Button, Form, FormInstance, Message, Modal, Upload } from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import React, { useRef, useState } from 'react';
import { useHistory } from 'react-router';
import i18n from './locale';
import Token from '@/utils/token';

const ImportYamlProject = () => {
  const t = useLocale(i18n);
  const dt = useLocale();
  const lang = useLocaleLang();
  const history = useHistory();
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();
  const [loading, setLoading] = useState(false);

  const yamlFormRef = useRef<FormInstance>();
  const handleImportOK = async () => {
    const { uploadRes: uploadRes } = await yamlFormRef.current.validate();
    if (uploadRes && uploadRes.length > 0) {
      const { response, status } = uploadRes[0];
      if (status === 'done') {
        const { id } = response;

        const data ={
          "id":id,
          locale: lang.startsWith('en') ? 'en' : 'zh',
        }

        // save to Project
        const project = await importZipProject(data);
        Message.success(dt['message.create.success']);
        history.push(`/projects/${project.id}/tool/setting`);
      }
    }
    yamlFormRef.current.setFields({
      uploadRes: {
        value: uploadRes,
        error: { message: "Required upload file" }
      }
    });
  };


  return (
    <div>
      <Button
        type="primary"
        icon={<IconPlus />}
        onClick={() => setVisible(true)}
      >
        {t['project.zip.import']}
      </Button>
      <Modal
        title={t['project.zip.import']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleImportOK}
        confirmLoading={loading}
        unmountOnExit
        style={{ width: 530 }}
      >
        <Form ref={yamlFormRef} layout="vertical">
          <Form.Item
            label={"Mica"}
            field="uploadRes"
          >
            <Upload
              action="/api/blobs/upload"
              limit={1}
              accept=".zip"
              headers={{ Authorization: Token.get() }}
              beforeUpload={(file) => {
                if (file.size > 2 * 1024 * 1024) {
                  Message.warning(
                    'The file exceeded the upper limit of 20 MB.'
                  );
                  return Promise.reject();
                }
                return Promise.resolve();
              }}
              onExceedLimit={() => {
                Message.warning(
                  'Should be upload One'
                );
              }}
            >
              <div className="app-upload-drag-trigger">
                <div>
                  {'Drag the file here or'}
                  <span style={{ color: '#3370FF', padding: '0 4px' }}>
                        {'Click to upload'}
                  </span>
                </div>
              </div>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ImportYamlProject;
