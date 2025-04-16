import { ButtonProps } from '@arco-design/web-react';
import React, { createContext, useContext, useEffect, useState } from 'react';

export interface Tool {
  key: string;
  props?: ButtonProps;
  component?: React.ReactElement;
}
interface ToolsContextValue {
  tools: Tool[];
  setTools: React.Dispatch<React.SetStateAction<Tool[]>>;
}
const defaultValue: ToolsContextValue = {
  tools: [],
  setTools: () => void 0,
};
const ToolsContext = createContext<ToolsContextValue>(defaultValue);

export default function ToolContextProvider({ children }) {
  const [tools, setTools] = useState<Tool[]>([]);
  return (
    <ToolsContext.Provider value={{ tools, setTools }}>
      {children}
    </ToolsContext.Provider>
  );
}

export function useTools(tools?: Tool[]) {
  const context = useContext(ToolsContext);
  if (!context) {
    throw new Error('useProjectTool should be in a context');
  }
  useEffect(() => {
    if (Array.isArray(tools)) context.setTools(tools.filter(Boolean));
    return () => {
      context?.setTools([]);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tools]);
  return context;
}
