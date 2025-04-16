import { useCountDown } from 'ahooks';
import { useCallback, useRef, useState } from 'react';

export default function useTimeOn(onTime?: () => void, initialTime?: number) {
  const isFirstEnd = useRef(!initialTime);
  const [endTime, setEndTime] = useState(initialTime);
  const [count] = useCountDown({
    targetDate: endTime,
    onEnd: onTime,
  });
  const setTimeOn = useCallback((milliseconds: number) => {
    isFirstEnd.current = false;
    setEndTime(Date.now() + milliseconds);
  }, []);
  return [!count && !isFirstEnd.current, setTimeOn] as unknown as [
    boolean,
    (milliseconds: number) => void
  ];
}
