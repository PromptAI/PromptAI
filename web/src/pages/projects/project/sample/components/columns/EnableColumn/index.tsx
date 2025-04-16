import { updateFaq } from '@/api/faq';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import { Checkbox, Message } from '@arco-design/web-react';
import React, { useState } from 'react';
import i18n from './locale';

const EnableColumn = ({ row, onSuccess }) => {
  const [disabled, setDisabled] = useState(false);
  const { projectId } = useUrlParams();
  const t = useLocale(i18n);
  const onChange = (checked: boolean) => {
    setDisabled(true);
    updateFaq({
      projectId,
      bot: row?.bot,
      user: {
        ...row?.user,
        data: {
          ...row?.user?.data,
          enable: checked,
        },
      },
    })
      .then(() => {
        Message.success(
          checked ? t['sample.enable.success'] : t['sample.enable.delete']
        );
        onSuccess();
      })
      .finally(() => {
        setDisabled(false);
      });
  };

  return (
    <Checkbox
      disabled={disabled}
      checked={row.user.data.enable}
      onChange={onChange}
    />
  );
};

export default EnableColumn;
