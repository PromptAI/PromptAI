import { Button, ButtonProps, Message } from '@arco-design/web-react';
import React, { memo, useCallback, useState } from 'react';

interface DownloadTemplateProps {
  name: string;
}
async function downloadTemplate(name: string) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        const link = document.createElement('a');
        link.style.display = 'none';
        link.href = `/templates/${name}`;
        link.setAttribute('download', name);
        document.body.appendChild(link);
        link.click();
        link.remove();
        resolve('ok');
      } catch (e) {
        reject(e);
      }
    }, 500);
  });
}
const DownloadTemplate = ({
  name,
  children,
  ...btnProps
}: DownloadTemplateProps &
  Omit<ButtonProps, 'onClick' | 'name' | 'loading'>) => {
  const [loading, setLoading] = useState(false);
  const onClick = useCallback(() => {
    setLoading(true);
    downloadTemplate(name)
      .catch(() => Message.error('Unknown File'))
      .finally(() => setLoading(false));
  }, [name]);
  return (
    <Button {...btnProps} onClick={onClick} loading={loading}>
      {children}
    </Button>
  );
};

export default memo(DownloadTemplate);
