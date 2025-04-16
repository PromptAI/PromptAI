import { MenuRoute } from '@/authentication-routes';
import ComLayout, { ComBreadcrumb } from '@/components/Layout';
import lazyload from '@/utils/lazyload';
import {
  IconApps,
  IconCommon,
  IconDashboard,
  IconFile,
  IconHome,
  IconPlayArrow,
  IconSwap,
  IconStar,
  IconCode,
  IconBook,
  IconFilePdf,
  IconCalendarClock,
  IconCloud,
  IconDesktop,
} from '@arco-design/web-react/icon';
import { useCreation } from 'ahooks';
import { cloneDeep, isEmpty } from 'lodash';
import React, { useMemo } from 'react';
import { Redirect, Route, Switch, useLocation, useParams } from 'react-router';
import ManageCircuit from '@/assets/menu-icon/manage-circuit.svg';
import Component from '@/assets/menu-icon/component.svg';
import Liebiao from '@/assets/menu-icon/liebiao.svg';
import Liebiaomoshi from '@/assets/menu-icon/liebiaomoshi.svg';
import Listol from '@/assets/menu-icon/listol.svg';
import Youxi from '@/assets/menu-icon/youxuliebiaozhongyitiao.svg';
import Branches from '@/assets/menu-icon/branches.svg';
import Yunchuanshu from '@/assets/menu-icon/yunchuanshu.svg';
import Code from '@/assets/menu-icon/code.svg';
import Setting from '@/assets/menu-icon/setting.svg';
import Yun from '@/assets/menu-icon/yun.svg';
import Message from '@/assets/menu-icon/message.svg';
import Xmgj from '@/assets/menu-icon/xmgj.svg';
import Fzdh from '@/assets/menu-icon/fzdh.svg';

import { ProjectLayoutContextProvider, useProjectContext } from './context';
import { ComMenu, Dynamic } from '@/components/Layout/types';
import { flattenTree } from '@/utils/tree';
import { AiOutlineGlobal } from 'react-icons/ai';

const menuIcons = {
  forms: <IconFile className="icon-size" />,
  faqs: <Liebiao className="icon-size" />,
  dashboard: <IconDashboard className="icon-size" />,
  home: <IconHome className="icon-size" />,
  entities: <IconCommon className="icon-size" />,
  intents: <Liebiaomoshi className="icon-size" />,
  bots: <Youxi className="icon-size" />,
  branch: <Branches className="icon-size" />,
  cloud: <Yun className="icon-size" />,
  slots: <Listol className="icon-size" />,
  stories: <Code className="icon-size" />,
  train: <Yunchuanshu className="icon-size" />,
  run: <IconPlayArrow className="icon-size" />,
  history: <Message className="icon-size" />,
  shareAlt: <ManageCircuit className="icon-size" />,
  palette: <Component className="icon-size" />,
  archive: <Xmgj className="icon-size" />,
  setting: <Setting className="icon-size" />,
  complex: <Fzdh className="icon-size" />,
  synonyms: <IconSwap className="icon-size" />,
  star: <IconStar className="icon-size" />,
  action: <IconCode className="icon-size" />,
  knowledge: <IconBook className="icon-size" />,
  text: <IconFile className="icon-size" />,
  web: <AiOutlineGlobal className="icon-size" />,
  pdf: <IconFilePdf className="icon-size" />,
  cycle: <IconCalendarClock className="icon-size" />,
  llm: <IconCloud className="icon-size" />,
  statistics: <IconDesktop className="icon-size" />,
};
interface ProjectLayoutProps {
  routes: MenuRoute[];
}

function parseDynamic(dynamic: Dynamic, routeParams: any): Dynamic {
  const api = dynamic.api
    .split('/')
    .map((s) => {
      if (s.startsWith(':')) {
        const key = s.replace(':', '');
        return routeParams[key];
      }
      return s;
    })
    .join('/');
  return { ...dynamic, api };
}
const ProjectLayout = ({ routes }: ProjectLayoutProps) => {
  const { pathname } = useLocation();
  const routeParams = useParams();
  const { id, name } = useProjectContext();
  const newRouter = useMemo(() => {
    return flattenTree(cloneDeep(routes)).filter((m) => !!m.componentPath);
  }, [routes]);
  const headers = useCreation<ComBreadcrumb[]>(() => {
    const [, , ...other] = pathname.split('/').filter((f) => !isEmpty(f));
    const withRoutes = other
      .map((o) =>
        newRouter.find((r) => r.name.replace('menu.projects.id.', '') === o)
      )
      .filter((it) => !!it)
      .map(({ name, icon, path }) => ({
        key: name,
        icon: menuIcons[icon],
        path: path?.replace(':id', id),
      }));
    return [
      { key: 'menu.projects', path: '/projects', icon: <IconApps /> },
      { key: name || 'loading' },
      ...withRoutes,
    ];
  }, [id, pathname, name]);
  const menus = useCreation<ComMenu[]>(() => {
    const mapMenu = (paths) => {
      return paths
        .filter((r) => !r.hideInMenu)
        .map(
          ({
            name,
            path,
            icon,
            spliter = false,
            selectable,
            dynamic,
            children,
            defaultExpend,
          }) => {
            return {
              name,
              path: path.replace(':id', id),
              icon: menuIcons[icon],
              children: children ? mapMenu(children) : false,
              selectable,
              spliter,
              dynamic: dynamic ? parseDynamic(dynamic, routeParams) : undefined,
              defaultExpend: defaultExpend ?? true,
            };
          }
        );
    };
    return mapMenu(routes);
  }, [routes, id, routeParams]);

  return (
    <ComLayout
      defaultSelectedKeys={[
        pathname === `/projects/${id}` ? menus[0].path : pathname,
      ]}
      headers={headers}
      menus={menus}
    >
      <Switch>
        {newRouter.map((route) => {
          return (
            <Route
              exact
              key={route.path}
              path={route.path}
              component={lazyload(
                () => import(`@/pages/${route.componentPath}`)
              )}
            />
          );
        })}
        <Route exact path="/projects/:id">
          <Redirect to={routes[0]?.path.replace(':id', id)} />
        </Route>
        <Route
          path="*"
          component={lazyload(() => import('@/pages/exception/403'))}
        />
      </Switch>
    </ComLayout>
  );
};

export default (props: ProjectLayoutProps) => {
  return (
    <ProjectLayoutContextProvider>
      <ProjectLayout {...props} />
    </ProjectLayoutContextProvider>
  );
};
