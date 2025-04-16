import * as React from 'react';
import { Checkbox, Message, Spin } from '@arco-design/web-react';
import useUrlParams from '../../../hooks/useUrlParams';
import { useMutation } from 'react-query';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { enableWeb } from '@/api/text/web';

interface EnableColumnProps {
  row: any;
  onSuccess: () => void;
}
const EnableColumn: React.FC<EnableColumnProps> = ({ row, onSuccess }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { isLoading, mutate } = useMutation(enableWeb, {
    onSuccess: (data) => {
      Message.success(t[`enable.${data.data.enable}`]);
      onSuccess();
    },
  });
  return (
    <Spin loading={isLoading}>
      <Checkbox
        disabled={isLoading}
        checked={row.data.enable}
        onChange={(enable) =>
          mutate({ projectId, row: { ...row, data: { ...row.data, enable } } })
        }
      />
    </Spin>
  );
};

export default EnableColumn;
