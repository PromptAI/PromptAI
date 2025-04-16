import { treeForEach } from '@/utils/tree';
import useLocale, { useLocaleLang } from '@/utils/useLocale';
import {
  Breadcrumb,
  Button,
  Divider,
  Dropdown,
  Menu,
  Spin,
  Tooltip,
} from '@arco-design/web-react';
import {
  IconDelete,
  IconMenuFold,
  IconMenuUnfold,
  IconMoreVertical,
} from '@arco-design/web-react/icon';
import { useCreation, useMemoizedFn } from 'ahooks';
import nProgress from 'nprogress';
import React, {
  CSSProperties,
  Fragment,
  ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useHistory } from 'react-router';
import DynamicMenuAdd from './dynamic-menu-add';
import DynamicMenuProvider, { useDynamicMenu } from './dynamic-menu-context';
import styles from './index.module.less';
import ToolContextProvider, { useTools } from './tools-context';
import { ComMenu, ComSiderProps, DynamicMenuProps } from './types';
import Branches from '@/assets/menu-icon/branches.svg';
import { Link } from 'react-router-dom';

export type ComBreadcrumb = {
  icon?: ReactNode;
  key: string;
  path?: string;
};

const BreadcrumbItem = ({
  icon,
  showIcon,
  name,
  children,
}: {
  icon?: React.ReactNode;
  name: string;
  showIcon?: boolean;
  children?: React.ReactNode;
}) => {
  return (
    <Tooltip content={name} mini>
      {children || (
        <div className={styles['breadcrumbs-icon']}>
          {showIcon && icon}
          <div className={styles['breadcrumbs-text']}>{name}</div>
        </div>
      )}
    </Tooltip>
  );
};

const ComBreadcrumbs = ({ breadcrumbs }: { breadcrumbs: ComBreadcrumb[] }) => {
  const t = useLocale();
  const dynamic = useDynamicMenu();
  const pathname = location.pathname;
  const dynamicMenuItem = dynamic?.dynamicMenus?.find(
    (menu) => menu.key === pathname
  );
  if (!breadcrumbs.length) return null;
  return (
    <Breadcrumb separator="/">
      {breadcrumbs.map(({ key, icon, path }, index) => (
        <Breadcrumb.Item key={key + index}>
          {path && key !== 'menu.projects.id.branch' ? (
            <BreadcrumbItem
              icon={icon}
              name={t[key] || key}
              showIcon={index === 0}
            >
              <Link to={path}>{t[key] || key}</Link>
            </BreadcrumbItem>
          ) : (
            <BreadcrumbItem
              icon={icon}
              name={t[key] || key}
              showIcon={index === 0}
            />
          )}
        </Breadcrumb.Item>
      ))}
      {dynamicMenuItem && (
        <Breadcrumb.Item>
          <BreadcrumbItem
            icon={<Branches className="icon-size" />}
            name={dynamicMenuItem?.name}
          />
        </Breadcrumb.Item>
      )}
    </Breadcrumb>
  );
};

const ComHeader = ({ breadcrumbs }: { breadcrumbs: ComBreadcrumb[] }) => {
  const { tools } = useTools();
  return (
    <div className={styles['com-layout-header']}>
      <ComBreadcrumbs breadcrumbs={breadcrumbs} />
      <div className="flex items-center gap-2 px-4">
        {tools.map(({ key, props, component }) =>
          component ? (
            React.cloneElement(component, { key })
          ) : (
            <Button {...props} key={key} />
          )
        )}
      </div>
    </div>
  );
};

const DynamicMenuText = ({ name }: { name: string }) => {
  return (
    <Tooltip content={name} mini>
      <div className={styles['dynamic-menu-item-text']}>{name}</div>
    </Tooltip>
  );
};
const DynamicMenu = ({
  path,
  name,
  icon,
  dynamic,
  onAfterAdd,
}: DynamicMenuProps) => {
  const t = useLocale();
  const { loading, dynamicMenus, refresh, onDelete } = useDynamicMenu();
  const hanldeAfterAdd = useMemoizedFn((menuKey) => {
    refresh();
    onAfterAdd(menuKey);
  });
  const onDeleteItem = (key, value, name) => {
    if (key === 'delete') {
      onDelete(value, name).then(() => refresh());
    }
  };

  return (
    <Spin loading={loading} className="w-full">
      <Menu.SubMenu
        key={path}
        title={
          <>
            {icon}
            {t[name] || name}
          </>
        }
        className={styles['dynamic-menu']}
      >
        {dynamicMenus?.map(({ key, name: dName, value }) => (
          <Menu.Item key={key} className={styles['dynamic-menu-item']}>
            <Branches className="icon-size" />
            <DynamicMenuText name={dName} />
            <Dropdown
              trigger="hover"
              droplist={
                <Menu
                  onClickMenuItem={(item) => onDeleteItem(item, value, dName)}
                  onClick={(evt) => evt.stopPropagation()}
                >
                  <Menu.Item key="delete">
                    <IconDelete style={{ color: 'red', marginRight: 4 }} />
                    {t['command.delete']}
                  </Menu.Item>
                </Menu>
              }
            >
              <IconMoreVertical
                onClick={(evt) => evt.stopPropagation()}
                className={styles['dynamic-menu-item-more']}
              />
            </Dropdown>
          </Menu.Item>
        ))}
        <Menu.Item key="add-button" className={styles['dynamic-menu-item-pl']}>
          <DynamicMenuAdd
            dynamic={dynamic}
            basePath={path}
            onAfterAdd={hanldeAfterAdd}
          />
        </Menu.Item>
      </Menu.SubMenu>
    </Spin>
  );
};

const ComSider = ({
  menus,
  collapsed,
  setCollapsed,
  defaultSelectedKeys,
  collapseDisabled,
}: ComSiderProps) => {
  const history = useHistory();
  const t = useLocale();
  useEffect(() => {
    setSelectedKeys([history?.location?.pathname]);
  }, [history?.location?.pathname]);
  const [selectedKeys, setSelectedKeys] = useState<string[]>(
    defaultSelectedKeys || [menus[0].path]
  );
  const onClickMenuItem = useMemoizedFn((key: string) => {
    if (key === 'add-button') {
      return;
    }
    nProgress.start();
    setSelectedKeys([key]);
    history.push(key);
    nProgress.done();
  });

  const defaultOpenKeys = useMemo(() => {
    return menus.map((c) => {
      if (c?.children && c.defaultExpend) {
        return c.path;
      }
    });
  }, [menus]);

  const onDynamicAfterAdd = useMemoizedFn((menuKey) => {
    setSelectedKeys([menuKey]);
  });

  const renderMenuItems = useCallback(
    (children: ComMenu[]) => {
      return (
        <Menu.ItemGroup>
          {children.map(({ icon, name, path, dynamic }) => {
            if (dynamic) {
              return (
                <DynamicMenu
                  key={path}
                  path={path}
                  icon={icon}
                  name={name}
                  dynamic={dynamic}
                  onAfterAdd={onDynamicAfterAdd}
                />
              );
            }
            return (
              <Menu.Item key={path}>
                <div className="flex items-center">
                  {icon}
                  {t[name] || name}
                </div>
              </Menu.Item>
            );
          })}
        </Menu.ItemGroup>
      );
    },
    [onDynamicAfterAdd, t]
  );
  const lang = useLocaleLang();
  return (
    <aside className={styles['com-layout-sider']}>
      <div className={styles['com-layout-menu-content']}>
        <Menu
          collapse={collapsed}
          onClickMenuItem={onClickMenuItem}
          selectedKeys={selectedKeys}
          defaultOpenKeys={defaultOpenKeys}
          style={{ paddingBottom: 120 }}
        >
          {menus.map(
            ({
              icon,
              name,
              path,
              spliter,
              selectable = false,
              dynamic,
              children,
            }) => {
              if (children) {
                if (selectable) {
                  return (
                    <div key={path}>
                      <Menu.Item key={path}>
                        {icon}
                        {t[name] || name}
                      </Menu.Item>
                      {renderMenuItems(children)}
                    </div>
                  );
                }
                return (
                  <Menu.SubMenu
                    key={path}
                    title={
                      <>
                        {icon}
                        {t[name] || name}
                      </>
                    }
                  >
                    {children.map(({ icon, name, path }) => {
                      return (
                        <Menu.Item key={path}>
                          <div className="flex items-center">
                            {icon}
                            {t[name] || name}
                          </div>
                        </Menu.Item>
                      );
                    })}
                  </Menu.SubMenu>
                );
              }
              if (dynamic) {
                return (
                  <DynamicMenu
                    key={path}
                    path={path}
                    icon={icon}
                    name={name}
                    dynamic={dynamic}
                    onAfterAdd={onDynamicAfterAdd}
                  />
                );
              }
              return (
                <Fragment key={path}>
                  <Menu.Item key={path}>
                    {icon}
                    {t[name] || name}
                  </Menu.Item>
                  {spliter && <Divider className="!my-2" />}
                </Fragment>
              );
            }
          )}
          <div className={styles['com-layout-sider-footer']}>
            {t['contact']}
            {lang === 'zh-CN' && (
              <a href="mailto:info@promptai.cn">info@promptai.cn</a>
            )}
            {lang === 'en-US' && (
              <a href="mailto:info@promptai.us">info@promptai.us</a>
            )}
          </div>
        </Menu>
        {!collapseDisabled && (
          <div className={styles['com-layout-collapse-btn']}>
            <Button
              size="small"
              type="secondary"
              shape="circle"
              onClick={() => setCollapsed((c) => !c)}
            >
              {collapsed ? <IconMenuUnfold /> : <IconMenuFold />}
            </Button>
          </div>
        )}
      </div>
    </aside>
  );
};

const ComLayout = ({
  headers,
  menus,
  breadcrumbs,
  defaultSelectedKeys,
  children,
}: {
  headers?: ComBreadcrumb[];
  breadcrumbs?: ComBreadcrumb[];
  menus?: ComMenu[];
  contentStyle?: CSSProperties;
  defaultSelectedKeys?: string[];
  children: ReactNode;
}) => {
  const [collapsed, setCollapsed] = useState(false);
  const haveHeaders = useCreation(() => !!(headers && headers.length), []);
  const haveMenus = useCreation(() => !!(menus && menus.length), []);
  const dynamicMenu = useCreation(() => {
    let target = undefined;
    treeForEach(
      menus || [],
      (node) => {
        if (node.dynamic) target = node;
      },
      'DFS'
    );
    return target;
  }, [menus]);
  return (
    <DynamicMenuProvider menu={dynamicMenu}>
      <ToolContextProvider>
        <div className={styles['com-layout']}>
          {haveHeaders && <ComHeader breadcrumbs={headers} />}
          <div
            className={styles['com-layout-main']}
            style={{
              height: haveHeaders ? 'calc(100vh - 99px)' : 'calc(100vh - 52px)',
            }}
          >
            {haveMenus && (
              <ComSider
                defaultSelectedKeys={defaultSelectedKeys}
                menus={menus}
                collapsed={collapsed}
                setCollapsed={(value) => {
                  setCollapsed(value);
                }}
                haveHeader={haveHeaders}
                collapseDisabled
              />
            )}
            <div className={styles['com-layout-frame']}>
              {breadcrumbs && <ComBreadcrumbs breadcrumbs={breadcrumbs} />}
              {children}
            </div>
          </div>
        </div>
      </ToolContextProvider>
    </DynamicMenuProvider>
  );
};

export default ComLayout;
