import { ObjectArrayHelper } from '@/graph-next/helper';
import useLocale from '@/utils/useLocale';
import { Button, Input, List, Space, Typography } from '@arco-design/web-react';
import {
  IconCheck,
  IconClose,
  IconDelete,
  IconEdit,
} from '@arco-design/web-react/icon';
import React, { useRef, useState } from 'react';
import i18n from './locale';

interface ResultProps {
  value?: string[];
  onChange?: (val: string[]) => void;
  error?: boolean;
}
const InputWrap = ({
  initialValue,
  disabled,
  onCurrentChange,
  index,
  onChange,
  help,
}) => {
  const [value, setValue] = useState(initialValue);
  const ref = useRef<any>();
  return (
    <>
      <Input
        value={value}
        disabled={disabled}
        ref={ref}
        suffix={
          disabled ? (
            <Space size="mini">
              <Button
                type="secondary"
                size="small"
                icon={<IconEdit />}
                onClick={() => onCurrentChange(index)}
              />
              <Button
                type="secondary"
                status="danger"
                size="small"
                icon={<IconDelete />}
                onClick={() => onChange(value, index, 'delete')}
              />
            </Space>
          ) : (
            <Space size="mini">
              <Button
                type="secondary"
                size="small"
                status="success"
                icon={<IconCheck />}
                onClick={() => {
                  onChange(value, index);
                  ref.current.blur();
                }}
              />
              <Button
                type="secondary"
                size="small"
                icon={<IconClose />}
                onClick={() => {
                  onCurrentChange(-1);
                  setValue(initialValue);
                  ref.current.blur();
                }}
              />
            </Space>
          )
        }
        onChange={setValue}
      />
      {help}
    </>
  );
};
const Result = ({ value, onChange, error }: ResultProps) => {
  const t = useLocale(i18n);
  const [current, setCurrent] = useState(-1);
  const onItemChange = (val, i, option) => {
    if (value) {
      let result;
      if (option === 'delete') {
        result = ObjectArrayHelper.del(value, (_, o) => o === i);
      } else {
        result = ObjectArrayHelper.override(value, val, (_, o) => o === i);
      }
      onChange(result);
      setCurrent(-1);
    }
  };
  const onCurrentChange = (index) => {
    if (current !== -1) {
      return;
    }
    setCurrent(index);
  };
  if (error)
    return (
      <Typography.Text type="warning">{value?.[0] || 'error'}</Typography.Text>
    );
  return (
    <List
      size="small"
      virtualListProps={{
        height: 254,
      }}
      bordered
    >
      {value?.map((item, i) => (
        <List.Item key={item + i}>
          <InputWrap
            disabled={i !== current}
            onCurrentChange={onCurrentChange}
            initialValue={item}
            index={i}
            onChange={onItemChange}
            help={
              current === i && (
                <Typography.Text type="secondary">
                  {t['user.ext.editing.error']}
                </Typography.Text>
              )
            }
          />
        </List.Item>
      ))}
    </List>
  );
};

export default Result;
