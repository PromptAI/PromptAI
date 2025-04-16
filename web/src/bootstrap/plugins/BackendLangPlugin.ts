import Plugin from '../Plugin';

type LangChangeFunc = (lang: string) => void;
export default class BackendLangPlugin extends Plugin {
  onLangChange: LangChangeFunc;
  constructor({ onLangChange }: { onLangChange: LangChangeFunc }) {
    super('logged');
    this.onLangChange = onLangChange;
  }
  async start(user) {
    const language = user.config?.language;
    if (language === 'zh') this.onLangChange('zh-CN');
    if (language === 'en') this.onLangChange('en-US');
  }
  done(): void {
    //
  }
}
