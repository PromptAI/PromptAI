import { Dispatch, ReactNode, SetStateAction } from 'react';

export type Dynamic = {
  api: string;
  nameIndex: string;
  params?: any;
  menuPrefix?: string;
  menuSuffix?: string;
  transformAdd: (values: any, urlParams: any) => object;
  onDeletedRedirectUrl: (dynamic: any, urlParams: any) => string | void;
};
export interface ComMenu {
  icon?: ReactNode;
  name: string;
  path: string;
  spliter?: boolean;
  selectable?: boolean;
  children?: ComMenu[];
  dynamic?: Dynamic;
  defaultExpend?: boolean;
}
export interface DynamicMenuProps {
  path: string;
  icon: ReactNode;
  name: string;
  dynamic: Dynamic;
  onAfterAdd: (menuKey: string) => void;
}
export interface ComSiderProps {
  menus: ComMenu[];
  collapsed: boolean;
  setCollapsed: Dispatch<SetStateAction<boolean>>;
  haveHeader: boolean;
  defaultSelectedKeys: string[];
  collapseDisabled?: boolean;
}
