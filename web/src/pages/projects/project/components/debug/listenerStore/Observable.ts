import { ObjectArrayHelper } from "@/graph-next/helper";

interface Listener<E extends string> {
  name: E;
  callback: <T = any>(params?: T) => void;
}
export class Observer<E extends string> {
  private listeners: Listener<E>[] = [];
  on(name: E, callback: <T = any>(params?: T) => void) {
    this.listeners.push({ name, callback });
  }
  emit<T = any>(name: E, params?: T) {
    this.listeners.forEach(l => l.name === name && l.callback(params));
  }
}
export default class Observable<E extends string> {
  private observers: Observer<E>[] = [];
  addObserver(observer: Observer<E>) {
    this.observers.push(observer)
  }
  removeObserver(observer: Observer<E>) {
    this.observers = ObjectArrayHelper.del(this.observers, o => o === observer);
  }
  clearObservers() {
    this.observers = [];
  }
  protected _emit<T = any>(name: E, params?: T) {
    this.observers.forEach(o => o.emit(name, params))
  }
}
