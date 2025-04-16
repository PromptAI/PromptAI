import React, { Fragment, useCallback } from 'react';
import { moveInTrash } from '@/api/trash';
import { Message, Modal } from '@arco-design/web-react';
import { FaTrashRestore } from 'react-icons/fa';
import { IconCopy, IconEdit, IconStarFill } from '@arco-design/web-react/icon';
import { PopupMenu } from '@/graph-next/Wrapper';
import { GraphTreeValue } from '@/core-next/types';
import { expendTree } from '@/core-next/utils';
import { useRedoUndo } from '../ru_context';
import { BuildMenusParams } from './types';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { useTrash } from '../trash/context';
import { deleteComponent } from '@/api/components';
import { isEmpty } from 'lodash';
import { useCopy } from '../copy/context';
import { doFavorite } from '@/api/favorites';
import { useFavorites } from '../favorites/context';
import { findParentForm } from '../util';

const footer = (cancel, ok) => [
  <Fragment key="ok">{ok}</Fragment>,
  <Fragment key="cancel">{cancel}</Fragment>,
];

function expendChildren4BreakNode(breakNode, form, children) {
  const {
    data: { conditionId },
  } = breakNode;
  const condition = form.children?.find((c) => c.id === conditionId);
  if (condition) {
    const conditionChildren = expendChildren(condition);
    children = [...children, ...conditionChildren, normalGraphNode(condition)];
  }
  return children;
}
export function createHandleDelFunc({
  title,
  content,
  projectId,
  flowId,
  node,
  RU,
  refresh,
  onChangeSelection,
  onChangeEditSelection,
}) {
  const handleDelete = () => {
    Modal.confirm({
      title,
      content,
      onConfirm: async () => {
        await deleteComponent(projectId, [node.id]);
        refresh();
        onChangeSelection(null);
        onChangeEditSelection(null);
        // handle type = break & condition children
        let children = expendChildren(node);

        if (children.some((c) => c.type === 'break')) {
          const form = computeParentForm(node);
          if (form) {
            console.debug('in form', form.id);
            const breakNodes = children.filter((x) => x.type === 'break');
            if (!isEmpty(breakNodes)) {
              breakNodes.forEach((breakNode) => {
                children = expendChildren4BreakNode(breakNode, form, children);
              });
            }
          }
        }
        if (node.type === 'break') {
          const form = computeParentForm(node);
          children = expendChildren4BreakNode(node, form, children);
        }
        if (node.type === 'condition') {
          const {
            data: { breakId },
            parent: form,
          } = node;
          const formChildren = expendChildren(form);
          const breakNode = formChildren.find((f) => f.id === breakId);
          if (breakNode) {
            const breakChildren = expendChildren(breakNode);
            children = [
              ...children,
              ...breakChildren,
              normalGraphNode(breakNode),
            ];
          }
        }
        RU.push({
          type: 'del_node',
          changed: normalGraphNode(node),
          dependencies: {
            projectId,
            children,
            parent: normalGraphNode(node?.parent),
            flowId,
          },
        });
      },
      footer,
    });
  };
  return handleDelete;
}

function buildMenus(
  {
    refreshTrash,
    t,
    haveTrash = true,
    haveFavorite = true,
    RU,
    props,
    submitCopy,
    refreshFavorites,
  }: BuildMenusParams,
  other?: PopupMenu[]
): PopupMenu[] {
  const { defaultProps, ...node } = props;
  const {
    projectId,
    rootComponentId,
    onChangeSelection,
    onChangeEditSelection,
    refresh,
  } = defaultProps;
  const moveToTrash = () => {
    Modal.confirm({
      title: t['low.node.move.trash'],
      content: t['flow.node.move.trash.confirm'],
      onConfirm: async () => {
        try {
          const form = findParentForm(node);
          let flag = node.id;
          let breakParentId = null;
          if (node.type === 'condition') {
            flag = node.data.breakId;
            const nodes = expendChildren(form);
            const breakNode = nodes.find((n) => n.id === flag);
            if (breakNode) {
              breakParentId = breakNode.parentId;
            }
          }
          await moveInTrash(projectId, rootComponentId, flag, form?.id);
          refresh();
          refreshTrash();
          onChangeSelection(null);
          onChangeEditSelection(null);
          RU.push({
            type: 'trash_in',
            changed: normalGraphNode(node),
            dependencies: {
              projectId,
              flowId: rootComponentId,
              parent: normalGraphNode(node?.parent),
              children: expendChildren(node),
              formId: form?.id,
              breakParentId,
            },
          });
        } catch (e) {
          Message.error(e.data?.message || t['flow.node.move.trash.error']);
        }
      },
      footer,
    });
  };
  const handleFavorite = async (widthChildren = false) => {
    const componentIds = widthChildren
      ? expendTree(node).map((n) => n.id)
      : [node.id];
    await doFavorite({
      projectId,
      rootComponentId,
      type: 'conversation',
      componentIds,
    });
    refreshFavorites();
    Message.success(t['flow.node.favorite.success']);
  };
  return [
    {
      key: 'edit',
      title: t['flow.node.edit'],
      icon: <IconEdit />,
      onClick: () => onChangeEditSelection(node),
    },
    {
      key: 'copy',
      title: t['flow.node.copy'],
      icon: <IconCopy />,
      onClick: () =>
        submitCopy({
          key: node.id,
          type: node.type,
          data: { breakpoint: normalGraphNode(node) },
        }),
      hidden: !['user', 'option', 'bot'].includes(node.type),
    },
    {
      key: 'trash',
      title: t['flow.node.trash'],
      icon: <FaTrashRestore className="arco-icon" style={{ color: 'red' }} />,
      onClick: moveToTrash,
      hidden: !haveTrash,
    },
    ...other,
    {
      key: 'favorite',
      title: t['flow.node.favorite'],
      icon: <IconStarFill className="arco-icon favorites-icon" />,
      hidden: !haveFavorite,
      divider: true,
      children: [
        {
          key: 'favorite_this',
          title: t['flow.node.favorite.current'],
          onClick: handleFavorite,
        },
        {
          key: 'favorite',
          title: t['flow.node.favorite.node'],
          onClick: () => handleFavorite(true),
        },
      ],
    },
  ];
}
export function useBuildMenus() {
  const t = useLocale(i18n);
  const { RU } = useRedoUndo();
  const { refreshTrash } = useTrash();
  const { submit } = useCopy();
  const { refreshFavorites } = useFavorites();
  return {
    buildMenus: useCallback(
      (
        params: Omit<
          BuildMenusParams,
          'RU' | 't' | 'refreshTrash' | 'submitCopy' | 'refreshFavorites'
        >,
        other?: PopupMenu[]
      ) =>
        buildMenus(
          {
            ...params,
            RU,
            t,
            refreshTrash,
            submitCopy: submit,
            refreshFavorites,
          },
          other
        ),
      [RU, refreshFavorites, refreshTrash, submit, t]
    ),
    RU,
    t,
  };
}

export function computeParentForm(node: GraphTreeValue) {
  if (node.type === 'form') {
    return node;
  }
  let temp = node;
  let parentForm = null;
  while (temp.parent) {
    temp = temp.parent;
    if (temp.type === 'form') {
      parentForm = temp;
      break;
    }
    if (['confirm', 'condition'].includes(temp.type)) {
      parentForm = null;
      break;
    }
  }
  return parentForm;
}

export function normalGraphNode(node: GraphTreeValue) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { children, subChildren, parent, ...normarl } = node;
  return normarl;
}
export function expendChildren(root, temp = []) {
  const nodes = expendTree(root, temp);
  return nodes.filter((n) => n.id !== root.id);
}
