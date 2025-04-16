import React, {
  ReactNode,
  isValidElement,
  cloneElement,
  ComponentPropsWithRef,
} from 'react';
import { useGraphStore } from '../../store/graph';
import cn from '@/utils/cn';
import { GraphNode } from '@/graph-next/type';

export interface ViewProps extends ComponentPropsWithRef<'div'> {
  icon?: ReactNode;
  id: string;
  label?: ReactNode;
  validatorError?: GraphNode['validatorError'] | null;
  topExtra?: ReactNode;
  bottomExtra?: ReactNode;
  color?: string;
  firstComponent?: boolean;
}
const View: React.ForwardRefRenderFunction<HTMLDivElement, ViewProps> = (
  {
    id,
    label,
    icon,
    validatorError,
    topExtra,
    bottomExtra,
    color,
    className,
    children,
    ...props
  },
  ref
) => {
  const selection = useGraphStore((s) => s.selection);
 const firstComponent = props.firstComponent;
  return (
    <div ref={ref} className={className} {...props}>
      <div
        className={cn(
          'p-2 text-[var(--color-text-1)] border-2 border-transparent hover:border-blue-600 rounded flex items-center relative cursor-pointer',
          { 'border-blue-600': selection?.id === id },
          { 'border-orange-500': validatorError }
        )}
        style={{ color }}
      >
        {/*第一个节点变大 */}
        {isValidElement(icon) && cloneElement(icon, { className: `w-${firstComponent ? 12 : 6} h-${firstComponent ? 12 : 6}` } as any)}
        {label && <div className={`ml-2 max-w-[14rem] truncate ${firstComponent ? 'text-2xl' : 'text-lg'}`}>{label}</div>}
        {topExtra && (
          <div className="absolute -top-4 left-1 max-w-[calc(100%_-_0.5rem)]">
            {topExtra}
          </div>
        )}
        {bottomExtra && (
          <div className="absolute top-8 left-1 max-w-[calc(100%_-_0.5rem)]">
            {bottomExtra}
          </div>
        )}
        {children}
      </div>
    </div>
  );
};

export default React.forwardRef<HTMLDivElement, ViewProps>(View);
