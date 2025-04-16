import { ObjectArrayHelper } from '@/graph-next/helper';
import { Input } from '@arco-design/web-react';
import { IconMinusCircle, IconPlusCircle } from '@arco-design/web-react/icon';
import { useControllableValue } from 'ahooks';
import { isEmpty } from 'lodash';
import React, { ReactNode, useState } from 'react';
import styles from './index.module.less';
import classNames from 'classnames';

type PairType = {
  key: string;
  value: string;
};
type PairProps = {
  value?: PairType;
  onChange?: (v: PairType) => void;
  keyName?: string;
  valueName?: string;
  spliter?: ReactNode;
  showLabel?: boolean;
  onAddClick?: () => void;
  onMinusClick?: () => void;
  showAdd?: boolean;
  showMinus?: boolean;
  reversed?: boolean;
};

const Pair = ({
  value,
  onChange,
  keyName = 'key',
  valueName = 'value',
  spliter = ':',
  showLabel = false,
  onMinusClick,
  onAddClick,
  showAdd = false,
  showMinus = false,
  reversed = false,
}: PairProps) => {
  const [keyError, setError] = useState(false);
  const onKeyChange = (key: string) => {
    setError(isEmpty(key));
    onChange({ ...value, key });
  };
  const onValueChange = (val: string) => {
    onChange({ ...value, value: val || '' });
  };
  return (
    <div className={styles.container}>
      {showLabel && (
        <div
          className={classNames(styles.labelContainer, {
            [styles.reversed]: reversed,
          })}
        >
          <span className={styles.pairLabelLeft}>{keyName}</span>
          <span className={styles.pairLabelRight}>{valueName}</span>
        </div>
      )}
      <div className={styles.pairContainer}>
        <IconPlusCircle
          className={classNames(styles.add, { [styles.hidden]: !showAdd })}
          onClick={onAddClick}
        />
        <div className={styles.pairItem}>
          <Input
            value={reversed ? value?.value : value?.key}
            placeholder={reversed ? valueName : keyName}
            onChange={reversed ? onValueChange : onKeyChange}
            status={keyError ? 'error' : null}
          />
        </div>
        <span className={styles.pairSpliter}>{spliter}</span>
        <div className={styles.pairItem}>
          <Input
            value={reversed ? value?.key : value?.value}
            placeholder={reversed ? keyName : valueName}
            onChange={reversed ? onKeyChange : onValueChange}
          />
        </div>
        <IconMinusCircle
          className={classNames(styles.minus, { [styles.hidden]: !showMinus })}
          onClick={onMinusClick}
        />
      </div>
    </div>
  );
};

type PairsProps = {
  value?: PairType[];
  onChange?: (v: PairType[]) => void;
  keyName?: string;
  valueName?: string;
  showLabel?: boolean;
  spliter?: ReactNode;
  reversed?: boolean;
};
const defaultPair: PairType = { key: '', value: '' };
export const Pairs = (props: PairsProps) => {
  const [value, onChange] = useControllableValue<PairType[]>({
    // 从 json 切换到 form，需要将 value 的 json 转换为数组
    value: isEmpty(props.value) 
      ? [defaultPair] 
      : typeof props.value === 'string' 
        ? Object.entries(JSON.parse(props.value)).map(([key, value]) => ({key, value}))
        : props.value,
    onChange: props.onChange,
  });
  const onAddClick = () => {
    onChange(ObjectArrayHelper.add(value, defaultPair));
  };
  const onMinusClick = (index: number) => {
    onChange(ObjectArrayHelper.del(value, (_, i) => i === index));
    if (index === 0) {
      onChange([defaultPair]);
    }
  };
  const onPariChange = (pair: PairType, index: number) => {
    onChange(ObjectArrayHelper.update(value, pair, (_, i) => i === index));
  };
  return (
    <div className={styles.pairsContainer}>
      {value.length > 0 &&
        value?.map((p, index) => (
          <Pair
            key={index}
            value={p}
            onChange={(v) => onPariChange(v, index)}
            keyName={props.keyName}
            valueName={props.valueName}
            showLabel={props.showLabel && index === 0}
            onAddClick={onAddClick}
            onMinusClick={() => onMinusClick(index)}
            showAdd={index === 0}
            showMinus
            spliter={props.spliter}
            reversed={props.reversed}
          />
        ))}
    </div>
  );
};

export default Pair;
