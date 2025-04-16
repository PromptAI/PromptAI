import {
  createBot,
  createBreak,
  createComponent,
  createField,
  createForm,
  createGoto,
  createIntentOption,
  createNextIntent,
} from '@/api/components';
import randomColor from '@/utils/randomColor';
import { nanoid } from 'nanoid';

export function parseCreateComponentRes(nodes: any[], type: string) {
  const node = nodes.find((n) => n.type === type);
  const children = nodes.filter((n) => n.type !== type);
  return [node, children];
}
const DEFAULT_INTENT: any = {
  name: '',
  examples: [],
  mappingsEnable: false,
  mappings: [],
  display: 'user_input',
};
const DEFAULT_BOT = {
  responses: [
    { id: nanoid(), type: 'text', content: { text: '' }, delay: 500 },
  ],
};
export async function createDefaultComponent(projectId, type, other) {
  return createComponent(projectId, type, other).then((nodes) =>
    parseCreateComponentRes(nodes, type)
  );
}
export async function creaeteDefaultBot(projectId, parentId, relations) {
  return createBot(projectId, parentId, DEFAULT_BOT, relations, {
    usedByComponentRoots: [
      {
        rootComponentId: relations[0],
        componentId: null,
        rootComponentType: 'flow',
      },
    ],
  }).then((nodes) => parseCreateComponentRes(nodes, 'bot'));
}
export async function createDefaultUser(projectId, parentId, relations) {
  return createNextIntent(projectId, parentId, relations, DEFAULT_INTENT).then(
    (nodes) => parseCreateComponentRes(nodes, 'user')
  );
}
export async function createDefaultRhetoricalUser(
  projectId,
  parentId,
  data,
  relations,
  afterRhetorical
) {
  return createNextIntent(
    projectId,
    parentId,
    relations,
    data,
    afterRhetorical
  ).then((nodes) => parseCreateComponentRes(nodes, 'user'));
}
export async function createDefaultForm(projectId, parentId, data, relations) {
  return createForm(projectId, parentId, data, relations).then((nodes) =>
    parseCreateComponentRes(nodes, 'form')
  );
}

export async function createDefaultBreak(projectId, parentId, data, relations) {
  return createBreak(
    projectId,
    parentId,
    { ...data, color: data.color || randomColor() },
    relations
  ).then((nodes) => parseCreateComponentRes(nodes, 'break'));
}
export async function createDefaultOption(
  projectId,
  parentId,
  data,
  relations
) {
  return createIntentOption(projectId, parentId, data, relations).then(
    (nodes) => parseCreateComponentRes(nodes, 'user')
  );
}
export async function createDefaultField(projectId, parentId, data, relations) {
  return createField(projectId, parentId, data, relations).then((nodes) =>
    parseCreateComponentRes(nodes, 'field')
  );
}
export async function createDefaultGoto(
  projectId,
  parentId,
  linkId,
  name,
  description,
  relations
) {
  return createGoto(
    projectId,
    parentId,
    linkId,
    name,
    description,
    relations
  ).then((nodes) => parseCreateComponentRes(nodes, 'goto'));
}
