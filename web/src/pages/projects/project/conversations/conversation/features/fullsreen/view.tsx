import React, { RefObject } from 'react';
import i18n from './i18n';
import useLocale from '@/utils/useLocale';
import { Button } from '@arco-design/web-react';
import { useFullscreen } from 'ahooks';
import {
  IconFullscreen,
  IconFullscreenExit,
} from '@arco-design/web-react/icon';

export interface FullScreenProps {
  containerRef: RefObject<HTMLDivElement>;
}
export const FullScreen: React.FC<FullScreenProps> = ({ containerRef }) => {
  const t = useLocale(i18n);
  const [isFullscreen, { toggleFullscreen }] = useFullscreen(containerRef);
  return (
    <Button
      icon={isFullscreen ? <IconFullscreenExit /> : <IconFullscreen />}
      size="small"
      onClick={toggleFullscreen}
      type="text"
    >
      {isFullscreen ? t['exitFullscreen'] : t['fullscreen']}
    </Button>
  );
};
