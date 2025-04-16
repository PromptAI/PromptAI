import config from './config';
import { DataItem, EmitHandler, RedoUndoState, StateListener } from './types';

export const initialRedoUndoState: RedoUndoState = {
  redoLoading: false,
  redoLength: 0,
  undoLoading: false,
  undoLength: 0,
};

class CapacityStack {
  items: DataItem[];
  capacity: number;
  constructor(capacity: number, initialValues: DataItem[]) {
    this.capacity = capacity;
    this.items = initialValues.slice(0, capacity);
  }
  push(item: DataItem) {
    this.items.unshift(item);
    this.items = this.items.slice(0, this.capacity);
  }
  pop() {
    if (this.items.length > 0) {
      return this.items.shift();
    }
    throw new Error('empty stack is do`nt pop');
  }
  isEmpty() {
    return this.items.length === 0;
  }
  length() {
    return this.items.length;
  }
}
class Eventer {
  listeners: StateListener[];
  constructor() {
    this.listeners = [];
  }
  addListener(listener: StateListener) {
    this.listeners = [...this.listeners, listener];
  }
  emit: EmitHandler = (state, eventKey) => {
    this.listeners.forEach(({ key, callback }) => {
      if (eventKey === key) {
        callback(state);
      }
    });
  };
}
export class RUInstance {
  manager?: RedoUndoManager;
  eventer: Eventer;
  constructor() {
    this.eventer = new Eventer();
  }
  setManager(manager: RedoUndoManager) {
    this.manager = manager;
  }
  push(item: DataItem) {
    if (this.manager) {
      this.manager.push(item);
    }
  }
  async redo() {
    if (this.manager) {
      return this.manager.redo();
    }
    return Promise.reject();
  }
  async undo() {
    if (this.manager) {
      return this.manager.undo();
    }
    return Promise.reject();
  }
  destoryManager() {
    this.manager = null;
  }
}
export class RedoUndoManager {
  key: string;
  capacity: number;
  state: RedoUndoState;
  _emit: EmitHandler;
  data: {
    redoStack: CapacityStack;
    undoStack: CapacityStack;
  };

  constructor(key: string, capacity: number, emit: EmitHandler) {
    this.capacity = capacity;
    this.state = initialRedoUndoState;
    this.key = key;
    this._emit = emit;
    this._initialStack();
  }
  _initialStack() {
    this._doLoading({ redoLoading: true, undoLoading: true });
    const initialValues = this._unPersistence();
    this.data = {
      redoStack: new CapacityStack(this.capacity, initialValues.redoItems),
      undoStack: new CapacityStack(this.capacity, initialValues.undoItems),
    };
    this._unLoading();
  }
  _doLoading(loading: Omit<RedoUndoState, 'redoLength' | 'undoLength'>) {
    this.state = { ...this.state, ...loading };
    this._emit(this.state, 'change_state');
  }
  _unLoading() {
    this.state = {
      redoLoading: false,
      redoLength: this.data.redoStack.length(),
      undoLoading: false,
      undoLength: this.data.undoStack.length(),
    };
    this._emit(this.state, 'change_state');
  }
  _unPersistence() {
    const obj_str = window.localStorage.getItem(this.key);
    let initialValues = { undoItems: [], redoItems: [] };
    try {
      initialValues = JSON.parse(obj_str) || { undoItems: [], redoItems: [] };
    } catch (e) {
      //
    }
    return initialValues;
  }
  _doPersistence() {
    const {
      redoStack: { items: redoItems },
      undoStack: { items: undoItems },
    } = this.data;
    window.localStorage.setItem(
      this.key,
      JSON.stringify({ redoItems, undoItems })
    );
  }
  _getHandler(type: string, kind: 'redo' | 'undo') {
    const handler = config[type][kind];
    if (!handler) {
      throw new Error(`no handler of ${type}:${kind}`);
    }
    return handler;
  }
  push(item: DataItem) {
    this._doLoading({ redoLoading: true, undoLoading: true });
    this.data.undoStack.push(item);
    this._doPersistence();
    this._unLoading();
  }
  async redo(): Promise<[any, RedoUndoState]> {
    if (this.data.redoStack.isEmpty()) {
      throw new Error('no data');
    }
    this._doLoading({ redoLoading: true, undoLoading: false });
    // pop redo stack
    const item = this.data.redoStack.pop();
    console.debug('redo item is', item.changed.id);
    const handler = this._getHandler(item.type, 'redo');
    // action redo
    let result;
    try {
      result = await handler(item);
    } catch (e) {
      // rollback
      this.data.redoStack.push(item);
      this._unLoading();
      return Promise.reject(e);
    }

    // push undo stack
    console.debug('push to undo stack: ', item.changed.id);
    this.data.undoStack.push(item);

    this._doPersistence();
    this._unLoading();
    if (result) {
      this._emit(this.state, result);
    }
    return [result, this.state];
  }
  async undo(): Promise<[any, RedoUndoState]> {
    if (this.data.undoStack.isEmpty()) {
      throw new Error('no data');
    }
    this._doLoading({ undoLoading: true, redoLoading: false });
    // pop undo stack
    const item = this.data.undoStack.pop();
    console.debug('undo item is', item.changed.id);
    const handler = this._getHandler(item.type, 'undo');
    // action undo
    let result;
    try {
      result = await handler(item);
    } catch (e) {
      // rollback
      console.debug('rollback item: push in undo', item);
      this.data.undoStack.push(item);
      this._unLoading();
      return Promise.reject(e);
    }
    // push redo stack
    console.debug('push to redo stack: ', item.changed.id);
    this.data.redoStack.push(item);

    this._doPersistence();
    this._unLoading();
    if (result) {
      this._emit(this.state, result);
    }
    return [result, this.state];
  }
}
