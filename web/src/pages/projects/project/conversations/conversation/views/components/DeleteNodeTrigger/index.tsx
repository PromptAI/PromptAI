import React, { useCallback } from 'react';
import { NodeProps } from '../../types';
import { Button, Message, Modal } from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { useGraphStore } from '../../../store/graph';
import { expendChildren, unwrap } from '../../utils/node';
import ru, { RU_TYPE } from '../../../features/ru';
import { moveInTrash } from '@/api/trash';
import { useTrash } from '../../../trash/context';
import { findParentForm } from '../../../util';

interface DeleteNodeTriggerProps {
  node: NodeProps;
}
const DeleteNodeTrigger: React.FC<DeleteNodeTriggerProps> = ({ node }) => {
  const t = useLocale(i18n);
  const { refreshTrash } = useTrash();
  const { projectId, flowId, refresh } = useGraphStore(
    ({ projectId, flowId, refresh }) => ({ projectId, flowId, refresh })
  );
  const onClick = useCallback(() => {
    Modal.confirm({
      title: t['node.delete'],
      content: <div className="text-center">{t['content']}</div>,
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
          await moveInTrash(projectId, flowId, flag, form?.id);
          refresh();
          refreshTrash();
          ru.push({
            type: RU_TYPE.TRASH_IN,
            changed: unwrap(node),
            dependencies: {
              projectId,
              flowId,
              parent: node?.parent && unwrap(node?.parent) || undefined,
              children: expendChildren(node),
              formId: form?.id,
              breakParentId,
            },
          });
        } catch (e) {
          Message.error(e.data?.message || t['node.delete.error']);
        }
      },
      footer: (cancel, ok) => (
        <>
          {ok}
          {cancel}
        </>
      ),
    });
  }, [flowId, node, projectId, refresh, refreshTrash, t]);
  return (
    <Button icon={<IconDelete />} status="danger" onClick={onClick}>
      {t['node.delete']}
    </Button>
  );
};

export default DeleteNodeTrigger;
