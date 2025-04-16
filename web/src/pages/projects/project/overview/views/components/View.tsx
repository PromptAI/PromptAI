import cn from '@/utils/cn';
import * as React from 'react';
import { useGraphStore } from '../../store';

export interface ViewProps extends React.HTMLAttributes<HTMLDivElement> {
  icon: React.ReactNode;
  label: React.ReactNode;
  id: string;
}

const View = React.forwardRef<HTMLDivElement, ViewProps>(
  ({ className, id, label, icon, ...props }, ref) => {
    const selection = useGraphStore((s) => s.selection);
    return (
      <div ref={ref} className={className} {...props}>
        <div
          className={cn(
            'p-2 text-[var(--color-text-1)] border-2 border-transparent hover:border-blue-600 rounded flex items-center relative cursor-pointer',
            { 'border-blue-600': selection?.id === id }
          )}
        >
          {React.isValidElement(icon) &&
            React.cloneElement(icon, { className: 'w-4 h-4' } as any)}
          <div className="ml-2 max-w-[14rem] truncate">{label}</div>
        </div>
      </div>
    );
  }
);

export default View;
