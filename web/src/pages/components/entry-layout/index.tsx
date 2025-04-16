import React, {
  PropsWithChildren,
  useContext,
  useEffect,
  useMemo,
} from 'react';

import Footer from '@/components/Footer';
import css from './index.module.less';
import cls from 'classnames';
import { Link } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from '@/pages/entry/login/locale';
import { GlobalContext } from '@/context';
import { configMe } from '@/api/auth';
import useDocumentLinks from '@/hooks/useDocumentLinks';

interface EntryLayoutProps {
  title?: string;
  types?: { type: string; name: string; icon: React.ReactNode }[];
  value?: string;
  onChange?: (t: string) => void;
  custom?: boolean;
  backend?: boolean;
}
const canbeChangeLang = process.env.REACT_APP_LANG_CHANGE === 'yes';
const SelectLang = ({ backend }) => {
  const { lang, setLang } = useContext(GlobalContext);
  const name = useMemo(() => (lang === 'zh-CN' ? 'English' : '中文'), [lang]);
  const handleChange = () => {
    const target = lang === 'zh-CN' ? 'en-US' : 'zh-CN';
    setLang(target);
    backend && configMe({ language: target.split('-')[0] });
  };
  return (
    <a className="arco-link" onClick={handleChange}>
      {name}
    </a>
  );
};
function EntryLayout({
  title,
  types,
  value,
  onChange = (t: any) => t,
  custom = false,
  backend = false,
  children,
}: PropsWithChildren<EntryLayoutProps>) {
  const t = useLocale(i18n);

  useEffect(() => {
    setTimeout(() => {
      document.body.removeAttribute('arco-theme');
    });
  }, []);
  const docs = useDocumentLinks();
  const { lang } = useContext(GlobalContext);
  return (
    <div>
      <a
        className={css.entryLogo}
        href={docs.website}
        target="_blank"
        rel="noreferrer"
      >
        <span>PROMPT AI</span>
        <i>Beta</i>
      </a>
      <div className={css.entryNav}>
        {canbeChangeLang && <SelectLang backend={backend} />}
        <Link target="_blank" rel="noopener" href={docs.welcome}>
          {t['login.nav.docs']}
        </Link>
        <Link target="_blank" rel="noopener" href={docs.example}>
          {t['login.nav.examples']}
        </Link>
        <Link target="_blank" rel="noopener" href={docs.welcome}>
          {t['login.nav.aboutUs']}
        </Link>
      </div>
      {!custom && lang === 'zh-CN' && canbeChangeLang && (
        <div className={css.contactUs}>
          <div>
            <img
              src="https://app.promptai.cn/api/blobs/group/qrcode?type=wechat"
              alt=""
            />
          </div>
          <span>{t['entry.scan']}</span>
        </div>
      )}
      <div className={css.entryBackground}>
        {!custom && (
          <div className={css.main}>
            <div
              className={cls(css.entryBox, {
                [css.entryBoxRegister]: types?.length === 1,
              })}
            >
              <h1 className={css.entryTitle}>{title}</h1>
              <div className={css.entryFormContainer}>
                <div className={css.entryTypes}>
                  {types?.map(({ type: t, icon, name }) => (
                    <div
                      key={t}
                      className={cls({ [css.selected]: value === t })}
                      onClick={() => onChange(t)}
                    >
                      {icon}
                      <span>{name}</span>
                    </div>
                  ))}
                </div>
                <div className={css.entryBody}>{children}</div>
              </div>
            </div>
          </div>
        )}
        {custom && children}
        <Footer style={{ marginTop: 'auto' }} />
      </div>
    </div>
  );
}

EntryLayout.displayName = 'EntryLayout';

export default EntryLayout;
