import { clearTrash, moveInTrash, moveOutTrash } from '@/api/trash';
import {
  batchCreateComponent,
  deleteComponent,
  updateComponent,
} from '@/api/components';
import { moveNode } from '@/api/move';
import { DataItemHandlerMap } from './types';

export enum RU_TYPE {
  UPDATE_NODE = 'update_node',
  MOVE_NODE = 'move_node',
  ADD_NODE = 'add_node',
  DEL_NODE = 'del_node',
  TRASH_IN = 'trash_in',
  TRASH_OUT = 'trash_out',
  TRASH_CLEAR = 'trash_clear',
  DO_FAVORITE = 'do_favorite',
}
const config: DataItemHandlerMap = {
  [RU_TYPE.UPDATE_NODE]: {
    redo: async (item) => {
      const {
        changed: { after },
        dependencies: { projectId },
      } = item;
      const { type, id, ...other } = after;
      console.debug('redo update', after);
      await updateComponent(projectId, type, id, other);
    },
    undo: async (item) => {
      const {
        changed: { before },
        dependencies: { projectId },
      } = item;
      const { type, id, ...other } = before;
      console.debug('undo update', before);
      await updateComponent(projectId, type, id, other);
    },
  },
  [RU_TYPE.MOVE_NODE]: {
    redo: async (item) => {
      const { changed: moveData } = item;
      console.debug('move node', moveData);
      await moveNode(moveData);
    },
    undo: async (item) => {
      const { changed: moveData } = item;
      const unMoveData = { c1: moveData.c2, c2: moveData.c1 };
      console.debug('un move node', unMoveData);
      await moveNode(unMoveData);
    },
  },
  [RU_TYPE.ADD_NODE]: {
    redo: async (item) => {
      const {
        changed,
        dependencies: { projectId, children, flowId },
      } = item;
      console.debug('add_node redo will be create component', changed);
      await batchCreateComponent(projectId, flowId, [changed, ...children]);
    },
    undo: async (item) => {
      const {
        changed: { id },
        dependencies: { projectId, children },
      } = item;
      console.debug('add_node undo will be delete', [
        id,
        ...children.map((c) => c.id),
      ]);
      await deleteComponent(projectId, [id, ...children.map((c) => c.id)]);
    },
  },
  [RU_TYPE.DEL_NODE]: {
    redo: async (item) => {
      const {
        changed: { id },
        dependencies: { projectId, children },
      } = item;
      console.debug('del_node redo will be delete', [
        id,
        ...children.map((c) => c.id),
      ]);
      await deleteComponent(projectId, [id, ...children.map((c) => c.id)]);
    },
    undo: async (item) => {
      const {
        changed,
        dependencies: { projectId, children, flowId },
      } = item;
      // batch add node
      console.debug('del_node undo will be delete', [changed, ...children]);

      await batchCreateComponent(projectId, flowId, [changed, ...children]);
    },
  },
  [RU_TYPE.TRASH_IN]: {
    redo: async (item) => {
      const {
        changed,
        dependencies: { projectId, flowId, formId },
      } = item;
      let flag = changed.id;
      if (changed.type === 'condition') {
        flag = changed.data.breakId;
      }
      await moveInTrash(projectId, flowId, flag, formId);
      return 'reload_trash';
    },
    undo: async (item) => {
      const {
        changed,
        dependencies: { projectId, flowId, parent, formId, breakParentId },
      } = item;
      let flag = changed.id;
      let parentId = parent.id;
      if (changed.type === 'condition') {
        flag = changed.data.breakId;
        parentId = breakParentId;
      }
      await moveOutTrash(projectId, flowId, flag, parentId, formId);
      return 'reload_trash';
    },
  },
  [RU_TYPE.TRASH_OUT]: {
    redo: async (item) => {
      const {
        changed,
        dependencies: { projectId, flowId, parent, formId },
      } = item;
      await moveOutTrash(projectId, flowId, changed.id, parent.id, formId);
      return 'reload_trash';
    },
    undo: async (item) => {
      const {
        changed,
        dependencies: { projectId, flowId, formId },
      } = item;
      await moveInTrash(projectId, flowId, changed.id, formId);
      return 'reload_trash';
    },
  },
  [RU_TYPE.TRASH_CLEAR]: {
    redo: async (item) => {
      const {
        changed: { after },
        dependencies: { projectId, flowId },
      } = item;
      await clearTrash(
        projectId,
        flowId,
        after.map((a) => a.id)
      );
      return 'reload_trash';
    },
    undo: async (item) => {
      const {
        changed: { after },
        dependencies: { projectId, flowId },
      } = item;

      await batchCreateComponent(projectId, flowId, after);
      return 'reload_trash';
    },
  },
  [RU_TYPE.DO_FAVORITE]: {
    redo: async (item) => {
      const {
        changed: { after },
        dependencies: { projectId, flowId },
      } = item;
      await batchCreateComponent(projectId, flowId, after);
    },
    undo: async (item) => {
      const {
        changed: { after },
        dependencies: { projectId },
      } = item;
      await deleteComponent(
        projectId,
        after.map((a) => a.id)
      );
    },
  },
};

export default config;
