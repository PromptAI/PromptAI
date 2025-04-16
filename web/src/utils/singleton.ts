export const key_default_account = '04fabd51df78';

export default class Singleton {
  static defaultAccount() {
    return window.localStorage.getItem(key_default_account) || false;
  }
  static set(key: string, value) {
    window.localStorage.setItem(key, value);
  }

  static remove(key: string) {
    window.localStorage.removeItem(key);
  }
}
