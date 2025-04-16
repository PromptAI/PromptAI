import React from 'react';
import { Layout } from '@arco-design/web-react';
import { FooterProps } from '@arco-design/web-react/es/Layout/interface';
import cs from 'classnames';
import useLocale from '@/utils/useLocale';
import locale from '@/locale';
import styles from './style/index.module.less';

const isEn = process.env.REACT_APP_LANG === 'en-US';
function Footer(props: FooterProps = {}) {
  const { className, ...restProps } = props;
  const t = useLocale(locale);
  return (
    <Layout.Footer className={cs(styles.footer, className)} {...restProps}>
      {!isEn && (
        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noreferrer"
          className={styles.footer}
        >
          ⓒ Copyright {t['footer.copyright']} 浙ICP备2022024214号
        </a>
      )}
      {isEn && (
        <div className={styles.footerContent}>
          <a href="/privacie.html" target="_blank">
            Privacy
          </a>
          <a href="/term.html" target="_blank">
            Term
          </a>
        </div>
      )}
    </Layout.Footer>
  );
}

export default Footer;
