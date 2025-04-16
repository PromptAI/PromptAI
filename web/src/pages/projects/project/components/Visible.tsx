import React, {
  cloneElement,
  isValidElement,
  useCallback,
  useState,
} from 'react';
const Visible: React.FC = ({ children }) => {
  const [key, setKey] = useState(0);
  const [visible, setVisible] = useState(false);
  const onVisibleChange = useCallback((v) => {
    setVisible(v);
    if (v) {
      setKey((k) => k + 1);
    }
  }, []);
  return isValidElement(children)
    ? cloneElement(children, { key, visible, onVisibleChange } as any)
    : null;
};

export default Visible;
