import {
  createBot,
  createBreak,
  createField,
  createGoto,
  createIntentOption,
  createNextIntent,
  createReturn,
  createRhetorical,
  deleteComponent,
} from '@/api/components';
import {
  BotDataType,
  BotResponse,
  ComponentRelation,
  FieldData,
  GraphBot,
  GraphBreak,
  GraphField,
  GraphGoto,
  GraphIntentNext,
  GraphOption,
  GraphReturn,
  GraphRhetorical,
  IntentNextData,
  RhetoricalData,
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
export const defaultIntentData: IntentNextData = {
  name: '',
  examples: [],
  mappingsEnable: false,
  mappings: [],
  display: 'user_input',
};

interface NewIntentParams extends BaseParams<IntentNextData, GraphIntentNext> {
  afterRhetorical?: string;
  parent?: any;
}

export async function newIntent(params: NewIntentParams) {
  const {
    parent,
    projectId,
    parentId,
    relations,
    data,
    callback,
    afterRhetorical,
  } = params;
  nProgress.start();
  return createNextIntent(projectId, parentId, relations, data, afterRhetorical)
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        type: 'user',
        data,
        parent,
        relations,
        afterRhetorical,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function newOption(
  projectId: string,
  parentId: string,
  label: string,
  relations: string[],
  callback: Callback<GraphOption>
) {
  return createIntentOption(
    projectId,
    parentId,
    { examples: [label] },
    relations
  )
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        type: 'option',
        data: {
          examples: [label],
          description: '',
        },
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function delNode(
  projectId: string,
  ids: string[],
  callback: Callback<string[]>
) {
  nProgress.start();
  return deleteComponent(projectId, ids)
    .then(() => {
      callback(ids);
    })
    .finally(nProgress.done);
}

export async function newRhetorical(
  projectId: string,
  parentId: string,
  slot: string,
  rhetorical: string,
  relations: string[],
  callback: Callback<GraphRhetorical>
) {
  nProgress.start();
  const initialvalues: RhetoricalData = {
    slot,
    rhetorical,
    from_entity: [],
    from_intent: [],
    from_text: [],
    from_trigger_intent: [],
  };
  return createRhetorical(projectId, parentId, initialvalues, relations)
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        type: 'rhetorical',
        data: initialvalues,
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function newField(
  projectId: string,
  parentId: string,
  relations: string[],
  data: FieldData,
  callback: Callback<GraphField>
) {
  nProgress.start();
  return createField(projectId, parentId, data, relations)
    .then(({ id, validatorError }) => {
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

export async function newRhetoricalNext(
  projectId: string,
  parentId: string,
  relations: string[],
  responses: BotResponse<BotDataType>[],
  callback: Callback<GraphBot>
) {
  nProgress.start();
  return createRhetorical(projectId, parentId, { responses }, relations)
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        type: 'rhetorical',
        data: {
          responses,
        },
        relations,
        componentRelation: null,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function newResponse(
  projectId: string,
  parentId: string,
  relations: string[],
  responses: BotResponse<BotDataType>[],
  callback: Callback<GraphBot>
) {
  nProgress.start();
  const componentRelation: ComponentRelation = {
    usedByComponentRoots: [
      {
        rootComponentId: relations[0],
        componentId: null,
        rootComponentType: 'flow',
      },
    ],
  };
  return createBot(
    projectId,
    parentId,
    { responses },
    relations,
    componentRelation
  )
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        type: 'bot',
        data: {
          responses,
        },
        relations,
        componentRelation: {
          usedByComponentRoots: [
            {
              rootComponentId: relations[0],
              componentId: id,
              rootComponentType: 'flow',
            },
          ],
        },
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function newBreak(
  projectId: string,
  parentId: string,
  relations: string[],
  formId: string,
  name: string,
  callback: Callback<GraphBreak>
) {
  nProgress.start();
  return createBreak(
    projectId,
    parentId,
    { formId, name, color: '#ff0000' },
    relations
  )
    .then(({ id, data: { conditionId }, validatorError }) => {
      callback({
        id,
        parentId,
        type: 'break',
        data: {
          formId,
          conditionId,
          name,
          color: '#ff0000',
        },
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function newGoto(
  projectId: string,
  parentId: string,
  linkId: string,
  name: string,
  description: string,
  relations: string[],
  callback: Callback<GraphGoto>
) {
  nProgress.start();
  return createGoto(projectId, parentId, linkId, name, description, relations)
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        data: {
          name,
          linkId,
          description,
        },
        type: 'goto',
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export async function newReturn(
  projectId: string,
  parentId: string,
  relations: string[],
  callback: Callback<GraphReturn>
) {
  nProgress.start();
  return createReturn(projectId, parentId, relations)
    .then(({ id, validatorError }) => {
      callback({
        id,
        parentId,
        data: {},
        type: 'return',
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

export const defaultActionCode = `from typing import Any, Text, Dict, List
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher

class ActionHelloWorld(Action):
    def name(self) -> Text:
        return "action_hello_world"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        dispatcher.utter_message(text="Hello World!")

        return []

`;
