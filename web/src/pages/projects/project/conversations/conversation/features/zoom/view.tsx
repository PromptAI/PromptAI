import React, { useCallback, useEffect, useRef, useState } from 'react';
import i18n from './i18n';
import useLocale from '@/utils/useLocale';
import { Button } from '@arco-design/web-react';
import store from '@/core-next/store';
import { IconZoomIn, IconZoomOut } from '@arco-design/web-react/icon';

const useZoom = () => {
  const instanceRef = useRef(store.getInstance('flow-graph'));
  const [zoom, setZoom] = useState(1);
  useEffect(() => {
    instanceRef.current.on('zoom', setZoom);
    return () => {
      store.removeInstance('flow-graph');
    };
  }, []);
  const zoomIn = useCallback(() => instanceRef.current.zoomIn(), []);
  const zoomOut = useCallback(() => instanceRef.current.zoomOut(), []);
  return { zoom, zoomIn, zoomOut };
};

export const Zoom: React.FC = () => {
  const t = useLocale(i18n);
  const { zoom, zoomIn, zoomOut } = useZoom();
  return (
    <div className="flex items-center gap-1">
      <Button icon={<IconZoomIn />} type="text" size="small" onClick={zoomIn}>
        {t['zoomIn']}
      </Button>
      <span>{`${(zoom * 100).toFixed(0)}%`}</span>
      <Button icon={<IconZoomOut />} type="text" size="small" onClick={zoomOut}>
        {t['zoomOut']}
      </Button>
    </div>
  );
};
