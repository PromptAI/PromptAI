import React, { useState, useEffect } from 'react';
import Token from '@/utils/token';
import {
  IconLoading,
  IconExclamationCircle,
} from '@arco-design/web-react/icon';
import { Tooltip } from '@arco-design/web-react';

type QrcodeForPreviewProps = Pick<
  React.ImgHTMLAttributes<HTMLImageElement>,
  'src'
> &
  Partial<Omit<React.ImgHTMLAttributes<HTMLImageElement>, 'src'>>;

const QrcodeForPreview: React.FC<QrcodeForPreviewProps> = ({
  src,
  ...rest
}) => {
  const [loading, setLoading] = useState(true);
  const [localUri, setLocalUri] = useState(null);
  useEffect(() => {
    fetch(src, {
      headers: {
        Authorization: Token.get(),
      },
    })
      .then((response) => response.blob())
      .then((imageBlob) => {
        // Then create a local URL for that image and print it
        const imageObjectURL = URL.createObjectURL(imageBlob);
        setLocalUri(imageObjectURL);
        setLoading(false);
      })
      .catch(() => {
        setLocalUri(0);
        setLoading(false);
      });
  }, [src]);
  return (
    <>
      {loading && <IconLoading />}
      {typeof localUri === 'string' && <img src={localUri} {...rest} />}
      {localUri === 0 && (
        <Tooltip content="Service error">
          <IconExclamationCircle color="warning" />
        </Tooltip>
      )}
    </>
  );
};

export default QrcodeForPreview;
