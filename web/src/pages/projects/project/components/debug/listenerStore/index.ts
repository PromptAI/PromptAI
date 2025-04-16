import RunningPublishRecordObservable, {
  RunningPublishRecordObserver,
  RUNNING_TASK_DEFAULT_STATE,
} from './RunningPublishRecordObservable';

class RunningRecordStore {
  private store = new Map<string, RunningPublishRecordObservable>();

  getInstance(key: string, timeout?: number) {
    if (this.store.has(key)) {
      return this.store.get(key);
    }
    const instance = new RunningPublishRecordObservable(timeout);
    this.store.set(key, instance);
    return instance;
  }

  removeInstance(key: string) {
    if (this.store.has(key)) {
      this.store.get(key).unSubscription();
      this.store.delete(key);
    }
  }
}

const store = new RunningRecordStore();
if (window && process.env.NODE_ENV === 'development') {
  (window as any).runningTaskStore = store;
}
export default store;
export { RunningPublishRecordObserver, RUNNING_TASK_DEFAULT_STATE };
