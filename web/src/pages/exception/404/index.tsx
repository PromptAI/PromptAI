import React from 'react';
import { Result, Button } from '@arco-design/web-react';
import locale from './locale';
import useLocale from '@/utils/useLocale';
import styles from './style/index.module.less';
import { useHistory } from 'react-router';
import nProgress from 'nprogress';

function Exception403() {
  const t = useLocale(locale);
  const history = useHistory();
  const goback = () => {
    nProgress.start();
    history.goBack();
    nProgress.done();
  };
  return (
    <div className={styles.container}>
      <div className={styles.wrapper}>
        <Result
          className={styles.result}
          status="404"
          subTitle={t['exception.result.404.description']}
          extra={
            <Button key="back" type="primary" onClick={goback}>
              {t['exception.result.404.back']}
            </Button>
          }
        />
      </div>
    </div>
  );
}

export default Exception403;
