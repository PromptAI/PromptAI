import { createContext, useContext } from 'react';

export const GlobalContext = createContext<{
  lang?: string;
  setLang?: (value: string) => void;
  collapsed?: boolean;
  theme?: string;
  setTheme?: (value: string) => void;
  setCollapsed?: (value: boolean) => void;
}>({});

export function useGlobalContext() {
  return useContext(GlobalContext);
}
