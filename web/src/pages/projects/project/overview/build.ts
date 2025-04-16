import { GraphNode } from '@/graph-next/type';
import { nanoid } from 'nanoid';

function getProjectNode({ id, ...data }): GraphNode {
  return {
    id,
    data,
    type: 'project',
    relations: [],
    parentId: null,
    validatorError: null,
  };
}
function getFallbackNode(project): GraphNode {
  return {
    id: nanoid(),
    data: {
      fallback: project.fallback,
      fallbackType: project.fallbackType,
      webhooks: project.webhooks,
      actions: project.actions,
      talk2bits: project.talk2bits,
      fallbackButtons: project.fallbackButtons,
    },
    type: 'fallback',
    relations: [project.id],
    parentId: project.id,
    validatorError: null,
  };
}
function getSampleNodes(project, faqs): GraphNode[] {
  return (
    faqs?.map((f) => ({
      id: f.id,
      data: {
        ...f.data,
      },
      type: 'sample',
      relations: [project.id],
      parentId: project.id,
    })) || []
  );
}
function getComplexNode(project): GraphNode {
  return {
    id: nanoid(),
    data: {
      branchWelcome: project.branchWelcome,
      showSubNodesAsOptional: project.showSubNodesAsOptional,
      showSubNodesCount: project.showSubNodesCount,
    },
    type: 'complex',
    relations: [project.id],
    parentId: project.id,
    validatorError: null,
  };
}
function getBranchNode(complex, project, flow): GraphNode {
  return {
    id: flow.id,
    data: flow.data,
    type: flow.type,
    relations: [project.id],
    parentId: complex.id,
    validatorError: null,
  };
}
export default function build(project, flows, faqs): GraphNode[] {
  const complex = getComplexNode(project);
  return [
    getProjectNode(project),
    ...getSampleNodes(project, faqs),
    complex,
    getFallbackNode(project),
    ...flows.map((flow) => getBranchNode(complex, project, flow)),
  ];
}
