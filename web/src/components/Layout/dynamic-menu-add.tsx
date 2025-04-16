import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import { post } from '@/utils/request';
import useLocale from '@/utils/useLocale';
import { Form, Input, Modal, Radio } from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import React from 'react';
import { useHistory, useParams } from 'react-router';
import { Dynamic } from './types';

interface DynamicMenuAddProps {
  dynamic: Dynamic;
  basePath: string;
  onAfterAdd: (menuKey: string) => void;
}
const DynamicMenuAdd = ({
  dynamic,
  basePath,
  onAfterAdd,
}: DynamicMenuAddProps) => {
  const t = useLocale();
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();
  const urlParams = useParams();
  const histoty = useHistory();
  const handleSubmit = async () => {
    // hard code in prompt ai project
    const { api, menuPrefix = '', menuSuffix = '', transformAdd } = dynamic;
    const values = await form.validate();
    // todo dynamic conversation api
    const nodes = await post(api, transformAdd(values, urlParams));
    const conversation = nodes.find((n) => n.type === 'conversation');
    setVisible(false);
    const path = `${menuPrefix}${basePath}/${conversation?.id}/${menuSuffix}`;
    histoty.push(path);
    onAfterAdd(path);
  };

  return (
    <>
      <div
        className="flex items-center"
        style={{ color: 'rgb(var(--primary-6))', fontWeight: 500 }}
        onClick={() => setVisible(true)}
      >
        <IconPlus style={{ marginRight: 8, stroke: 'rgb(var(--primary-6))' }} />
        {t['menu.projects.id.complex.add']}
      </div>
      <Modal
        visible={visible}
        unmountOnExit
        title={t['menu.projects.id.complex.add.title']}
        onCancel={() => setVisible(false)}
        maskClosable={false}
        onOk={handleSubmit}
      >
        <Form layout="vertical" form={form}>
          <Form.Item
            label={t['menu.projects.id.complex.add.form.name']}
            field="name"
            rules={rules}
          >
            <Input
              placeholder={
                t['menu.projects.id.complex.add.form.name.placeholder']
              }
            />
          </Form.Item>
          <Form.Item
            label={t['menu.projects.id.complex.add.form.description']}
            field="description"
            required={true}
            rules={rules}
          >
            <Input.TextArea
              autoSize
              placeholder={
                t['menu.projects.id.complex.add.form.description.placeholder']
              }
            />
          </Form.Item>
          <Form.Item  field="type"
                     label={t[`menu.projects.id.complex.flow.type`]}
                     required={true}>
            <Radio.Group name="type" defaultValue={"flowAgent"}>
              <Radio value="flowAgent">{t['menu.projects.id.complex.flow.type.flowAgent']}</Radio>
              <Radio value="llmAgent">{t['menu.projects.id.complex.flow.type.llmAgent']}</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item shouldUpdate noStyle>
            {({ type }) => (
              <div style={{ marginTop: 16 }}>

                {type === 'llmAgent' && (
                  <Form.Item
                    label={t['menu.projects.id.complex.flow.type.llmAgent.prompt']}
                    field="prompt"
                    required={true}
                    layout="vertical"
                  >
                    <Input.TextArea
                      autoSize
                      placeholder={t['menu.projects.id.complex.flow.type.llmAgent.prompt.placeholder']}
                      style={{ minHeight: 64 }}
                    />
                  </Form.Item>
                )}
              </div>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default DynamicMenuAdd;
