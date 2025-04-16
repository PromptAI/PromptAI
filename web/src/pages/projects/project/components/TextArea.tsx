import { Input, TextAreaProps } from '@arco-design/web-react';
import { RefTextAreaType } from '@arco-design/web-react/es/Input';
import React from 'react';

export type { TextAreaProps };
const TextArea = React.forwardRef<RefTextAreaType, TextAreaProps>(
  ({ style, ...props }, ref) => {
    return (
      <Input.TextArea
        autoSize
        style={{ minHeight: 48, ...style }}
        {...props}
        ref={ref}
      />
    );
  }
);

export default TextArea;
