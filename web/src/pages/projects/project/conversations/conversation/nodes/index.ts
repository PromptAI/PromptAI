import Flow, { FlowView } from './flow';
import Intent, { UserView } from './intent';
import Response, { ResponseView } from './response';
import NodeForm, { FormView } from './form';
import GPTNodeForm, { GPTFormView } from './gpt-form';
import Slots, { SlotsView } from './slots';
import Rhetorical, { RhetoricalView } from './rhetorical';
import Field, { FieldView } from './field';
import Interrupt, { InterruptView } from './interrupt';
import Break, { BreakView } from './break';
import Confirm, { ConfirmView } from './confirm';
import Condition, { ConditionView } from './condition';
import Goto, { GotoView } from './goto';
import { NodeDefined } from '@/core-next/types';
import './index.css';
import NodeGPTFunctions, { FunctionsView } from './gpt-functions';
import NodeGPTComplete, { CompleteView } from './gpt-complete';
import NodeGPTAbort, { AbortView } from './gpt-abort';
import NodeGPTFunction, { FunctionView } from './gpt-function';
import NodeGPTSlot, { GptSlotView } from './gpt-slot';
import NodeGPTSlots, { GptSlotsView } from './gpt-slots';
import NodeGPT, { GPTView } from './gpt';

export default function getNodes(
  props?,
  isDisplay = false
): Record<string, NodeDefined> {
  return {
    conversation: {
      component: isDisplay ? FlowView : Flow,
      props,
    },
    user: {
      component: isDisplay ? UserView : Intent,
      props,
    },
    bot: {
      component: isDisplay ? ResponseView : Response,
      props,
    },
    form: {
      component: isDisplay ? FormView : NodeForm,
      props,
    },
    slots: {
      component: isDisplay ? SlotsView : Slots,
      props,
    },
    rhetorical: {
      component: isDisplay ? RhetoricalView : Rhetorical,
      props,
    },
    confirm: {
      component: isDisplay ? ConfirmView : Confirm,
      props,
    },
    condition: {
      component: isDisplay ? ConditionView : Condition,
      props,
    },
    interrupt: {
      component: isDisplay ? InterruptView : Interrupt,
      props,
    },
    break: {
      component: isDisplay ? BreakView : Break,
      props,
    },
    field: {
      component: isDisplay ? FieldView : Field,
      props,
    },
    goto: {
      component: isDisplay ? GotoView : Goto,
      props,
    },
    ['form-gpt']: {
      component: isDisplay ? GPTFormView : GPTNodeForm,
      props,
    },
    ['gpt']: {
      component: isDisplay ? GPTView : NodeGPT,
      props,
    },
    ['slots-gpt']: {
      component: isDisplay ? GptSlotsView : NodeGPTSlots,
      props,
    },
    'slot-gpt': {
      component: isDisplay ? GptSlotView : NodeGPTSlot,
      props,
    },
    'abort-gpt': {
      component: isDisplay ? AbortView : NodeGPTAbort,
      props,
    },
    'functions-gpt': {
      component: isDisplay ? FunctionsView : NodeGPTFunctions,
      props,
    },
    'function-gpt': {
      component: isDisplay ? FunctionView : NodeGPTFunction,
      props,
    },
    'complete-gpt': {
      component: isDisplay ? CompleteView : NodeGPTComplete,
      props,
    },
  };
}
