import './style/global.less';
import './index.css';
import React, { useEffect, useLayoutEffect } from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { ConfigProvider } from '@arco-design/web-react';
import { BrowserRouter, Switch, Route, useHistory } from 'react-router-dom';
import store from './store';
import { GlobalContext, useGlobalContext } from './context';
import Login from './pages/entry/login';
import changeTheme from './utils/changeTheme';
import useStorage from './utils/useStorage';
import Authentication from './authentication';
import Register from './pages/entry/register';
import Applying from './pages/entry/applying';
import VersionControl from './version-control';
import { QueryClient, QueryClientProvider } from 'react-query';
import useLang from './hooks/useLang';
import Bootstrap from './bootstrap';
import {
  AutoRefreshTokenPlugin,
  BackendLangPlugin,
  InitialStorePlugin,
  LicensePlugin,
  RedirectLoginPlugin,
  ViewHeightPlugin,
} from './bootstrap/plugins';
import Forgot from './pages/entry/forgot';
import OAuthComponent from './pages/entry/login/google/OAuthComponent';
import CheckLicense from './pages/check-license';
import Templates from '@/pages/entry/templates';

// Create a client
const queryClient = new QueryClient();
const VersionAuthentication = () => {
  const history = useHistory();
  const { setLang } = useGlobalContext();
  useLayoutEffect(() => {
    const bootstrap = new Bootstrap();
    bootstrap.apply(new ViewHeightPlugin());
    bootstrap.apply(new LicensePlugin(history));
    bootstrap.apply(new InitialStorePlugin());
    bootstrap.apply(new BackendLangPlugin({ onLangChange: setLang }));
    bootstrap.apply(new AutoRefreshTokenPlugin());
    bootstrap.apply(new RedirectLoginPlugin());
    bootstrap.mouted();
    return () => {
      bootstrap.unmouted();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <VersionControl>
      <Authentication />
    </VersionControl>
  );
};

const Index = () => {
  const [lang, setLang, locale] = useLang();

  const [theme, setTheme] = useStorage('arco-theme', 'light');
  useEffect(() => {
    changeTheme(theme);
  }, [theme]);

  const contextValue = {
    lang,
    setLang,
    theme,
    setTheme,
  };

  return (
    <ConfigProvider
      locale={locale}
      componentConfig={{
        Modal: {
          escToExit: false,
          maskClosable: false,
          footer: (cancel, ok) => (
            <>
              {ok}
              {cancel}
            </>
          ),
        },
      }}
    >
      <Provider store={store}>
        <QueryClientProvider client={queryClient}>
          <GlobalContext.Provider value={contextValue}>
            <Switch>
              <Route path="/login" component={Login} />
              <Route path="/register" component={Register} />
              <Route path="/forgot" component={Forgot} />
              <Route path="/applying" component={Applying} />
              <Route path="/oauth" component={OAuthComponent} />
              <Route path="/templates" component={Templates} />
              <Route path="/" component={VersionAuthentication} />
            </Switch>
          </GlobalContext.Provider>
        </QueryClientProvider>
      </Provider>
    </ConfigProvider>
  );
};

ReactDOM.render(
  <BrowserRouter>
    <Index />
  </BrowserRouter>,
  document.getElementById('root')
);
