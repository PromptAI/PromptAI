import { downloadRasaFile } from '@/api/components';
import { useProjectContext } from '@/layout/project-layout/context';
import { downloadFile } from '@/utils/downloadObject';
import { Button, Message } from '@arco-design/web-react';
import { IconDownload } from '@arco-design/web-react/icon';
import React, { useState } from 'react';

interface DownloadRasaProps {
  componentIds: string[];
  title: string;
}
const DownloadRasa = ({ componentIds, title }: DownloadRasaProps) => {
  const project = useProjectContext();
  const [loading, setLoading] = useState(false);
  const handleClick = async () => {
    setLoading(true);
    downloadRasaFile({ projectId: project.id, componentIds })
      .then((res) => {
        downloadFile(res, componentIds.join());
        Message.success(`${title}`);
      })
      .finally(() => setLoading(false));
  };
  return (
    <Button
      loading={loading}
      type="text"
      onClick={handleClick}
      icon={<IconDownload />}
    >
      {title}
    </Button>
  );
};

export default DownloadRasa;
