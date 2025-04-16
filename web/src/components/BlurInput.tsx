import { Input, InputProps } from '@arco-design/web-react';
import { RefInputType } from '@arco-design/web-react/es/Input/interface';
import React, {
  KeyboardEvent,
  MutableRefObject,
  useCallback,
  useMemo,
  useRef,
} from 'react';

const BlurInput = (props: InputProps, ref?: MutableRefObject<RefInputType>) => {
  const innerRef = useRef<RefInputType>();
  const refFunc = useMemo(
    () => (target) => {
      innerRef.current = target;
      if (ref) {
        ref.current = target;
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );
  const onPressEnter = useCallback(
    (evt: KeyboardEvent) => {
      props?.onPressEnter?.(evt);
      if (!evt.isDefaultPrevented()) innerRef.current.blur();
    },
    [props]
  );
  return <Input ref={refFunc} onPressEnter={onPressEnter} {...props} />;
};

export default React.forwardRef(BlurInput);
