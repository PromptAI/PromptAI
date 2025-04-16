const key = 'AD9Swf3epfzD-bvgI2PLc';
const expireAt = 'AD9Swf3epfzD-touibjch';
export default class Token {
  static get() {
    return window.localStorage.getItem(key) || '';
  }
  static getToken() {
    return {
      token: this.get(),
      tokenExpireAt: Number(window.localStorage.getItem(expireAt) || 0),
    };
  }
  static set(token: string, tokenExpireAt: string) {
    window.localStorage.setItem(key, token + '');
    window.localStorage.setItem(expireAt, tokenExpireAt + '');
  }
  static setToken(token: string) {
    window.localStorage.setItem(key, token + '');
  }
  static remove() {
    window.localStorage.removeItem(key);
    window.localStorage.removeItem(expireAt);
  }
}
