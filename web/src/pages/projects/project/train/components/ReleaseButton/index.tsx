import React, { ComponentProps } from 'react';
import styles from './index.module.less';
import classNames from 'classnames';
import { BsRocket } from 'react-icons/bs';
import useLocale from '@/utils/useLocale';
import i18n from '../../locale';

interface ReleaseButtonProps
  extends Omit<ComponentProps<'button'>, 'children'> {
  loading?: boolean;
  releasedTime?: string;
}
const ReleaseButton: React.FC<ReleaseButtonProps> = ({
  loading,
  releasedTime,
  className,
  ...props
}) => {
  const t = useLocale(i18n);
  return (
    <div className={styles.container}>
      <button
        className={classNames(
          styles.btn,
          { [styles.released]: !!releasedTime && !loading },
          className
        )}
        {...props}
      >
        <div className={styles.iconContainer}>
          {loading && (
            <>
              <BsRocket className={styles.placeholder} />
              <BsRocket className={styles.iconLoading} />
            </>
          )}
          {!loading && <BsRocket />}
        </div>
      </button>
      {releasedTime && (
        <div className={styles.releasedTime}>
          <p>{t['train.publish.released']}</p>
          <span>{releasedTime}</span>
        </div>
      )}
      {!releasedTime && !loading && (
        <span>{t['train.publish.notReleased']}</span>
      )}
      {loading && <span>{t['train.publish.releasing']}</span>}
    </div>
  );
};

export default ReleaseButton;
