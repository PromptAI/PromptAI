export type PluginType = 'license' | 'logged' | 'unlogin' | 'normal' | 'error';
export default abstract class Plugin {
  type: PluginType;
  constructor(type: PluginType) {
    this.type = type;
  }
  abstract start(...args: any): Promise<void>;
  abstract done(): void;
}
