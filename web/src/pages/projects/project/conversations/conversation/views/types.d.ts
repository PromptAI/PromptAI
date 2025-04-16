import { NodeDefinedProps } from '@/core-next/types';

export interface NodeProps extends NodeDefinedProps {
  relations: string[];
  afterRhetorical?: string;
  linkedFrom?: { id: string; name: string };
  validatorError?: { errorCode: number; errorMessage: string };
  rootComponentId: string;
  projectId: string;
}
export type NodeRule = {
  key: string;
  show: boolean;
};
