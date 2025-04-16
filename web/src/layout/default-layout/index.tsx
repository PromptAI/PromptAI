import { MenuRoute } from '@/authentication-routes';
import ComLayout from '@/components/Layout';
import lazyload from '@/utils/lazyload';
import useLocale from '@/utils/useLocale';
import { Space, Tabs, Typography } from '@arco-design/web-react';
import React, { useEffect, useState } from 'react';
import { Route, Switch, useHistory } from 'react-router';
import { Header } from '../global-layout';

const DefaultLayout = ({
  route,
  headers,
}: {
  route: MenuRoute;
  headers: Header[];
}) => {
  const t = useLocale();
  const history = useHistory();
  const [activeTab, setActiveTab] = useState<string>();
  useEffect(() => {
    setActiveTab(window.location.pathname);
  }, []);
  return (
    <ComLayout>
      <Tabs
        activeTab={activeTab}
        onClickTab={(path) => {
          history.push(path);
        }}
      >
        {headers.map(({ path, key, icon }) => {
          return (
            <Tabs.TabPane
              key={path}
              title={
                <Space>
                  {icon}
                  <Typography.Text>{t[`menu.${key}`]}</Typography.Text>
                </Space>
              }
            />
          );
        })}
      </Tabs>

      <Switch>
        <Route
          exact
          path={route.path}
          component={lazyload(() => import(`@/pages/${route.componentPath}`))}
        />
      </Switch>
    </ComLayout>
  );
};

export default DefaultLayout;
