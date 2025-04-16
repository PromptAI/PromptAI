import { Dispatch, SetStateAction, useEffect, useMemo, useState } from "react";

import zhCN from '@arco-design/web-react/es/locale/zh-CN';
import enUS from '@arco-design/web-react/es/locale/en-US';

const DEFAULT_APP_LANG = process.env.REACT_APP_LANG || 'en-US';

// url > backend > localStore > process.env > default;
export default function useLang() {
  const localeLang = useMemo(() => {
    const search = new URLSearchParams(window.location.search);
    return search.has('lang') ? search.get('lang') : (window.localStorage.getItem('lang') || DEFAULT_APP_LANG);
  }, []);
  const [lang, setLang] = useState(localeLang);

  useEffect(() => {
    if (lang) {
      window.localStorage.setItem('lang', lang + '')
    }
  }, [lang])

  const locale = useMemo(() => {
    switch (lang) {
      case 'zh-CN':
        return zhCN;
      case 'en-US':
        return enUS;
      default:
        return enUS;
    }
  }, [lang]);
  return [lang, setLang, locale] as [string, Dispatch<SetStateAction<string>>, any];
}