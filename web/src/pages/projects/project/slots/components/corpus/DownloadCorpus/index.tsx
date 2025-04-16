import { downloadFile } from '@/utils/downloadObject';
import { get } from '@/utils/request';
import useLocale from '@/utils/useLocale';
import { Button } from '@arco-design/web-react';
import React, { useState } from 'react';
import i18n from './locale';

const DownloadCorpus = ({ name, slotId }) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  const handle = () => {
    setLoading(true);
    get(`/api/project/component/entity/dictionary/${slotId}`)
      .then((res) => downloadFile(res, `${name}.txt`))
      .finally(() => setLoading(false));
  };
  return (
    <Button loading={loading} type="outline" size="mini" onClick={handle}>
      {t['slot.tale.dictionary.download']}
    </Button>
  );
};

export default DownloadCorpus;
