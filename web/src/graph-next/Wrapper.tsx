import {
  Divider,
  Menu,
  Space,
  Tooltip,
  Trigger,
  Typography,
} from '@arco-design/web-react';
import { IconCloseCircle } from '@arco-design/web-react/icon';
import React, {
  Fragment,
  ReactNode,
  useCallback,
  useMemo,
  useState,
} from 'react';
import cls from 'classnames';
import './Wrapper.css';

export interface PopupMenu {
  key: string;
  title: React.ReactNode;
  icon?: React.ReactNode;
  divider?: boolean;
  onClick?: () => void;
  hidden?: boolean;
  children?: PopupMenu[];
}
interface WrapperProps {
  menus?: PopupMenu[];
  selected?: boolean;
  children: ReactNode;
  validatorError?: { errorCode: number; errorMessage: string; color?: string };
}
const menuStyle = {
  maxWidth: 228,
  border: '1px dashed var(--color-text-4)',
  borderRadius: 4,
  background: 'var(--color-bg-2)',
  boxShadow:
    '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
};
const menuItemStyle = {
  height: 32,
  lineHeight: '32px',
  display: 'flex',
  alignItems: 'center',
};
const arrowProps = { style: { backgroundColor: 'var(--color-text-4)' } };
const MenuChildren = ({ children, menus, error }) => {
  const [visible, setVisible] = useState(false);
  const renderPopup = useCallback(() => {
    const buildClick = (onClick) => () => {
      onClick();
      setVisible(false);
    };
    return (
      <div style={menuStyle} onClick={(evt) => evt.stopPropagation()}>
        {error.error && (
          <>
            <Space
              className="w-full"
              size={16}
              style={{ padding: '8px 20px 4px' }}
            >
              <Typography.Text type="error">
                <IconCloseCircle />
              </Typography.Text>
              <Typography.Text type="error" style={{ margin: 0, fontSize: 12 }}>
                {error.message}
              </Typography.Text>
            </Space>
            <Divider style={{ margin: 4 }} />
          </>
        )}
        <Menu mode="pop">
          <div>
            {menus
              ?.filter((m) => !m.hidden)
              .map(({ key, title, onClick, divider, icon, children }) => (
                <Fragment key={key}>
                  {divider && <Divider key={key + '-'} style={{ margin: 4 }} />}
                  {children ? (
                    <Menu.SubMenu
                      key={`${key}_sub`}
                      style={menuItemStyle}
                      title={
                        <>
                          {icon}
                          {title}
                        </>
                      }
                    >
                      {children.map(
                        ({
                          key: subKey,
                          onClick: subOnClick,
                          icon: subIcon,
                          title: subTitle,
                        }) => (
                          <Menu.Item
                            key={subKey}
                            onClick={buildClick(subOnClick)}
                            style={menuItemStyle}
                          >
                            {subIcon}
                            <Typography.Text
                              ellipsis={{ showTooltip: true }}
                              style={{ margin: 0, flex: 1 }}
                            >
                              {subTitle}
                            </Typography.Text>
                          </Menu.Item>
                        )
                      )}
                    </Menu.SubMenu>
                  ) : (
                    <Menu.Item
                      key={key}
                      onClick={buildClick(onClick)}
                      style={menuItemStyle}
                    >
                      {icon}
                      <Typography.Text
                        ellipsis={{ showTooltip: true }}
                        style={{ margin: 0, flex: 1 }}
                      >
                        {title}
                      </Typography.Text>
                    </Menu.Item>
                  )}
                </Fragment>
              ))}
          </div>
        </Menu>
      </div>
    );
  }, [menus, error]);

  return menus && menus.length > 0 ? (
    <Trigger
      trigger={['click', 'contextMenu']}
      popupVisible={visible}
      onVisibleChange={setVisible}
      popup={renderPopup}
      mouseEnterDelay={50}
      mouseLeaveDelay={100}
      position="bl"
      updateOnScroll
      showArrow
      arrowProps={arrowProps}
    >
      <div>{children}</div>
    </Trigger>
  ) : (
    <Tooltip content={error.message}>
      <div>{children}</div>
    </Tooltip>
  );
};
const Wrapper = ({
  menus,
  selected,
  validatorError,
  children,
}: WrapperProps) => {
  const error = useMemo(
    () => ({
      error:
        !!validatorError &&
        validatorError.errorCode !== undefined &&
        validatorError.errorCode !== 0,
      message: validatorError?.errorMessage,
      color: validatorError?.color,
    }),
    [validatorError]
  );
  return (
    <div
      className={cls(
        'node',
        { 'node-error': error.error && !selected },
        { 'node-selected': selected }
      )}
    >
      <MenuChildren menus={menus} error={error}>
        {children}
      </MenuChildren>
    </div>
  );
};

export default Wrapper;
