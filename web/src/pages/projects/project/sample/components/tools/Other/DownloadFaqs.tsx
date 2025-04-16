import useLocale from '@/utils/useLocale';
import React, { useState } from 'react';
import i18n from './locale';
import { Button } from '@arco-design/web-react';
import { IconDownload } from '@arco-design/web-react/icon';
import { downloadFaqs } from '@/api/faq';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { downloadFile } from '@/utils/downloadObject';

export interface DownloadFaqsProps {
  components?: string[];
}
const DownloadFaqs: React.FC<DownloadFaqsProps> = ({ components }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [loading, setLoading] = useState(false);
  const handleClick = async () => {
    setLoading(true);
    try {
      const response = await downloadFaqs(projectId, components);
      downloadFile(response);
    } catch (error) {
      console.error(error);
    }
    setLoading(false);
  };
  return (
    <Button
      type="text"
      onClick={handleClick}
      icon={<IconDownload />}
      loading={loading}
    >
      {t['sample.download']}
    </Button>
  );
};

export default DownloadFaqs;
