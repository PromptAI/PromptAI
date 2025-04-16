import { Space, Typography } from '@arco-design/web-react';
import React, { useMemo } from 'react';
import DeleteCorpus from './DeleteCorpus';
import DownloadCorpus from './DownloadCorpus';
import PreviewCorpus from './PreviewCorpus';
import UploadCorpus from './UploadCorpus';

interface OperationMenuCorpusProps {
  row: any;
  classifier: string;
  onSuccess: () => void;
  title: string;
  placeholder: string;
}
const OperationMenuCorpus = ({
  row,
  classifier,
  title,
  placeholder,
  onSuccess,
}: OperationMenuCorpusProps) => {
  const countName = useMemo(
    () => `${row?.data.dictionary?.length || 0} ${classifier}`,
    [classifier, row]
  );
  return (
    <div className="flex justify-center items-center flex-wrap">
      <Typography.Text>{countName}</Typography.Text>
      <Space
        size="small"
        style={{ flexWrap: 'wrap', justifyContent: 'center', marginLeft: 8 }}
      >
        <PreviewCorpus
          items={row.data?.dictionary || []}
          title={title}
          placeholder={placeholder}
          name={row.display}
        />
        <UploadCorpus
          slotId={row.id}
          name={row.display}
          onSuccess={onSuccess}
        />
        <DownloadCorpus slotId={row.id} name={row.display} />
        <DeleteCorpus slotId={row.id} onSuccess={onSuccess} />
      </Space>
    </div>
  );
};

export default OperationMenuCorpus;
