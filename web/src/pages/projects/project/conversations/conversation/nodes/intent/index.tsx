import { Form, Input, Message, Modal } from '@arco-design/web-react';
import { isEmpty } from 'lodash';
import React, { useCallback, useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { VscDebugBreakpointLog } from 'react-icons/vsc';
import { IconFile, IconSend } from '@arco-design/web-react/icon';
import { RelationNodeDefinedProps } from '../types';
import useRules from '@/hooks/useRules';
import useModalForm from '@/hooks/useModalForm';
import { useGraphNodeDrop } from '../hooks';
import UserView from './view';
import { computeParentForm, normalGraphNode, useBuildMenus } from '../util';
import {
  creaeteDefaultBot,
  createDefaultBreak,
  createDefaultForm,
} from '../helper';
import { treeForEach } from '@/utils/tree';

const Intent = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;

  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();

  const { buildMenus, t, RU } = useBuildMenus();

  const handleAddForm = useCallback(async () => {
    const { id } = node;
    const {
      projectId,
      rootComponentId: flowId,
      refresh,
      onChangeSelection,
    } = defaultProps;
    const values = await form.validate();
    const [formNode, children] = await createDefaultForm(
      projectId,
      id,
      values,
      [flowId]
    );
    Message.success(t['message.create.success']);
    setVisible(false);
    onChangeSelection(null);
    refresh();
    RU.push({
      type: 'add_node',
      changed: formNode,
      dependencies: {
        parent: normalGraphNode(node),
        projectId,
        children,
        flowId,
      },
    });
  }, [RU, defaultProps, form, node, setVisible, t]);
  const menus = useMemo<PopupMenu[]>(() => {
    const { defaultProps, ...node } = props;
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const { id } = node;
    const parentForm = computeParentForm(node);
    const handleAddResponse = async () => {
      const [bot, children] = await creaeteDefaultBot(projectId, id, [flowId]);
      onChangeSelection(bot);
      onChangeEditSelection(bot);
      refresh();
      // push redo-undo
      RU.push({
        type: 'add_node',
        changed: bot,
        dependencies: {
          parent: normalGraphNode(node),
          projectId,
          children,
          flowId,
        },
      });
    };
    const handleAddBreak = async () => {
      const getBreakCount = (currentForm, breakCountTemp = 0) => {
        const { subChildren } = currentForm;
        const interrrupt = subChildren.find((s) => s.type === 'interrupt');
        treeForEach([interrrupt], (n) => {
          if (n.type === 'break') {
            breakCountTemp += 1;
          }
          if (n.type === 'form') {
            getBreakCount(n, breakCountTemp);
          }
        });
        return breakCountTemp;
      };
      const breackCount = getBreakCount(parentForm);
      const [breakNode, children] = await createDefaultBreak(
        projectId,
        id,
        { formId: parentForm.id, name: `break-${breackCount + 1}` },
        [flowId]
      );
      onChangeSelection(breakNode);
      onChangeEditSelection(breakNode);
      refresh();
      RU.push({
        type: 'add_node',
        changed: breakNode,
        dependencies: {
          parent: normalGraphNode(node),
          projectId,
          children,
          flowId,
        },
      });
    };
    const hidden = !isEmpty(node.children) || !isEmpty(node.afterRhetorical);
    return buildMenus({ props }, [
      {
        key: 'bot',
        title: t['flow.node.bot'],
        icon: <IconSend />,
        onClick: handleAddResponse,
        hidden: node.afterRhetorical
          ? true
          : !!node.children?.some((n) => n.type !== 'bot'),
        divider: true,
      },
      {
        key: 'form',
        title: t['flow.node.form'],
        icon: <IconFile />,
        onClick: () => setVisible(true),
        hidden,
      },
      {
        key: 'break',
        title: t['flow.node.break'],
        icon: <VscDebugBreakpointLog className="arco-icon" />,
        onClick: handleAddBreak,
        hidden: hidden || !parentForm,
      },
    ]);
  }, [props, buildMenus, t, RU, setVisible]);

  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <div>
      <Wrapper
        menus={menus}
        selected={props.selected}
        validatorError={props.validatorError}
      >
        <div {...dropProps}>
          <UserView {...props} />
        </div>
      </Wrapper>
      <div onDoubleClick={(evt) => evt.stopPropagation()}>
        <Modal
          title={t['flow.node.form.create']}
          visible={visible}
          onCancel={() => setVisible(false)}
          onOk={handleAddForm}
        >
          <Form layout="vertical" form={form}>
            <Form.Item
              label={t['flow.node.form.name']}
              field="name"
              rules={rules}
            >
              <Input placeholder="" />
            </Form.Item>
            <Form.Item
              label={t['flow.node.form.description']}
              field="description"
            >
              <Input.TextArea autoSize placeholder="" />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </div>
  );
};

export default Intent;
export { UserView };
