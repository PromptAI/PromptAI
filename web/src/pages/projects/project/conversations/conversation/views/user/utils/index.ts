import { IntentMapping, IntentNextData } from '@/graph-next/type';
import { InputWayEnum } from '../enums';
import { nanoid } from 'nanoid';
import { isEmpty } from 'lodash';

export function wrapFormValues(initialValues?: IntentNextData): any {
  if (!initialValues) return { display: InputWayEnum.INPUT };
  const examples = initialValues.examples?.slice();
  const mainExample = examples?.shift();
  const name = initialValues.name || mainExample?.text;
  return {
    ...initialValues,
    name,
    display: initialValues.display || InputWayEnum.INPUT,
    mainExample,
    examples
  };
}

export function unwrapFormValues({ mainExample, name, ...rest }: any): any {
  const mappingsEnable = rest.mappings && rest.mappings.length > 0;
  return {
    ...rest,
    mappingsEnable: mappingsEnable,
    examples: [mainExample, ...(rest.examples || [])],
    /// when the name is empty, we will be set the main-example`s value to it.
    name: isEmpty(name) ? mainExample.text : name
  };
}

export function createMapping(): IntentMapping {
  return {
    id: nanoid(),
    slotId: null,
    slotName: null,
    slotDisplay: null,
    type: null,
    enable: true
  };
}
