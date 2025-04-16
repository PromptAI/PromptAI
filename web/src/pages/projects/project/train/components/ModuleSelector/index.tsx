import { ObjectArrayHelper } from '@/graph-next/helper';
import useLocale, { useDefaultLocale } from '@/utils/useLocale';
import {
  Button,
  Card,
  Checkbox,
  Empty,
  Space,
  Spin,
} from '@arco-design/web-react';
import { IconSelectAll, IconSwap, IconSync } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from '../../locale';
import styles from './index.module.less';
import classNames from 'classnames';

type ValueItem = {
  id: string;
  name: string;
  description?: string;
  checked?: boolean;
  disabled?: boolean;
  type: 'faq-root' | 'conversation';
};
type ModuleSelectorProps = {
  value: ValueItem[];
  onChange?: (val: ValueItem[]) => void;
  title: React.ReactNode;
  loading: boolean;
  refresh: () => void;
  released?: boolean;
};
const ModuleSelector = ({
  value,
  onChange,
  title,
  loading,
  refresh,
  released,
}: ModuleSelectorProps) => {
  const t = useLocale(i18n);
  const dt = useDefaultLocale();
  const selectAll = () => {
    onChange(
      value.map((item) => ({ ...item, checked: item.disabled ? false : true }))
    );
  };
  const selectReverse = () => {
    onChange(
      value.map((item) => ({
        ...item,
        checked: item.disabled ? false : !item.checked,
      }))
    );
  };
  const selectItem = (id: string, checked: boolean) => {
    onChange(ObjectArrayHelper.update(value, { checked }, (v) => v.id === id));
  };
  return (
    <Card
      title={title}
      bordered={false}
      size="small"
      extra={
        <Space>
          <Button
            size="small"
            type="text"
            icon={<IconSelectAll />}
            onClick={selectAll}
          >
            {t['train.all.selection']}
          </Button>
          <Button
            size="small"
            type="text"
            icon={<IconSwap />}
            onClick={selectReverse}
          >
            {t['train.reverse.selection']}
          </Button>
          <Button
            size="small"
            type="text"
            icon={<IconSync />}
            onClick={refresh}
          >
            {t['train.refresh']}
          </Button>
        </Space>
      }
    >
      <Spin loading={loading} className="w-full">
        <div className={styles.container}>
          {value.map(({ id, name, description, checked, disabled }) => (
            <div
              key={id}
              className={classNames(styles.item, {
                [styles.released]: released && checked,
              })}
              onClick={() => !disabled && selectItem(id, !checked)}
            >
              <div className={styles.titleContainer}>
                <span className={styles.title}>{name}</span>
                <Checkbox disabled={disabled} checked={checked} />
              </div>
              <div className={styles.body}>
                <div title={description} className={styles.description}>
                  {description}
                </div>
              </div>
              <div className={styles.footer}>
                {!(released && checked) && t['train.unpublish']}
              </div>
            </div>
          ))}
        </div>
        {value.length === 0 && <Empty description={dt['common.empty']} />}
      </Spin>
    </Card>
  );
};

export default ModuleSelector;
