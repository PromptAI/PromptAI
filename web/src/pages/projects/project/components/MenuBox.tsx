import { GraphNode } from '@/graph-next/type';
import { Dropdown, DropdownProps } from '@arco-design/web-react';
import { IconExclamationCircle } from '@arco-design/web-react/icon';
import React, {
  Children,
  PropsWithChildren,
  ReactNode,
  useEffect,
  useState,
} from 'react';
import { styled } from 'styled-components';

export const MenuBoxDivider = () => {
  return <div className="border-b"></div>;
};

const MenuContainer = styled.div`
  & button {
    width: 100%;
    text-align: start;
  }
`;
interface MenuBoxProps {
  trigger: ReactNode;
  validatorError?: GraphNode['validatorError'] | null;
  position?: DropdownProps['position'];
  action?: DropdownProps['trigger'];
  triggerProps?: DropdownProps['triggerProps'];
}
const defaultTrigger: DropdownProps['trigger'] = ['click', 'contextMenu'];
const MenuBox: React.FC<PropsWithChildren<MenuBoxProps>> = ({
  validatorError,
  trigger,
  position = 'bl',
  action = defaultTrigger,
  triggerProps,
  children,
}) => {
  const [popupVisible, setPopupVisible] = useState(false);
  useEffect(() => {
    if (popupVisible) {
      const onWheel = () => setPopupVisible(false);
      document.addEventListener('wheel', onWheel);
      return () => document.removeEventListener('wheel', onWheel);
    }
  }, [popupVisible]);
  const droplist = !!Children.toArray(children).filter(Boolean).length && (
    <MenuContainer
      className="p-2 rounded border border-dashed space-y-2 shadow text-[var(--color-text-1)] max-w-sm overflow-x-hidden bg-[var(--color-bg-3)] flex flex-col"
      onClick={() => setPopupVisible(false)}
    >
      {validatorError && (
        <div className="flex items-center text-orange-500 space-x-2 text-sm">
          <IconExclamationCircle />
          <span>{validatorError.errorMessage}</span>
        </div>
      )}
      {children}
    </MenuContainer>
  );
  return (
    <Dropdown
      trigger={action}
      position={position}
      droplist={droplist}
      popupVisible={popupVisible}
      onVisibleChange={setPopupVisible}
      unmountOnExit={false}
      triggerProps={triggerProps}
    >
      {trigger}
    </Dropdown>
  );
};

export default MenuBox;
