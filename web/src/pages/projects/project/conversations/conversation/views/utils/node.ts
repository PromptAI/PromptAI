import { isEmpty, omit, uniqBy } from 'lodash';
import { NodeProps } from '../types';
import { expendTree } from '@/core-next/utils';
import { GraphTreeValue } from '@/core-next/types';
import { decodeAttachmentText } from '@/utils/attachment';
import { treeForEach } from '@/utils/tree';
import { Types, createComponent, updateComponent } from '@/api/components';
import ru, { RU_TYPE } from '../../features/ru';
import { FlowIcon } from '../flow';
import { UserIcon } from '../user';
import { BotIcon } from '../bot';
import { BreakIcon } from '../break';
import { ConditionIcon } from '../condition';
import { ConfirmIcon } from '../confirm';
import { FieldIcon } from '../field';
import { FormIcon } from '../form';
import { InterruptIcon } from '../interrupt';
import { RhetoricalIcon } from '../rhetorical';
import { SlotsIcon } from '../slots';
import { GotoIcon } from '../goto';
import Openai from "@/assets/openai_icon.svg";
import {
  GptAbortIcon,
  GptCompletedIcon,
  GptFormIcon,
  GptFunctionIcon,
  GptFunctionsIcon,
  GptSlotIcon,
  GptSlotsIcon,
} from '../gpt';
import { IconStar } from '@arco-design/web-react/icon';

export function unwrap(node: Partial<NodeProps>) {
  return omit({ ...node, data: omit(node.data, ['children', 'parent']) }, [
    'children',
    'parent',
  ]);
}
export function parseResponseOfCreate(nodes: any[], type: string) {
  const node = nodes.find((n) => n.type === type);
  const children = nodes.filter((n) => n.type !== type);
  return [node, children];
}
export function expendChildren(root, temp = []) {
  const nodes = expendTree(root, temp);
  return nodes.filter((n) => n.id !== root.id);
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
export function expendChildren4BreakNode(breakNode, form, children) {
  const {
    data: { conditionId },
  } = breakNode;
  const condition = form.children?.find((c) => c.id === conditionId);
  if (condition) {
    const conditionChildren = expendChildren(condition);
    children = [...children, ...conditionChildren, unwrap(condition)];
  }
  return children;
}
const DEFAULT_LABEL = '-';
export function getNodeLabel(node: any): string {
  if (!node.data) return DEFAULT_LABEL;
  switch (node.type) {
    case 'conversation':
      return node.data.name || DEFAULT_LABEL;
    case 'user':
      return node.data.examples?.[0]?.text || DEFAULT_LABEL;
    case 'bot':
      if (isEmpty(node.data.responses)) return DEFAULT_LABEL;
      const { content: { text } = { text: undefined }, type } =
        node.data.responses[0];
      if (type === 'attachment' && text) return decodeAttachmentText(text).name;
      if (type === 'action') return text || 'action';
      return text || DEFAULT_LABEL;
    case 'break':
      return node.data.name || 'Break';
    case 'condition':
      return node.data.name || 'Condition';
    case 'confirm':
      return 'Confirm';
    case 'field':
      return node.data?.slotDisplay || DEFAULT_LABEL;
    case 'form':
      return node.data.name || DEFAULT_LABEL;
    case 'interrupt':
      return 'Interrupts';
    case 'rhetorical':
      return node.data.responses?.[0]?.content.text || DEFAULT_LABEL;
    case 'slots':
      return 'Slots';
    case 'goto':
      return 'Goto';
    case 'gpt':
      return node.data.name;
    case 'form-gpt':
      return node.data.name;
    case 'slots-gpt':
      return 'Slots';
    case 'slot-gpt':
      return node.data?.slotDisplay || node.data?.slotName || DEFAULT_LABEL;
    case 'functions-gpt':
      return 'Functions';
    case 'function-gpt':
      return node.data.name;
    case 'complete-gpt':
      return 'Complete';
    case 'abort-gpt':
      return 'Abort';
  }
  return DEFAULT_LABEL;
}

export function getNodeIcon(node: any): any {
  switch (node.type) {
    case 'conversation':
      return FlowIcon;
    case 'user':
      return UserIcon;
    case 'bot':
      return BotIcon;
    case 'break':
      return BreakIcon;
    case 'condition':
      return ConditionIcon;
    case 'confirm':
      return ConfirmIcon;
    case 'field':
      return FieldIcon;
    case 'form':
      return FormIcon;
    case 'interrupt':
      return InterruptIcon;
    case 'rhetorical':
      return RhetoricalIcon;
    case 'slots':
      return SlotsIcon;
    case 'goto':
      return GotoIcon;
    case 'form-gpt':
      return GptFormIcon;
    case 'slots-gpt':
      return GptSlotsIcon;
    case 'slot-gpt':
      return GptSlotIcon;
    case 'functions-gpt':
      return GptFunctionsIcon;
    case 'function-gpt':
      return GptFunctionIcon;
    case 'complete-gpt':
      return GptCompletedIcon;
    case 'abort-gpt':
      return GptAbortIcon;
    case 'gpt':
      return Openai;
    default:
      return IconStar;
  }
}

export const getBreakCount = (currentForm, breakCountTemp = 0) => {
  const { children } = currentForm;
  const interrrupt = children.find((s) => s.type === 'interrupt');
  if (!interrrupt) return breakCountTemp;
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

export const findRoot = (node) => {
  let p = node;
  while (p.parent) {
    p = p.parent;
  }
  return p;
};
export const findClosestUserNode = (node) => {
  let p = node;
  while ((p = p.parent)) {
    if (['user', 'option'].includes(p.type)) return p;
  }
  return null;
};
// find the nodes of can be goto some target nodes.
export function findGotoFilterTargets(node: any) {
  function findPathParentNodes(start: any) {
    const ns = [];
    let p = start;
    while ((p = p.parent)) {
      ns.push(p);
      if (p.type === 'user' || p.type === 'abort-gpt') break;
    }
    return ns;
  }
  function findRoot(start: any) {
    let p = start;
    while (p) {
      if (!p.parent) return p;
      p = p.parent;
    }
    return p;
  }
  // current node
  const currentPathNodes = findPathParentNodes(node);
  // other goto path
  const root = findRoot(node);
  const existedGotos = [];
  treeForEach([root], (n) => {
    if (n.type === 'goto') {
      existedGotos.push(n);
    }
  });
  const otherPathNodes = existedGotos.flatMap((enode) =>
    findPathParentNodes(enode)
  );

  return uniqBy([node, ...currentPathNodes, ...otherPathNodes], 'id');
}

export const getAllNodes = (node) => {
  const root = findRoot(node);
  const stack = [root],
    result = [];
  let n;
  while ((n = stack.pop())) {
    if (n.children) {
      stack.unshift(...n.children);
      result.push(n);
    }
  }
  return result;
};

export async function updateNodeFetch(
  node: Partial<NodeProps>,
  data: any,
  type: Types,
  dependencies: { projectId: string; flowId: string }
) {
  const after = await updateComponent(dependencies.projectId, type, node.id, {
    ...unwrap(node),
    data,
  });
  ru.push({
    type: RU_TYPE.UPDATE_NODE,
    changed: {
      after,
      before: unwrap(node),
    },
    dependencies,
  });
}
export async function createNodeFetch(
  node: Partial<NodeProps>,
  data: any,
  type: string,
  dependencies: { projectId: string; flowId: string }
) {
  const nodes = await createComponent(dependencies.projectId, type as any, {
    data,
    parentId: node.id,
  });
  const [changed, children] = parseResponseOfCreate(nodes, type);
  ru.push({
    type: RU_TYPE.ADD_NODE,
    changed,
    dependencies: {
      children,
      ...dependencies,
    },
  });
}
