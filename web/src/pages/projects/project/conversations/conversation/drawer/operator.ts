import {
  updateBot,
  updateNextIntent,
  updateBreak as updateBreakApi,
  updateGoto as updateGotoApi,
  updateCondition as updateConditionApi,
  updateIntentOption,
  updateConversation,
  updateForm,
  updateField,
  updateRhetorical,
  updateComponent,
} from '@/api/components';
import {
  BotData,
  BotDataType,
  ConversationData,
  GraphBot,
  GraphBreak,
  GraphConversation,
  GraphForm,
  GraphIntentNext,
  GraphOption,
  IDName,
  IntentNextData,
  OptionData,
  FormData,
  BotResponseBaseContent,
  GraphCondition,
  GraphField,
  FieldData,
  ComponentRelation,
  GraphGoto,
} from '@/graph-next/type';
import nProgress from 'nprogress';

interface BaseParams<D, N> {
  projectId: string;
  parentId: string;
  relations: string[];
  data: D;
  callback: Callback<N>;
}

type Callback<T> = (val: T) => void;

interface UpdateParams<D, N> extends BaseParams<D, N> {
  id: string;
  linkedFrom?: IDName;
  afterRhetorical?: string;
}

export async function updateIntent(
  params: UpdateParams<IntentNextData, GraphIntentNext>
) {
  const {
    projectId,
    parentId,
    relations,
    data,
    id,
    linkedFrom,
    afterRhetorical,
    callback,
  } = params;
  nProgress.start();
  return updateNextIntent(
    projectId,
    id,
    parentId,
    relations,
    data,
    linkedFrom,
    afterRhetorical
  )
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'user',
        relations,
        data,
        linkedFrom,
        afterRhetorical,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function updateSlots(projectId, params, callback) {
  return updateComponent(projectId, 'slots', params.id, params).then(
    ({ validatorError }) => {
      callback({
        ...params,
        validatorError,
      });
    }
  );
}

export async function updateBreak(params: {
  id: string;
  projectId: string;
  parentId: string;
  data: {
    name: string;
    formId: string;
    color: string;
    conditionId?: string;
  };
  relations: string[];
  callback: Callback<GraphBreak>;
}) {
  const { id, projectId, parentId, data, relations, callback } = params;
  return updateBreakApi(id, projectId, parentId, data, relations).then(
    ({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'break',
        data,
        relations,
        validatorError,
      });
    }
  );
}

export async function updateGoto(params: {
  id: string;
  projectId: string;
  parentId: string;
  data: {
    name: string;
    linkId: string;
    description: string;
  };
  relations: string[];
  callback: Callback<GraphGoto>;
}) {
  const { id, projectId, parentId, data, relations, callback } = params;
  return updateGotoApi(id, projectId, parentId, data, relations).then(
    ({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'goto',
        data,
        relations,
        validatorError,
      });
    }
  );
}

export async function updateCondition(params: {
  id: string;
  projectId: string;
  parentId: string;
  data: {
    name: string;
    color: string;
  };
  relations: string[];
  callback: Callback<GraphCondition>;
}) {
  const { id, projectId, parentId, data, relations, callback } = params;
  return updateConditionApi(id, projectId, parentId, data, relations).then(
    ({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'condition',
        data,
        relations,
        validatorError,
      });
    }
  );
}

export async function updateResponse({
  projectId,
  id,
  data,
  parentId,
  relations,
  linkedFrom,
  callback,
}: UpdateParams<BotData<BotDataType>, GraphBot>) {
  nProgress.start();
  const componentRelation: ComponentRelation = {
    usedByComponentRoots: [
      {
        rootComponentId: relations[0],
        componentId: id,
        rootComponentType: 'flow',
      },
    ],
  };
  return updateBot(
    projectId,
    id,
    data,
    relations,
    parentId,
    componentRelation,
    linkedFrom
  )
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'bot',
        data,
        relations,
        componentRelation,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function updateRhetoricalNode({
  projectId,
  id,
  data,
  parentId,
  relations,
  callback,
}: UpdateParams<BotData<BotResponseBaseContent>, GraphBot>) {
  nProgress.start();
  return updateRhetorical(projectId, parentId, id, data, relations)
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'rhetorical',
        data,
        relations,
        componentRelation: null,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function updateOptinal({
  projectId,
  id,
  data,
  relations,
  parentId,
  callback,
}: UpdateParams<OptionData, GraphOption>) {
  nProgress.start();
  return updateIntentOption(projectId, id, data, relations, parentId)
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'option',
        data,
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function updateFlowRoot({
  projectId,
  id,
  data,
  parentId,
  relations,
  callback,
}: UpdateParams<ConversationData, GraphConversation>) {
  nProgress.start();
  return updateConversation(projectId, id, data, relations, '')
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'conversation',
        data,
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function updateFormNode({
  projectId,
  id,
  data,
  parentId,
  relations,
  callback,
}: UpdateParams<FormData, GraphForm>) {
  nProgress.start();
  return updateForm(projectId, parentId, id, data, relations)
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'form',
        data,
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function updateFieldNode({
  projectId,
  id,
  data,
  parentId,
  relations,
  callback,
}: UpdateParams<FieldData, GraphField>) {
  nProgress.start();
  return updateField(projectId, id, parentId, data, relations)
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'field',
        data,
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}
