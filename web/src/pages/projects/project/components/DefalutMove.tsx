import Move from '@/components/Move';
import React, { useEffect, useState } from 'react';

export default function DefalutMove(props) {
  const { bounds, childWidth, childHeight, keyword } = props;
  const [position, setPosition] = useState({ y: 0, x: 0 });

  useEffect(() => {
    if (bounds) {
      const width = document.documentElement.clientWidth - childWidth - 40;
      const height = document.documentElement.clientHeight - childHeight - 30;
      setPosition({ y: height, x: width });
    }
  }, [bounds, childHeight, childWidth, keyword]);

  return (
    <Move defaultPostion={position} {...props}>
      {props.children}
    </Move>
  );
}
