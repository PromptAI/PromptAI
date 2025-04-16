import downloadJSON from '@/utils/downloadObject';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { IconDownload } from '@arco-design/web-react/icon';
import React from 'react';

const Download = ({ factory }: { factory: () => Promise<any> }) => {
  const t = useLocale();
  const onCommand = async () => {
    await factory()
      .then((project) => {
        downloadJSON(project, project.label);
      })
      .catch((e) => Message.error(e));
  };
  return (
    <Button type="text" onClick={onCommand} icon={<IconDownload />}>
      {t['command.download']}
    </Button>
  );
};

export default Download;
