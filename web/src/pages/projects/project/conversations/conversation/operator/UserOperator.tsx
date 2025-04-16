import { createForm } from '@/api/components';
import { sampleSelect } from '@/graph-next';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphIntentNext } from '@/graph-next/type';
import useModalForm from '@/hooks/useModalForm';
import { VscDebugBreakpointLog } from 'react-icons/vsc';
import useRules from '@/hooks/useRules';
import useLocale, { useDefaultLocale } from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  Message,
  Modal,
  Space,
  Trigger,
} from '@arco-design/web-react';
import {
  IconCloud,
  IconFile,
  IconPlus,
  IconSend,
} from '@arco-design/web-react/icon';
import { useSafeState } from 'ahooks';
import { isEmpty } from 'lodash';
import { nanoid } from 'nanoid';
import React, { useMemo } from 'react';
import { SelectionProps } from '../types';
import i18n from './locale';
import { newResponse, newBreak } from './operator';
import { computeParentForm } from '../nodes/util';

interface UserOperatorProps extends SelectionProps<GraphIntentNext> {
  refresh: () => void;
}

const UserOperator = ({
  projectId,
  selection,
  onChange,
  onChangeSelection,
  refresh,
  onChangeEditSelection,
}: UserOperatorProps) => {
  const [loading, setLoading] = useSafeState(false);
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();
  const t = useLocale(i18n);
  const dt = useDefaultLocale();
  const parentForm = useMemo(() => computeParentForm(selection), [selection]);
  const handleAddText = () => {
    const { id, relations } = selection;
    setLoading(true);
    newResponse(
      projectId,
      id,
      relations,
      [{ id: nanoid(), type: 'text', content: { text: '' }, delay: 500 }],
      (node) => {
        onChange((vals) =>
          sampleSelect(ObjectArrayHelper.add(vals, node), node)
        );
        onChangeSelection(node);
        onChangeEditSelection(node);
      }
    ).finally(() => setLoading(false));
  };
  const handleAddImageResponse = () => {
    const { id, relations } = selection;
    setLoading(true);
    newResponse(
      projectId,
      id,
      relations,
      [
        {
          id: nanoid(),
          type: 'image',
          content: { text: '', image: [{ id: nanoid(), url: '' }] },
          delay: 500,
        },
      ],
      (node) => {
        onChange((vals) =>
          sampleSelect(ObjectArrayHelper.add(vals, node), node)
        );
        onChangeSelection(node);
        onChangeEditSelection(node);
      }
    ).finally(() => setLoading(false));
  };
  const handleAddFormResponse = () => {
    form
      .validate()
      .then((values) => {
        createForm(projectId, selection.id, values, selection.relations).then(
          () => {
            Message.success(dt['message.create.success']);
            setVisible(false);
            onChangeSelection(null);
            refresh();
          }
        );
      })
      .catch(() => null);
  };
  const handleAddWebhookResponse = () => {
    const { id, relations } = selection;
    setLoading(true);
    newResponse(
      projectId,
      id,
      relations,
      [
        {
          id: nanoid(),
          type: 'webhook',
          content: {
            text: '',
          },
          delay: 500,
        },
      ],
      (node) => {
        onChange((vals) =>
          sampleSelect(ObjectArrayHelper.add(vals, node), node)
        );
        onChangeSelection(node);
        onChangeEditSelection(node);
      }
    ).finally(() => setLoading(false));
  };

  const handleAddBreakResponse = () => {
    const { id, relations } = selection;
    setLoading(true);
    newBreak(
      projectId,
      id,
      relations,
      parentForm.id,
      t['operator.break.name'],
      (node) => {
        onChange((vals) =>
          sampleSelect(ObjectArrayHelper.add(vals, node), node)
        );
        onChangeSelection(node);
        onChangeEditSelection(node);
        refresh();
      }
    ).finally(() => setLoading(false));
  };
  // const handleAddActionResponse = () => {
  //   const { id, relations } = selection;
  //   setLoading(true);
  //   const resId = nanoid();
  //   newResponse(
  //     projectId,
  //     id,
  //     relations,
  //     [
  //       {
  //         id: resId,
  //         type: 'action',
  //         content: { text: `action_${resId}`, code: defaultActionCode },
  //       },
  //     ],
  //     (node) => {
  //       onChange((vals) =>
  //         sampleSelect(ObjectArrayHelper.add(vals, node), node)
  //       );
  //       onChangeSelection(node);
  //     }
  //   );
  // };
  if (!isEmpty(selection?.children) || !isEmpty(selection?.afterRhetorical))
    return null;
  return (
    <div>
      <Trigger
        position="rt"
        mouseEnterDelay={100}
        mouseLeaveDelay={100}
        popup={() => (
          <Space direction="vertical" className="app-operator-menu">
            <Button size="mini" type="text" onClick={handleAddText}>
              <IconSend className="mr-1" />
              {t['operator.send.text']}
            </Button>
            <Button size="mini" type="text" onClick={handleAddImageResponse}>
              <IconSend className="mr-1" />
              {t['operator.send.image']}
            </Button>
            <Button size="mini" type="text" onClick={handleAddWebhookResponse}>
              <IconCloud className="mr-1" />
              {t['operator.webhook']}
            </Button>
            {/* <Button size="mini" type="text" onClick={handleAddActionResponse}>
              <IconAt className="mr-1" />
              Action
            </Button> */}
            <Button size="mini" type="text" onClick={() => setVisible(true)}>
              <IconFile className="mr-1" />
              {t['operator.start.form']}
            </Button>
            {parentForm && (
              <Button size="mini" type="text" onClick={handleAddBreakResponse}>
                <VscDebugBreakpointLog
                  style={{ color: 'red' }}
                  className="mr-1"
                />
                {t['operator.break']}
              </Button>
            )}
          </Space>
        )}
        trigger="click"
        updateOnScroll
      >
        <Button
          loading={loading}
          size="mini"
          type="text"
          shape="circle"
          icon={<IconPlus />}
        />
      </Trigger>
      <Modal
        title={t['forms.create']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleAddFormResponse}
      >
        <Form
          labelCol={{ style: { flexBasis: 90 } }}
          wrapperCol={{ style: { flexBasis: 'calc(100% - 90px)' } }}
          form={form}
        >
          <Form.Item label={t['forms.form.name']} field="name" rules={rules}>
            <Input placeholder="" />
          </Form.Item>
          <Form.Item label={t['forms.form.description']} field="description">
            <Input.TextArea autoSize placeholder="" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default UserOperator;
