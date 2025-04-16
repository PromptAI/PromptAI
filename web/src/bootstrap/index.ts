import { authMe } from '@/api/auth';
import checkLogin from '@/utils/checkLogin';
import Plugin, { PluginType } from './Plugin';
import { isLicenseOk } from '@/api/licenses';


export default class Bootstrap {
  private plugins: Plugin[] = [];

  apply(plugin: Plugin) {
    this.plugins.push(plugin);
  }
  async mouted() {
    if (checkLogin()) {
      try {

        const res = await authMe();
        await this.startPlugins('logged', res);
      } catch (e) {
        console.log(e);
        this.startPlugins('error', e);
      }
    } else {
      this.startPlugins('unlogin');
    }
    this.startPlugins('normal');
  }
  private async startPlugins(type: PluginType, ...args: any) {
    const plugins = this.plugins.filter((p) => p.type === type);
    for (let index = 0; index < plugins.length; index++) {
      const plugin = plugins[index];
      await plugin.start(...args);
    }
  }
  unmouted() {
    this.plugins.forEach((p) => p.done());
  }
}
export { Plugin };
