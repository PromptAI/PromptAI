import { History } from 'history';
import Plugin from '../Plugin';

export default class LicensePlugin extends Plugin {
  history: History<unknown>;
  constructor(history: History) {
    super('license');
    this.history = history;
  }
  async start(res) {
    const { status } = res;
    if (status) return;
    this.history.replace('/check');
  }
  done(): void {
    //
  }
}
