import RunningTaskObservable, {
  RunningTaskObserver,
  RUNNING_TASK_DEFAULT_STATE,
} from './RunningTaskObservable';

class RunningTaskStore {
  private store = new Map<string, RunningTaskObservable>();

  getInstance(key: string, timeout?: number) {
    if (this.store.has(key)) {
      return this.store.get(key);
    }
    const instance = new RunningTaskObservable(timeout);
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

const store = new RunningTaskStore();
if (window && process.env.NODE_ENV === 'development') {
  (window as any).runningTaskStore = store;
}
export default store;
export { RunningTaskObserver, RUNNING_TASK_DEFAULT_STATE };
