import Plugin from '../Plugin';
import SystemRedirect from '@/utils/systemRedirect';

const AUTH_PAGES = ['/login', '/register', '/applying'];
const DEFAULT_REDIRECT = '/login';
export default class RedirectLoginPlugin extends Plugin {
  redirect: string;
  constructor(redirect = DEFAULT_REDIRECT) {
    super('unlogin');
    this.redirect = redirect;
  }
  async start() {
    if (!AUTH_PAGES.includes(window.location.pathname)) {
      // 未登录是直接访问页面，
      // 带着当前页面日志跳转到登录页面，登录后跳到登录前的页面
      SystemRedirect.redirect2Login();
    }
  }
  done(): void {
    //
  }
}
