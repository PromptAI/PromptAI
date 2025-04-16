import { downloadUrlFile } from '@/utils/downloadObject';
import { Button } from '@arco-design/web-react';
import { IconDownload } from '@arco-design/web-react/icon';
import React, { useState } from 'react';

interface DownloadRasaTemlateProps {
  title: string;
}
const DownloadRasaTemlate = ({ title }: DownloadRasaTemlateProps) => {
  const [loading, setLoading] = useState(false);
  function handleDownloadTemplate() {
    setLoading(true);
    downloadUrlFile('/templates/upload-faqs.csv');
    setLoading(false);
  }
  return (
    <Button
      type="text"
      onClick={handleDownloadTemplate}
      icon={<IconDownload />}
      loading={loading}
    >
      {title}
    </Button>
  );
};

export default DownloadRasaTemlate;
