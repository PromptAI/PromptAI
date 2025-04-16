import {cancelTask, hasTaskRunning, newStartTrain, publishRecordRunning} from '@/api/rasa';
import produce from 'immer';
import { isEmpty } from 'lodash';
import Observable, { Observer } from './Observable';

type RunningTaskEvents =
  | 'state_change'
  | 'create_model'
  | 'cancel_task'
  | 'cancel_state_change'
  | 'ready_model';
export class RunningPublishRecordObserver extends Observer<RunningTaskEvents> {}
type State = {
  operating: boolean;
  canceling: boolean;
  running: boolean;
  componentIds: string[];
};
const DEFAULT_COMPONENT_IDS = [];
export const RUNNING_TASK_DEFAULT_STATE: State = {
  operating: false,
  canceling: false,
  running: false,
  componentIds: DEFAULT_COMPONENT_IDS,
};
const DEFAULT_TIMEOUT = 2 * 1000;
export default class RunningPublishRecordObservable extends Observable<RunningTaskEvents> {
  private state: State = RUNNING_TASK_DEFAULT_STATE;
  private timeout = DEFAULT_TIMEOUT;
  private interval: any = null;
  private recordId: string | null;

  constructor(timeout = DEFAULT_TIMEOUT) {
    super();
    this.timeout = timeout;
  }
  private setState(state: Partial<State>) {
    this.state = produce(this.state, (draft) => {
      Object.entries(state).forEach(([key, value]) => {
        draft[key] = value;
      });
    });
    this._emit('state_change', this.state);
  }
  private async initListen() {
    const data = await publishRecordRunning();
    if (isEmpty(data)) {
      this.recordId = null;
      this.setState({ running: false, componentIds: DEFAULT_COMPONENT_IDS });
    } else if (data[0].status === "running") {
      this.recordId = data[0].id;
      this.setState({
        running: true,
        componentIds: data[0]?.properties.publishRoots || DEFAULT_COMPONENT_IDS,
      });
    }
  }
  async triggerRun(componentIds: string[], projectId: string) {
    this.setState({ operating: true });
    let state: Partial<State> = {
      operating: false,
      running: false,
      canceling: false,
      componentIds,
    };
    try {
      // 开始新的训练
      const data = await newStartTrain({ componentIds, projectId });
      if (isEmpty(data?.newRecord)) {
        this._emit('ready_model');
        state = {
          operating: false,
          running: false,
          canceling: false,
          componentIds,
        };
      } else {
        this._emit('create_model', data);
        state = {
          operating: false,
          running: false,
          canceling: false,
          componentIds,
        };
      }
    } catch (e) {
      //
    }
    this.setState(state);
  }
  async triggerStop(recordId?: string) {
    const target = recordId || this.recordId;
    if (target) {
      this._emit('cancel_state_change', true);
      this.setState({ canceling: true, componentIds: DEFAULT_COMPONENT_IDS });
      try {
        await cancelTask(target);
        this._emit('cancel_task', target);
        this.recordId = null;
      } catch (e) {
        //
      }
      this._emit('cancel_state_change', false);
      this.setState({ canceling: false, componentIds: DEFAULT_COMPONENT_IDS });
    }
  }
  subscription() {
    if (!this.interval) {
      this.interval = setInterval(() => this.initListen(), this.timeout);
    }
  }
  unSubscription() {
    this.interval && clearInterval(this.interval);
    this.interval = null;
    this.recordId = null;
    this.clearObservers();
  }
}
