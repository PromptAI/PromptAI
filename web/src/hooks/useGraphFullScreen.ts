import { useFullscreen } from 'ahooks';
import { useRef } from 'react';

export default () => {
  const ref = useRef();
  const [, { enterFullscreen, exitFullscreen }] = useFullscreen(ref);

  return { ref, enter: enterFullscreen, exit: exitFullscreen } as const;
};
