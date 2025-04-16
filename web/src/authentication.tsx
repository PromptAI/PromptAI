import { IconApps } from '@arco-design/web-react/icon';
import { IoLibrary } from 'react-icons/io5';
import React, { useEffect } from 'react';
import GlobalLayout, { Header } from './layout/global-layout';
import { Redirect, Route, Switch, useHistory } from 'react-router';
import { useCreation } from 'ahooks';
import DefaultLayout from './layout/default-layout';
import { getAuthRoutes } from './authentication-routes';
import lazyload from './utils/lazyload';
import ProjectLayout from './layout/project-layout';
import { Modal } from '@arco-design/web-react';
import { useDefaultLocale } from './utils/useLocale';
import SettingsLayout from './layout/settings-layout';

const globalHeaders: Header[] = [
  {
    key: 'projects',
    icon: <IconApps className="app-breadcrumb-icon" />,
    path: '/projects',
  },
  {
    key: 'libs',
    icon: <IoLibrary className="app-breadcrumb-icon" />,
    path: '/libs',
  },
];

const Authentication = () => {
  const routes = useCreation(getAuthRoutes, []);
  const history = useHistory();
  const dt = useDefaultLocale();
  useEffect(() => {
    const code = localStorage.getItem('login_from_code');
    if (!history.location.pathname.startsWith('/profile') && !!code) {
      Modal.warning({
        title: dt['globalLayout.register.placeholder'],
        closable: true,
        mask: false,
        // maskClosable: false,
        okText: dt['globalLayout.register.okText'],
        onOk: () => {
          history.replace(`/profile?code=${code}`);
          localStorage.removeItem('login_from_code');
        },
        onCancel() {
          localStorage.removeItem('login_from_code');
        },
      });
    }
  }, [dt, history]);
  return (
    <GlobalLayout headers={globalHeaders}>
      <Switch>
        {routes.map((r) => {
          if (r.layout === 'project') {
            return (
              <Route
                key={r.path}
                path={r.path}
                render={(props) => (
                  <ProjectLayout {...props} routes={r.children} />
                )}
              />
            );
          }
          if (r.layout === 'settings-layout') {
            return (
              <Route
                key={r.path}
                path={r.path}
                render={(props) => <SettingsLayout {...props} routes={[r]} />}
              />
            );
          }
          return (
            <Route
              key={r.path}
              exact
              path={r.path}
              render={(props) => (
                <DefaultLayout headers={globalHeaders} {...props} route={r} />
              )}
            />
          );
        })}
        <Route exact path="/">
          <Redirect to={routes[0].path} />
        </Route>
        <Route
          path="*"
          component={lazyload(() => import('./pages/exception/403'))}
        />
      </Switch>
    </GlobalLayout>
  );
};

export default Authentication;
