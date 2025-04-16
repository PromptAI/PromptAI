import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { IconFile, IconSend, IconUserAdd } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import React, { useCallback, useMemo } from 'react';
import { VscDebugBreakpointLog } from 'react-icons/vsc';
import { GrDirections } from 'react-icons/gr';
import {
  creaeteDefaultBot,
  createDefaultBreak,
  createDefaultForm,
  createDefaultGoto,
  createDefaultUser,
} from '../helper';
import { useGraphNodeDrop } from '../hooks';
import { RelationNodeDefinedProps } from '../types';
import { useBuildMenus, computeParentForm, normalGraphNode } from '../util';
import ResponseView from './view';
import { Form, Input, Message, Modal } from '@arco-design/web-react';
import useRules from '@/hooks/useRules';
import useModalForm from '@/hooks/useModalForm';
import { treeForEach } from '@/utils/tree';

const findClosestUserNode = (node) => {
  let p = node;
  while ((p = p.parent)) {
    // after interrupt`s node, not be have a goto`s node
    if (['user', 'option'].includes(p.type) && p.parent?.type !== 'interrupt')
      return p;
  }
  return null;
};

const Response = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { t, buildMenus, RU } = useBuildMenus();
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();
  const menus = useMemo<PopupMenu[]>(() => {
    const { defaultProps, ...node } = props;
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const parentForm = computeParentForm(node);
    const handleAddIntent = async () => {
      const { id } = node;
      const [user, children] = await createDefaultUser(projectId, id, [flowId]);
      onChangeSelection(user);
      onChangeEditSelection(user);
      refresh();
      RU.push({
        type: 'add_node',
        changed: user,
        dependencies: {
          projectId,
          parent: normalGraphNode(node),
          children,
          flowId,
        },
      });
    };
    // const handleAddOption = async () => {
    //   const { id } = node;
    //   const [option, children] = await createDefaultOption(
    //     projectId,
    //     id,
    //     { examples: [{ text: '' }], display: 'user_click' },
    //     [flowId]
    //   );
    //   onChangeSelection(option);
    //   onChangeEditSelection(option);
    //   refresh();
    //   RU.push({
    //     type: 'add_node',
    //     changed: option,
    //     dependencies: {
    //       projectId,
    //       parent: normalGraphNode(node),
    //       children,
    //       flowId,
    //     },
    //   });
    // };
    const handleAddResponse = async () => {
      const { id } = node;
      const [bot, children] = await creaeteDefaultBot(projectId, id, [flowId]);
      onChangeSelection(bot);
      onChangeEditSelection(bot);
      refresh();
      RU.push({
        type: 'add_node',
        changed: bot,
        dependencies: {
          projectId,
          parent: normalGraphNode(node),
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
      const { id } = node;
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
    const handleAddGoto = async () => {
      const { id, relations } = node;
      const [gotoNode, children] = await createDefaultGoto(
        projectId,
        id,
        '',
        '',
        '',
        relations
      );
      gotoNode.parent = node;
      onChangeSelection(gotoNode);
      onChangeEditSelection(gotoNode);
      refresh();
      RU.push({
        type: 'add_node',
        changed: normalGraphNode(gotoNode),
        dependencies: {
          parent: normalGraphNode(node),
          projectId,
          children,
          flowId,
        },
      });
    };
    const hidden = !isEmpty(node.children);
    const closestUserNode = findClosestUserNode(node);
    return buildMenus({ props }, [
      {
        key: 'user',
        title: t['flow.node.user'],
        icon: <IconUserAdd />,
        onClick: handleAddIntent,
        hidden: node.children?.some((n) => ['bot', 'break'].includes(n.type)),
        divider: true,
      },
      // {
      //   key: 'option',
      //   title: t['flow.node.option'],
      //   icon: <FaRegHandPointUp className="arco-icon" />,
      //   onClick: handleAddOption,
      //   hidden: node.children?.some((n) => ['bot', 'break'].includes(n.type)),
      // },
      {
        key: 'break',
        title: t['flow.node.break'],
        icon: <VscDebugBreakpointLog className="arco-icon" />,
        onClick: handleAddBreak,
        hidden: hidden || !parentForm,
      },
      {
        key: 'bot',
        title: t['flow.node.bot'],
        icon: <IconSend />,
        onClick: handleAddResponse,
        hidden,
      },
      // not root -> bot -> the closest user .... -> current node
      ...(closestUserNode?.parent?.parent?.parent?.parent
        ? [
            {
              key: 'goto',
              title: t['flow.node.goto'],
              icon: <GrDirections className="arco-icon" />,
              onClick: handleAddGoto,
              hidden,
            },
          ]
        : []),
      {
        key: 'form',
        title: t['flow.node.form'],
        icon: <IconFile />,
        onClick: () => setVisible(true),
        hidden: hidden || parentForm,
      },
    ]);
  }, [RU, buildMenus, props, t, setVisible]);
  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  const handleAddForm = useCallback(async () => {
    const { id, relations } = node;
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
      relations
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
  return (
    <div>
      <Wrapper
        menus={menus}
        selected={props.selected}
        validatorError={props.validatorError}
      >
        <div {...dropProps}>
          <ResponseView {...props} />
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

export default Response;
export { ResponseView };
