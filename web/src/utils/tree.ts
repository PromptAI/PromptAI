import { cloneDeep, keyBy, omit } from 'lodash';

export function flattenTree<T extends object>(
  trees: T[],
  childrenKey: string | 'children' = 'children'
): T[] {
  const collection: T[] = [];
  treeForEach(trees, (node: T) =>
    collection.push(omit(node, childrenKey) as any)
  );
  return collection;
}
export function DFS<T>(
  treeData: T[],
  cb: (node: T) => void,
  childrenKey: string | 'children' = 'children'
) {
  // eslint-disable-next-line
  for (const item of treeData) {
    cb(item);
    if (Array.isArray(item[childrenKey])) {
      DFS(item[childrenKey], cb);
    }
  }
}
export function BFS<T>(
  treeData: T[],
  cb: (node: T) => void,
  childrenKey: string | 'children' = 'children'
) {
  const queen = [...treeData];
  while (queen.length > 0) {
    const item = queen.shift();
    cb(item);
    if (Array.isArray(item[childrenKey])) {
      queen.push(...item[childrenKey]);
    }
  }
}
export function treeForEach<T>(
  tree: T[],
  cb: (node: T) => void,
  mode: 'BFS' | 'DFS' = 'BFS',
  childrenKey: 'children' | string = 'children'
) {
  mode === 'BFS' ? BFS(tree, cb, childrenKey) : DFS(tree, cb, childrenKey);
}
interface TreeNode {
  children?: any[];
  id: string;
  parentId?: string;
  parent?: string;
}
export function buildTree<T extends TreeNode>(data: T[]) {
  if (!data) return [];
  const tars = cloneDeep(data);
  const res: T[] = [];
  const idsmap = keyBy(tars, (n) => n.id);
  let node = tars.shift();
  while (node) {
    const mapNode = idsmap[node.parent || node.parentId];
    if (node.parent || node.parentId) {
      if (!mapNode) {
        res.push(node);
      } else {
        mapNode.children = mapNode.children || [];
        mapNode.children.push(node);
      }
    } else {
      res.push(node);
    }
    node = tars.shift();
  }
  return res;
}
