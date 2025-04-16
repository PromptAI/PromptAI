import { moveOutTrash } from '@/api/trash';
import { useCallback, useEffect, useMemo, useRef } from 'react';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { useDragContext } from '../dnd/drag-context';
import { Message } from '@arco-design/web-react';
import { useTrash } from '../trash/context';
import { updateComponent } from '@/api/components';
import { expendChildren, normalGraphNode } from './util';
import { cloneDeep, keyBy } from 'lodash';
import { moveNode } from '@/api/move';
import { useRedoUndo } from '../ru_context';
import { createDefaultComponent } from './helper';
import { doFavoritePaste } from '@/api/favorites';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { findParentForm } from '../util';

const searchParentElementByClassName = (
  start: HTMLElement,
  searchs: string | string[],
  limits?: string[]
) => {
  let c = start;
  do {
    if (limits?.some((limit) => c.classList.contains(limit))) {
      break;
    }
    if (
      Array.isArray(searchs) &&
      searchs.some((search) => c.classList.contains(search))
    ) {
      return c;
    }
    if (typeof searchs === 'string' && c.classList.contains(searchs)) {
      return c;
    }
  } while (c.parentElement && (c = c.parentElement));
  return null;
};
const className = 'drop-node';
let containerElement: HTMLElement = null;
let enterElement: HTMLElement = null;
const onListDragEnter = (e) => {
  e.stopPropagation();
  const nodeElement = searchParentElementByClassName(
    e.target as any,
    'mind-graph-node',
    ['mind-graph-node-children', 'mind-graph-node-sub-children']
  );

  if (nodeElement && nodeElement !== enterElement) {
    const enterContainer = searchParentElementByClassName(nodeElement, [
      'mind-graph-node-children',
      'mind-graph-node-sub-children',
    ]);
    if (containerElement && enterContainer === containerElement) {
      nodeElement.style.borderColor = 'red';
      if (enterElement) {
        enterElement.style.borderColor = 'transparent';
      }
      enterElement = nodeElement;
    } else {
      nodeElement.style.borderColor = 'transparent';
      enterElement = null;
    }
  }
};
const onListDragEnd = () => {
  if (enterElement) {
    enterElement.style.borderColor = 'transparent';
  }
};
const onListDragLevel = (e) => {
  const nodeElement = searchParentElementByClassName(
    e.target as any,
    'mind-graph-node'
  );
  if (nodeElement === e.target) {
    nodeElement.style.borderColor = 'transparent';
  } else {
    if (enterElement) {
      enterElement.style.borderColor = 'red';
    }
  }
};
export function useGraphNodeDrop(rules, node, refreshGraph) {
  const t = useLocale(i18n);
  const { projectId, flowId } = useUrlParams();
  const { dragItem, setDroping, setDragItem } = useDragContext();
  const { refreshTrash } = useTrash();
  const { RU } = useRedoUndo();
  const ref = useRef<HTMLDivElement>();

  const onDragOver = useCallback((evt) => {
    evt.preventDefault();
    evt.stopPropagation();
    if (ref.current.classList.contains('drop-node-anchor')) {
      ref.current.classList.add('drop-node-in');
    }
  }, []);
  const onDragStart = useCallback(
    (evt) => {
      evt.stopPropagation();
      setDragItem({
        type: 'node_frame',
        item: { key: node.id, type: node.type, data: { breakpoint: node } },
      });
      const listElement = searchParentElementByClassName(evt.target, [
        'mind-graph-node-children',
        'mind-graph-node-sub-children',
      ]);
      if (listElement) {
        containerElement = listElement;
        containerElement.addEventListener('dragenter', onListDragEnter);
        containerElement.addEventListener('dragend', onListDragEnd);
        containerElement.addEventListener('dragleave', onListDragLevel);
      }
    },
    [node, setDragItem]
  );
  const onDragEnter = useCallback((evt) => {
    evt.preventDefault();
    evt.stopPropagation();
    if (enterElement) {
      enterElement.style.borderColor = 'transparent';
      enterElement = null;
    }
  }, []);
  const onDragLeave = useCallback((evt) => {
    evt.preventDefault();
    evt.stopPropagation();
    ref.current.classList.remove('drop-node-in');
  }, []);
  const onDragEnd = useCallback((evt) => {
    evt.stopPropagation();
    const fromElement = searchParentElementByClassName(
      evt.target,
      'mind-graph-node'
    );
    if (enterElement && enterElement !== fromElement) {
      const fromId = fromElement.dataset['nodeId'];
      const toId = enterElement.dataset['nodeId'];
      if (fromId !== null && toId !== null && fromId !== toId) {
        // resort node
        setDroping(true);
        const moveData = { c1: fromId, c2: toId };
        moveNode(moveData)
          .then(() => {
            Message.success(t['message.create.success']);
            refreshGraph();
            RU.push({
              type: 'move_node',
              changed: moveData,
              dependencies: {
                projectId,
                flowId,
              },
            });
          })
          .finally(() => setDroping(false));
      }
    }
    setDragItem(null);
    if (containerElement) {
      containerElement.removeEventListener('dragenter', onListDragEnter);
      containerElement.removeEventListener('dragend', onListDragEnd);
      containerElement.removeEventListener('dragleave', onListDragLevel);
    }
    enterElement = null;
    containerElement = null;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const handleDrop = (evt) => {
      evt.preventDefault();
      evt.stopPropagation();
      if (dragItem && ref.current.classList.contains('drop-node-anchor')) {
        ref.current.classList.remove('drop-node-in');
        if (dragItem.type === 'trash_frame') {
          setDroping(true);
          const form = findParentForm(node);
          moveOutTrash(projectId, flowId, dragItem.item.key, node.id, form?.id)
            .then(() => {
              Message.success(t['message.create.success']);
              refreshGraph();
              refreshTrash();
              RU.push({
                type: 'trash_out',
                changed: normalGraphNode(dragItem.item.data.breakpoint),
                dependencies: {
                  projectId,
                  flowId,
                  parent: normalGraphNode(node),
                  children: [],
                  formId: form?.id,
                },
              });
            })
            .finally(() => setDroping(false));
        }
        if (dragItem.type === 'node_frame') {
          setDroping(true);
          const before = normalGraphNode(dragItem.item.data.breakpoint);
          const after = {
            ...before,
            parentId: node.id,
          };
          updateComponent(
            projectId,
            dragItem.item.type as any,
            dragItem.item.key,
            after
          )
            .then(() => {
              Message.success(t['message.create.success']);
              refreshGraph();
              RU.push({
                type: 'update_node',
                changed: {
                  before,
                  after,
                },
                dependencies: {
                  projectId,
                  flowId,
                },
              });
            })
            .finally(() => setDroping(false));
        }
        if (dragItem.type === 'copy_frame') {
          setDroping(true);
          const clone = cloneDeep(dragItem.item.data.breakpoint);
          clone.id = null;
          clone.parentId = node.id;

          createDefaultComponent(projectId, dragItem.item.type as any, clone)
            .then(([component, children]) => {
              Message.success(t['message.create.success']);
              refreshGraph();
              RU.push({
                type: 'add_node',
                changed: component,
                dependencies: {
                  projectId,
                  flowId,
                  children,
                  parent: normalGraphNode(node),
                },
              });
            })
            .finally(() => setDroping(false));
        }
        if (dragItem.type === 'favorite_frame') {
          setDroping(true);
          const clone = cloneDeep((dragItem.item.data as any).items);
          const idMap = keyBy(clone, 'id');
          const root = clone.find((c) => !idMap[c.parentId]);
          let others = clone.filter((c) => idMap[c.parentId]);
          if (root) {
            root.parentId = node.id;
            root.projectId = projectId;
            root.rootComponentId = flowId;
            others = others.map((o) => ({
              ...o,
              projectId,
              rootComponentId: flowId,
            }));
            doFavoritePaste([root, ...others], projectId, flowId)
              .then((newNodes) => {
                Message.success(t['message.create.success']);
                refreshGraph();
                RU.push({
                  type: 'do_favorite',
                  changed: {
                    after: newNodes,
                  },
                  dependencies: {
                    projectId,
                    flowId,
                  },
                });
              })
              .finally(() => setDroping(false));
          }
        }
      }
    };
    if (dragItem) {
      ref.current.addEventListener('drop', handleDrop);
    }
    return () => {
      if (ref.current) {
        // eslint-disable-next-line react-hooks/exhaustive-deps
        ref.current.removeEventListener('drop', handleDrop);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dragItem, node.id, t]);

  useEffect(() => {
    if (dragItem) {
      const {
        type,
        data: { breakpoint },
      } = dragItem.item;
      const rule = rules.find((r) => r.key === type);
      const breakpointChildren = expendChildren(breakpoint).map(({ id }) => id);
      if (rule && !rule.hidden && !breakpointChildren.includes(node.id)) {
        ref.current.classList.add('drop-node-anchor');
      }
    } else {
      ref.current.classList.remove('drop-node-anchor');
    }
  }, [dragItem, rules, node.id]);
  return useMemo(
    () => ({
      className: [className, node.id].join(' '),
      ref,
      onDragEnter,
      onDragLeave,
      onDragOver,
      draggable: true,
      onDragStart,
      onDragEnd,
      'data-id': node.id,
    }),
    [node.id, onDragEnd, onDragEnter, onDragLeave, onDragOver, onDragStart]
  );
}
