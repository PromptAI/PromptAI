import './global-layout.less';
import {
  Avatar,
  Button,
  Divider,
  Dropdown,
  Image,
  Link,
  Menu,
  Space,
  Tooltip,
  Typography
} from '@arco-design/web-react';
import React, { memo, useContext, useMemo } from 'react';
import useLocale from '@/utils/useLocale';
import IconButton from '@/components/NavBar/IconButton';
import {
  IconLanguage,
  IconMoonFill,
  IconPoweroff,
  IconQuestion,
  IconSafe,
  IconSettings,
  IconSunFill,
  IconUser
} from '@arco-design/web-react/icon';
import { GlobalContext } from '@/context';
import useStorage from '@/utils/useStorage';
import { useMemoizedFn } from 'ahooks';
import { useHistory } from 'react-router';
import nProgress from 'nprogress';
import { UserState, useSelectorStore } from '@/store';
import { authMe, configMe, loginOut } from '@/api/auth';
import Token from '@/utils/token';
import { IoLibrary } from 'react-icons/io5';
import { BiPurchaseTagAlt } from 'react-icons/bi';
// import AgentTip from './components/AgentTip';
// import AgentManager from './components/AgentManager';
import Qrcode from '@/components/NavBar/qrcode';
import useDocumentLinks from '@/hooks/useDocumentLinks';
import { MdOutlineGeneratingTokens } from 'react-icons/md';
import { debounce } from 'lodash';
import { useDispatch } from 'react-redux';
import styled from 'styled-components';
import Singleton, { key_default_account } from '@/utils/singleton';

const isEnEnv = process.env.REACT_APP_LANG === 'en-US';

const SelectLang = memo(() => {
  const { lang, setLang } = useContext(GlobalContext);
  const name = useMemo(() => (lang === 'zh-CN' ? 'English' : '中文'), [lang]);
  const handleChange = () => {
    const target = lang === 'zh-CN' ? 'en-US' : 'zh-CN';
    setLang(target);
    configMe({ language: target.split('-')[0] });
  };
  return (
    <Button type="text" icon={<IconLanguage />} onClick={handleChange}>
      {name}
    </Button>
  );
});

const MenuWrap = styled(Menu)`
    max-height: unset;
`;

const UserDropdown = () => {
  const t = useLocale();
  const history = useHistory();
  const userInfo = useSelectorStore<UserState>('user');

  const [, setUserStatus] = useStorage('userStatus');

  const onClickMenuItem = useMemoizedFn((key: string) => {
    switch (key) {
      case 'logout':
        setUserStatus(key);
        Token.remove();
        Singleton.remove(key_default_account);
        loginOut();
        window.location.href = '/login';
        break;
      case 'username':
        history.push('/profile');
        break;
      case 'purchase-tokens':
        history.push('/tokens');
        break;
      case 'settings':
        history.push('/settings');
        break;
    }
  });
  const disptach = useDispatch();
  const onVisibleChange = useMemo(
    () =>
      debounce((visible) => {
        if (visible) {
          authMe().then(({ restToken }) => {
            disptach({
              type: 'update-userInfo',
              payload: { userInfo: { restToken } }
            });
          });
        }
      }, 500),
    [disptach]
  );
  return (
    <Dropdown
      droplist={
        <MenuWrap onClickMenuItem={onClickMenuItem}>
          <Menu.Item key="username">
            <IconUser className="dropdown-icon" />
            <Typography.Text>
              {userInfo.username || userInfo.mobile}
            </Typography.Text>
          </Menu.Item>
          <Menu.Item key="settings">
            <IconSettings className="dropdown-icon" />
            <Typography.Text>{t['menu.settings']}</Typography.Text>
          </Menu.Item>
          <Divider style={{ margin: '4px 0' }} />
          <Menu.Item key="logout">
            <IconPoweroff className="dropdown-icon" />
            {t['navbar.logout']}
          </Menu.Item>
        </MenuWrap>
      }
      onVisibleChange={onVisibleChange}
      position="br"
    >
      <Avatar size={32} shape="circle" style={{ cursor: 'pointer' }}>
        <img
          alt="avatar"
          src={
            userInfo.avatar
              ? userInfo.avatar.startsWith('http')
                ? userInfo.avatar
                : `/api/blobs/get/${userInfo?.avatar}`
              : '/user.png'
          }
        />
      </Avatar>
    </Dropdown>
  );
};

export type Header = {
  key: string;
  icon?: React.ReactNode;
  path?: string;
};

const GlobalLayout = ({
                        children
                      }: {
  children: React.ReactNode;
  headers?: Header[];
}) => {
  const history = useHistory();
  const t = useLocale();
  const { theme, setTheme, lang } = useContext(GlobalContext);

  const linkTo = useMemoizedFn((path) => {
    nProgress.start();
    history.push(path);
    nProgress.done();
  });
  const docs = useDocumentLinks();
  return (
    <div className="global-layout">
      <header className="global-header">
        <Space size={64}>
          <Link onClick={() => linkTo('/')} style={{ lineHeight: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <Typography.Title
                heading={2}
                bold
                className="global-header-logo-text"
              >
                Prompt AI
              </Typography.Title>
              <i>Beta</i>
            </div>
          </Link>
        </Space>
        <Space>
          <Link>
            <Button
              icon={<IoLibrary />}
              type="text"
              onClick={() => history.push('/libs')}
            >
              {t['menu.libs']}
            </Button>
          </Link>
          {/* {canbeChangeLang && } */}
          <SelectLang />
          <Tooltip content={t['header.document']}>
            <div>
              <Link target="_blank" rel="noopener" href={docs.welcome}>
                <IconButton shape="circle" icon={<IconQuestion />} />
              </Link>
            </div>
          </Tooltip>
          {lang === 'zh-CN' && (
            <Tooltip
              content={
                <div
                  style={{
                    width: 140,
                    display: 'flex',
                    alignItems: 'center',
                    flexDirection: 'column'
                  }}
                >
                  <Image
                    width="100%"
                    src={`/api/blobs/group/qrcode?type=wechat&seed=${Date.now()}`}
                    alt="lamp"
                  />
                  <span style={{ fontSize: 14 }}>
                    {t['header.joinDiscuss']}
                  </span>
                </div>
              }
            >
              <div>
                <Qrcode />
              </div>
            </Tooltip>
          )}
          <Tooltip
            content={
              theme === 'light'
                ? t['settings.navbar.theme.toDark']
                : t['settings.navbar.theme.toLight']
            }
          >
            <IconButton
              icon={theme !== 'dark' ? <IconMoonFill /> : <IconSunFill />}
              onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            />
          </Tooltip>
          <UserDropdown />
        </Space>
      </header>
      <main className="global-content">{children}</main>
    </div>
  );
};

export default GlobalLayout;
