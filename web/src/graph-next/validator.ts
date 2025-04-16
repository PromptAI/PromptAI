import { isEmpty } from 'lodash';
import {
  GraphBot,
  GraphBreak,
  GraphCondition,
  GraphConfirm,
  GraphConversation,
  GraphFaq,
  GraphField,
  GraphForm,
  GraphIntentNext,
  GraphNode,
  GraphOption,
  GraphRhetoricalNext,
  GraphSlots,
} from './type';
import i18n from './validator-local';

export interface Rule<T> {
  message: string;
  validator: (node: T, message: string) => Promise<T>;
}
export interface Rules<T> {
  rules: Rule<T>[];
}
export async function validatorChain<T>(rules: Rule<T>[], node: T) {
  return rules.reduce(
    (p, c) => p.then((n) => c.validator(n, c.message)),
    Promise.resolve(node)
  );
}

/// defined validator

export async function intentNotEmptyValidator(
  node: GraphIntentNext,
  message: string
) {
  return new Promise<GraphIntentNext>((resolve, reject) => {
    isEmpty(node.data.examples) ||
    node.data.examples.some((example) => isEmpty(example.text))
      ? reject(message)
      : resolve(node);
  });
}
export async function intentNeedBotValidator(
  node: GraphIntentNext,
  message: string
) {
  return new Promise<GraphIntentNext>((resolve, reject) => {
    if (node.afterRhetorical) {
      resolve(node);
    }
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}
export async function botNotEmptyValidator(node: GraphBot, message: string) {
  return new Promise<GraphBot>((resolve, reject) => {
    isEmpty(node.data.responses) ||
    node.data.responses.some((res) => isEmpty(res.content.text))
      ? reject(message)
      : resolve(node);
  });
}
export async function optionNotEmptyValidator(
  node: GraphOption,
  message: string
) {
  return new Promise<GraphOption>((resolve, reject) => {
    isEmpty(node.data.examples) || node.data.examples.some((ex) => isEmpty(ex))
      ? reject(message)
      : resolve(node);
  });
}
export async function optionNeedBotValidator(
  node: GraphOption,
  message: string
) {
  return new Promise<GraphOption>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}
export async function formNotEmptyValidator(node: GraphForm, message: string) {
  return new Promise<GraphForm>((resolve, reject) => {
    isEmpty(node.data.name) ? reject(message) : resolve(node);
  });
}
export async function slotsNotEmptyValidator(
  node: GraphSlots,
  message: string
) {
  return new Promise<GraphSlots>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}
export async function fieldNotEmptyValidator(
  node: GraphField,
  message: string
) {
  return new Promise<GraphField>((resolve, reject) => {
    isEmpty(node.data.slotId) ? reject(message) : resolve(node);
  });
}
export async function rhetoricalNotEmptyValidator(
  node: GraphRhetoricalNext,
  message: string
) {
  return new Promise<GraphRhetoricalNext>((resolve, reject) => {
    isEmpty(node.data.responses) ||
    node.data.responses.some((res) => isEmpty(res.content.text))
      ? reject(message)
      : resolve(node);
  });
}
export async function rhetoricalNeedIntentValidator(
  node: GraphRhetoricalNext,
  message: string
) {
  return new Promise<GraphRhetoricalNext>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}

export async function breakNotEmptyValidator(
  node: GraphBreak,
  message: string
) {
  return new Promise<GraphBreak>((resolve, reject) => {
    isEmpty(node.data.name) ? reject(message) : resolve(node);
  });
}
export async function conditionNotEmptyValidator(
  node: GraphCondition,
  message: string
) {
  return new Promise<GraphCondition>((resolve, reject) => {
    isEmpty(node.data.name) ? reject(message) : resolve(node);
  });
}
export async function conditionNeedBotValidator(
  node: GraphCondition,
  message: string
) {
  return new Promise<GraphCondition>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}
export async function confirmNeedBotValidator(
  node: GraphConfirm,
  message: string
) {
  return new Promise<GraphConfirm>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}

export async function faqNotEmptyValidator(node: GraphFaq, message: string) {
  return new Promise<GraphFaq>((resolve, reject) => {
    isEmpty(node.data.name) ? reject(message) : resolve(node);
  });
}
export async function faqNeedIntentValidator(node: GraphFaq, message: string) {
  return new Promise<GraphFaq>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}
const faqFlowRules: Record<'faq-root' | 'user' | 'bot', Rules<GraphNode>> = {
  'faq-root': {
    rules: [
      { message: 'rules.checkEmpty', validator: faqNotEmptyValidator },
      {
        message: 'rules.checkNextNode',
        validator: faqNeedIntentValidator,
      },
    ],
  },
  user: {
    rules: [
      { message: 'rules.checkUserInput', validator: intentNotEmptyValidator },
      {
        message: 'rules.checkUserNextBot',
        validator: intentNeedBotValidator,
      },
    ],
  },
  bot: {
    rules: [{ message: 'rules.checkBot', validator: botNotEmptyValidator }],
  },
};

export function getFaqFlowRules(): any {
  const lang = window.localStorage.getItem('lang') || 'zh-CN';
  const t = i18n[lang];
  return Object.entries(faqFlowRules).reduce(
    (p, [key, val]) => ({
      ...p,
      [key]: {
        ...val,
        rules: val.rules.map((r) => ({ ...r, message: t[r.message] })),
      },
    }),
    {}
  );
}

export async function conversationNotEmptyValidator(
  node: GraphConversation,
  message: string
) {
  return new Promise<GraphConversation>((resolve, reject) => {
    isEmpty(node.data.name) ? reject(message) : resolve(node);
  });
}
export async function conversationNeedIntentValidator(
  node: GraphConversation,
  message: string
) {
  return new Promise<GraphConversation>((resolve, reject) => {
    isEmpty(node.children) ? reject(message) : resolve(node);
  });
}
type ConversationFlowRulesKey =
  | 'conversation'
  | 'user'
  | 'option'
  | 'bot'
  | 'form'
  | 'slots'
  | 'field'
  | 'rhetorical'
  | 'confirm'
  | 'break'
  | 'condition';
const conversationFlowRules: Record<
  ConversationFlowRulesKey,
  Rules<GraphNode>
> = {
  conversation: {
    rules: [
      { message: 'rules.checkEmpty', validator: conversationNotEmptyValidator },
      {
        message: 'rules.checkNextNode',
        validator: conversationNeedIntentValidator,
      },
    ],
  },
  user: {
    rules: [
      { message: 'rules.checkUserInput', validator: intentNotEmptyValidator },
      {
        message: 'rules.checkUserNextBot',
        validator: intentNeedBotValidator,
      },
    ],
  },
  option: {
    rules: [
      { message: 'rules.checkUserOption', validator: optionNotEmptyValidator },
      {
        message: 'rules.checkUserOprionNextBot',
        validator: optionNeedBotValidator,
      },
    ],
  },
  bot: {
    rules: [{ message: 'rules.checkBot', validator: botNotEmptyValidator }],
  },
  form: {
    rules: [{ message: 'rules.checkForm', validator: formNotEmptyValidator }],
  },
  slots: {
    rules: [
      {
        message: 'rules.checkSlotEmpty',
        validator: slotsNotEmptyValidator,
      },
    ],
  },
  field: {
    rules: [
      { message: 'rules.checkFieldEmtpy', validator: fieldNotEmptyValidator },
    ],
  },
  rhetorical: {
    rules: [
      {
        message: 'rules.checkRhetoricalEmpty',
        validator: rhetoricalNotEmptyValidator,
      },
      {
        message: 'rules.checkRhetoricalNextNode',
        validator: rhetoricalNeedIntentValidator,
      },
    ],
  },
  confirm: {
    rules: [
      {
        message: 'rules.checkConfirmNextBot',
        validator: confirmNeedBotValidator,
      },
    ],
  },
  break: {
    rules: [
      { message: 'rules.checkBreakEmpty', validator: breakNotEmptyValidator },
    ],
  },
  condition: {
    rules: [
      {
        message: 'rules.checkConditionEmpty',
        validator: conditionNotEmptyValidator,
      },
      {
        message: 'rules.checkConditionNextBot',
        validator: conditionNeedBotValidator,
      },
    ],
  },
};

export function getConversationFlowRules(): any {
  const lang = window.localStorage.getItem('lang') || 'zh-CN';
  const t = i18n[lang];
  return Object.entries(conversationFlowRules).reduce(
    (p, [key, val]) => ({
      ...p,
      [key]: {
        ...val,
        rules: val.rules.map((r) => ({ ...r, message: t[r.message] })),
      },
    }),
    {}
  );
}
