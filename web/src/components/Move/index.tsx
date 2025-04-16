import { useDeepCompareEffect } from 'ahooks';
import React, { CSSProperties, useState } from 'react';
import { useEffect } from 'react';
import { default as DraggableCore } from 'react-draggable';
import './index.less';

const Draggable: any = DraggableCore;

interface Info {
  y: number;
  x: number;
}
const defaultInfo: Info = {
  y: 0,
  x: 0,
};
class Position {
  static get(key: string) {
    try {
      return (JSON.parse(window.localStorage.getItem(key)) ||
        defaultInfo) as Info;
    } catch (e) {
      return defaultInfo;
    }
  }
  static set(key: string, info: Info) {
    window.localStorage.setItem(key, JSON.stringify(info));
  }
  static remove(key: string) {
    window.localStorage.removeItem(key);
  }
}

interface MoveProps {
  visible?: boolean;
  keyword?: string;
  children: React.ReactNode;
  defaultPostion?: Info;
  className?: string;
  style?: CSSProperties;
  bodyStyle?: CSSProperties;
  onPositionChange?: (x, y) => void;
  bounds?: string;
  handle?: string;
  disabled?: boolean;
}
const Move = ({
  keyword,
  visible = true,
  children,
  defaultPostion,
  className,
  style,
  bodyStyle,
  onPositionChange,
  bounds,
  handle,
  disabled,
}: MoveProps) => {
  const [position, setPosition] = useState(Position.get(keyword));
  useEffect(() => {
    defaultPostion && setPosition(defaultPostion);
  }, [defaultPostion]);
  useDeepCompareEffect(() => {
    onPositionChange?.(position.x || 0, position.y || 0);
  }, [position]);
  const onStop = (_, { x, y }) => {
    keyword && Position.set(keyword, { x, y });
    setPosition({ x, y });
  };

  return (
    <div
      className={className ? `move-container ${className}` : 'move-container'}
      style={{ display: visible ? null : 'none', ...style }}
    >
      <Draggable
        handle={handle || '.move-pointer'}
        defaultPosition={position}
        position={position}
        onStop={onStop}
        bounds={bounds}
        disabled={disabled}
        enableUserSelectHack
      >
        <div style={bodyStyle}>{children}</div>
      </Draggable>
    </div>
  );
};

export default Move;
