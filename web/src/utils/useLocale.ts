import { useContext } from 'react';
import { GlobalContext } from '../context';
import defaultLocale from '../locale';

function useLocale(locale = null) {
  const { lang } = useContext(GlobalContext);

  return (locale || defaultLocale)[lang] || {};
}

export function useLocaleLang() {
  const { lang } = useContext(GlobalContext);
  return lang
}

export function useDefaultLocale() {
  const { lang } = useContext(GlobalContext);
  return defaultLocale[lang] || {};
}

export default useLocale;
