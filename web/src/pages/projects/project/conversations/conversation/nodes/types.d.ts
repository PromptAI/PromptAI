import { NodeDefinedProps } from '@/core-next/types';
import { RUInstance } from '../ru_context';
import { Item } from '../types';

export interface RelationNodeDefinedProps extends NodeDefinedProps {
  relations: string[];
  afterRhetorical?: string;
  linkedFrom?: { id: string; name: string };
  validatorError?: { errorCode: number; errorMessage: string };
  rootComponentId: string;
  projectId: string;
}
export interface BuildMenusParams {
  refreshTrash: () => void;
  t: any;
  haveTrash?: boolean;
  haveFavorite?: boolean;
  RU: RUInstance;
  props: RelationNodeDefinedProps;
  submitCopy: (data: Item) => void;
  refreshFavorites: () => void;
}
