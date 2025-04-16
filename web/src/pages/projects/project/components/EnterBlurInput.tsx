import React, { useRef } from 'react';
import { Input } from '@arco-design/web-react';

function EnterBlurInput(props) {
  const inputRef = useRef<any>();
  const onKeyDown = (e) => {
    if (e.code === 'Enter') {
      inputRef.current.blur();
    }
  };

  return <Input ref={inputRef} onKeyDown={onKeyDown} {...props} />;
}

export default EnterBlurInput;
