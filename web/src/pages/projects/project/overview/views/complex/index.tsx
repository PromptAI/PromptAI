import { IconSync } from '@arco-design/web-react/icon';
import * as React from 'react';
import { NodeProps } from '../types';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';

export const ComplexIcon = IconSync;

// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface ComplexViewProps {}
export const ComplexView: React.FC<ComplexViewProps> = () => {
  const t = useLocale(i18n);
  return (
    <div className="p-2 text-[var(--color-text-1)] rounded flex items-center relative cursor-pointer">
      <ComplexIcon className="w-4 h-4" />
      <div className="ml-2 max-w-[14rem] truncate">{t['complex.name']}</div>
    </div>
  );
};
export const ComplexNode: React.FC<NodeProps> = () => {
  return <ComplexView />;
};
