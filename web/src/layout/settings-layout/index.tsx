import { MenuRoute } from '@/authentication-routes';
import ComLayout from '@/components/Layout';
import { ComMenu } from '@/components/Layout/types';
import lazyload from '@/utils/lazyload';
import { flattenTree } from '@/utils/tree';
import { useCreation } from 'ahooks';
import { cloneDeep } from 'lodash';
import React, { createElement } from 'react';
import { Redirect, Route, Switch } from 'react-router';
import menuIcons from './menuIcons';

interface SettingsLayoutProps {
  routes: MenuRoute[];
}
const SettingsLayout = ({ routes }: SettingsLayoutProps) => {
  const menus = useCreation<ComMenu[]>(() => {
    function mapMenu(configs) {
      return configs
        .filter((c) => !c.hideInMenu)
        .map((route) => ({
          ...route,
          icon:
            route.icon &&
            createElement(menuIcons[route.icon], {
              className: 'icon-size',
            }),
          children: route.children ? mapMenu(route.children) : undefined,
        }));
    }
    return mapMenu(routes[0].children);
  }, []);
  const flatRoutes = useCreation(() => flattenTree(cloneDeep(routes)), []);
  return (
    <ComLayout menus={menus}>
      <Switch>
        {flatRoutes.map((route) => {
          return route.redirect ? (
            <Route key={route.path} exact path={route.path}>
              <Redirect to={route.redirect} />
            </Route>
          ) : (
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
      </Switch>
    </ComLayout>
  );
};

export default SettingsLayout;
