import store from '@/store';
import Plugin from '../Plugin';

export default class InitialStorePlugin extends Plugin {
  constructor() {
    super('logged');
  }
  async start(userInfo) {
    store.dispatch({
      type: 'update-userInfo',
      payload: { userInfo, userLoading: false },
    });
  }
  done(): void {
    //
  }
}
