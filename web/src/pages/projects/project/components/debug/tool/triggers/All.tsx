import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { isEmpty } from 'lodash';
import React from 'react';
import { fetchData } from './fetch-data';
import i18n from './locale';
import { DebugRunProps } from './types';

const DebugAll = ({ title, icon, start }: DebugRunProps) => {
  const { projectId } = useUrlParams();
  const t = useLocale(i18n);
  const handle = async () => {
    try {
      const options = await fetchData(projectId);
      const okOptions =
        options?.filter((o) => !o.disabled).map((o) => o.value) || [];
      if (isEmpty(okOptions)) {
        Message.warning(t['debug.all.module.run.warning']);
      } else {
        start(okOptions, projectId);
      }
    } catch (e) {
      Message.error(t['debug.all.module.run.error']);
    }
  };
  return (
    <Button type="text" icon={icon} onClick={handle}>
      {title}
    </Button>
  );
};

export default DebugAll;
