import { Button, Tooltip } from '@arco-design/web-react';
import {
  IconDelete,
  IconEdit,
  IconRotateRight,
  IconStop,
  IconUpload,
} from '@arco-design/web-react/icon';
import React, { ReactNode } from 'react';
import i18n from '@/locale';
import useLocale from '@/utils/useLocale';

type TriggerProps = {
  onClick?: any;
  loading?: boolean;
  children?: ReactNode;
  status?: 'default' | 'danger' | 'warning' | 'success';
  disabled?: boolean;
  className?: string;
};

export const DeleteTrigger = (props: TriggerProps) => {
  const t = useLocale(i18n);
  return (
    <Tooltip content={t['delete']}>
      {/* <Button
        type="text"
        icon={}
        status="danger"
        shape="circle"
        key="delete"
        className="cursor-pointer"
        {...props}
      /> */}
      <IconDelete
        className="cursor-pointer"
        style={{ color: 'red' }}
        {...props}
      />
    </Tooltip>
  );
};

export const EditTrigger = (props: TriggerProps) => {
  const t = useLocale(i18n);

  return (
    <Tooltip content={t['edit']}>
      {/* <Button
        type="text"
        icon={<IconEdit />}
        status="warning"
        shape="circle"
        className="cursor-pointer"
        {...props}
      /> */}
      <IconEdit
        className="cursor-pointer"
        style={{ color: '#FADC19' }}
        {...props}
      />
    </Tooltip>
  );
};

export const LinkTrigger = (props: TriggerProps) => {
  const t = useLocale(i18n);
  return (
    <Tooltip content={t['link']}>
      <Button
        type="text"
        icon={<IconRotateRight />}
        shape="circle"
        key="link"
        className="cursor-pointer"
        {...props}
      />
    </Tooltip>
  );
};

export const ReleaseTrigger = (props: TriggerProps) => (
  <Button
    type="text"
    icon={<IconUpload />}
    // status="success"
    key="release"
    {...props}
  ></Button>
);

export const StopReleaseTrigger = (props: TriggerProps) => (
  <Button
    type="text"
    icon={<IconStop />}
    // status="success"
    key="release"
    {...props}
  ></Button>
);
