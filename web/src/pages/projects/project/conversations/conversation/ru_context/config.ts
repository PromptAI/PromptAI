import { clearTrash, moveInTrash, moveOutTrash } from '@/api/trash';
import {
  batchCreateComponent,
  deleteComponent,
  updateComponent,
} from '@/api/components';
import { moveNode } from '@/api/move';
import { DataItemHandlerMap } from './types';

const config: DataItemHandlerMap = {
  update_node: {
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
  move_node: {
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
  add_node: {
    redo: async (item) => {
      const {
        changed,
        dependencies: { projectId, children, flowId },
      } = item;
      await batchCreateComponent(projectId, flowId, [changed, ...children]);
    },
    undo: async (item) => {
      const {
        changed: { id },
        dependencies: { projectId, children },
      } = item;

      await deleteComponent(projectId, [id, ...children.map((c) => c.id)]);
    },
  },
  del_node: {
    redo: async (item) => {
      const {
        changed: { id },
        dependencies: { projectId, children },
      } = item;

      await deleteComponent(projectId, [id, ...children.map((c) => c.id)]);
    },
    undo: async (item) => {
      const {
        changed,
        dependencies: { projectId, children, flowId },
      } = item;
      // batch add node

      await batchCreateComponent(projectId, flowId, [changed, ...children]);
    },
  },
  trash_in: {
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
  trash_out: {
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
  trash_clear: {
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
  do_favorite: {
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
