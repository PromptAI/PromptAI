import Plugin from '../Plugin';

export default class ViewHeightPlugin extends Plugin {
  constructor() {
    super('normal');
    window.addEventListener('resize', this.correctAppViewHeight.bind(this));
  }
  async start() {
    this.correctAppViewHeight();
  }
  done(): void {
    window.removeEventListener('resize', this.correctAppViewHeight.bind(this));
  }
  correctAppViewHeight() {
    const doc = document.documentElement;
    doc.style.setProperty('--app-view-height', `${window.innerHeight}px`);
  }
}
