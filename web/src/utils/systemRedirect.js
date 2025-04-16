export default class SystemRedirect {
  // 带着当前页面的参数
  static redirect2Login() {
    const encodedUrl = encodeURIComponent(window.location.href);
    window.location.href = `/login?redirect=` + encodedUrl;
  }
}
