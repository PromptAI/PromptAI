const key = 'runner_info';

type Info = {
  y: number;
  x: number;
};
const defaultInfo: Info = {
  y: 0,
  x: 0,
};
export default class RunnerInfo {
  static get() {
    try {
      return (JSON.parse(window.localStorage.getItem(key)) ||
        defaultInfo) as Info;
    } catch (e) {
      return defaultInfo;
    }
  }
  static set(info: Info) {
    window.localStorage.setItem(key, JSON.stringify(info));
  }
  static remove() {
    window.localStorage.removeItem(key);
  }
}
