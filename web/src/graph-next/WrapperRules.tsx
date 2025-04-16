import { GraphTreeValue, NodeDefinedProps } from '@/core-next/types';
import { Tag } from '@arco-design/web-react';
import React, { Fragment, useEffect, useMemo, useState } from 'react';

interface Rule {
  validator: (node: GraphTreeValue) => React.ReactNode;
}
interface WrapperProps {
  color?: string;
  node?: NodeDefinedProps;
  children: React.ReactNode;
}
const Wrapper = ({ color = null, children, node }: WrapperProps) => {
  const error = useMemo(() => {
    if (node) {
      const { defaultProps } = node;
      if (defaultProps?.rules) {
        const rules = defaultProps.rules as Rule[];
        for (let i = 0; i < rules.length; i++) {
          const rule: Rule = rules[i];
          const result = rule.validator(node);

          if (result) {
            return result;
          }
        }
      }
    }
    return undefined;
  }, [node]);
  return (
    <div
      className="app-node-wrapper flex flex-col items-start"
      style={{ backgroundColor: color }}
    >
      {children}
      <Tag size="small" color="red">
        {error || 'success'}
      </Tag>
    </div>
  );
};

export default Wrapper;
