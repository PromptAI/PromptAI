import React from 'react';
import { Button, ButtonProps, Modal } from '@arco-design/web-react';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { useRequest } from 'ahooks';
import { deleteLicense } from '@/api/licenses';

interface DeleteLicenseProps extends Omit<ButtonProps, 'loading' | 'onClick'> {
  row: any;
  onSuccess: () => void;
}
const DeleteLicense: React.FC<DeleteLicenseProps> = ({
  row,
  onSuccess,
  ...props
}) => {
  const t = useLocale(i18n);
  const { loading, runAsync } = useRequest(() => deleteLicense(row.id), {
    manual: true,
    refreshDeps: [row.id],
    onSuccess,
  });
  const onClick = (row) => {
    Modal.confirm({
      title: t['delete'],
      content:
        (row.used ? t['license.using'] : '') +
        (row.properties.subscription
          ? t['delete.subtitle.subscription']
          : t['delete.subtitle']),
      onOk: runAsync,
      footer: (cancel, ok) => [ok, cancel],
    });
  };

  return <Button {...props} loading={loading} onClick={() => onClick(row)} />;
};

export default DeleteLicense;
