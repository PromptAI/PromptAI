import { cloneDeep } from 'lodash';

interface TreeNode {
  id: string;
  parentId: string;
}
type Callback<T> = (item: T) => void;
export function treeForEach<T extends TreeNode>(
  tree: T[],
  cb: Callback<T>,
  mode = 'BFS',
  childrenKeys = ['children', 'subChildren']
) {
  // 深度优先遍历 depth first search
  function DFS(treeData: T[]) {
    // eslint-disable-next-line
    for (const item of treeData) {
      cb(item);
      for (const childrenKey of childrenKeys) {
        if (Array.isArray(item[childrenKey])) {
          DFS(item[childrenKey]);
        }
      }
    }
  }
  // 广度优先遍历 breadth first search
  function BFS(treeData: T[]) {
    const queen = treeData;
    while (queen.length > 0) {
      const item = queen.shift();
      cb(item);
      for (const childrenKey of childrenKeys) {
        if (Array.isArray(item[childrenKey])) {
          queen.push(...item[childrenKey]);
        }
      }
    }
  }
  const clone = cloneDeep(tree);
  if (mode === 'BFS') {
    BFS(clone);
  } else {
    DFS(clone);
  }
}

type PromiseCallback<T> = (item: T) => Promise<void>;
export async function promiseTreeForEach<T extends TreeNode>(
  tree: T[],
  cb: PromiseCallback<T>,
  mode = 'BFS',
  childrenKeys = ['children', 'subChildren']
) {
  // 深度优先遍历 depth first search
  async function DFS(treeData: T[]) {
    // eslint-disable-next-line
    for (const item of treeData) {
      await cb(item);
      for (const childrenKey of childrenKeys) {
        if (Array.isArray(item[childrenKey])) {
          DFS(item[childrenKey]);
        }
      }
    }
  }
  // 广度优先遍历 breadth first search
  async function BFS(treeData: T[]) {
    const queen = treeData;
    while (queen.length > 0) {
      const item = queen.shift();
      await cb(item);
      for (const childrenKey of childrenKeys) {
        if (Array.isArray(item[childrenKey])) {
          queen.push(...item[childrenKey]);
        }
      }
    }
  }
  const clone = cloneDeep(tree);
  if (mode === 'BFS') {
    await BFS(clone);
  } else {
    await DFS(clone);
  }
}
interface ChildrenNode {
  id: string;
  children?: ChildrenNode[];
  subChildren?: ChildrenNode[];
}
export function findAllChildren(node: ChildrenNode[], temp: string[]) {
  node?.forEach((n) => {
    temp.push(n.id);
    findAllChildren([...(n.children || []), ...(n.subChildren || [])], temp);
  });
}
export function expendTree(root, temp = []) {
  const tempd = temp || [];
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { children, parent, subChildren, ...node } = root;
  if (children && children.length > 0) {
    children.forEach((child) => expendTree(child, tempd));
  }
  if (subChildren && subChildren.length > 0) {
    subChildren.forEach((child) => expendTree(child, tempd));
  }
  tempd.push(node);
  return tempd;
}
