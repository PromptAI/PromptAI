import { useEffect, useState } from 'react';

const MEMORIED_KEY = 'memory_state_';
const loadLocalStorge = <T>(key: string, defaultValue: T): T => {
  const value = window.localStorage.getItem(MEMORIED_KEY + key);
  try {
    const val = JSON.parse(value);
    return val || defaultValue;
  } catch (error) {
    return defaultValue;
  }
};
const toMemoried = (key: string, value: any) => {
  try {
    window.localStorage.setItem(MEMORIED_KEY + key, JSON.stringify(value));
  } catch (error) {
    // stringify error
  }
};
const useLocalMemoried = <T>(key: string, defaultValue: T) => {
  const [value, set] = useState(() => loadLocalStorge(key, defaultValue));
  useEffect(() => {
    toMemoried(key, value);
  }, [value, key]);
  return [value, set] as const;
};

export default useLocalMemoried;
