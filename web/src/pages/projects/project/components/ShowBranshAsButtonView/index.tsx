import React, { useMemo } from 'react';
import styles from './index.module.less';
import { Button, Input } from '@arco-design/web-react';
import { IconSend } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

interface ButtonItem {
  label: string;
  value: string;
}
interface ShowBranshAsButtonViewProps {
  buttons: ButtonItem[];
  max?: number;
  mode: 'none' | 'all' | 'custom';
  shows: string[];
}
const ShowBranshAsButtonView: React.FC<ShowBranshAsButtonViewProps> = ({
  buttons,
  max = 3,
  mode,
  shows,
}) => {
  const t = useLocale(i18n);
  const options = useMemo(() => {
    if (mode === 'all') return buttons.slice(0);
    if (mode === 'custom')
      return buttons
        .filter((b) => shows?.some((s) => s === b.value))
        .slice(0)
        .slice(0, max);
    return [];
  }, [buttons, max, mode, shows]);
  return (
    <div className={styles.Container}>
      <div className={styles.Robot}>
        <div className={styles.RobotHeader}>
          <img src="/robot.png" />
        </div>
        <div className={styles.RobotContentContainer}>
          <h5>{t['view.robot']}</h5>
          <div className={styles.RobotContent}>
            <div>{t['view.your.welcome']}</div>
            <div className={styles.RobotContentButtons}>
              {options.map(({ label, value }) => (
                <Button key={value} type="outline" size="small">
                  {label}
                </Button>
              ))}
              {mode === 'custom' && shows?.length > max && (
                <Button type="outline" size="small">
                  ...
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
      <Input placeholder="..." readOnly suffix={<IconSend />} />
    </div>
  );
};

export default ShowBranshAsButtonView;
