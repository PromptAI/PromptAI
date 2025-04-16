import { BotResponse, BotResponseBaseContent } from '@/graph-next/type';
import { decodeAttachmentText } from '@/utils/attachment';
import useLocale from '@/utils/useLocale';
import { Card, InputNumber, Space } from '@arco-design/web-react';
import {
  IconClockCircle,
  IconFilePdf,
  IconImage,
} from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import i18n from './locale';

const FileItemIconMap = {
  png: <IconImage />,
  avg: <IconImage />,
  jpg: <IconImage />,
  jpeg: <IconImage />,
  gif: <IconImage />,
  webp: <IconImage />,
};
interface FileItemProps {
  name: string;
  href: string;
  type: string;
  version: string;
}
const FileItem = React.memo(({ name, href, type }: FileItemProps) => {
  return (
    <div className="arco-upload-list-item-text">
      <div className="arco-upload-list-item-text-content">
        <div className="arco-upload-list-item-text-name">
          <span className="arco-upload-list-file-icon">
            {FileItemIconMap[type] || <IconFilePdf />}
          </span>
          <a
            href={href}
            target="_blank"
            rel="noreferrer"
            className="arco-upload-list-item-text-name-link"
          >
            {name}
          </a>
        </div>
        <span className="arco-upload-list-success-icon">
          <svg
            fill="none"
            stroke="currentColor"
            strokeWidth="4"
            viewBox="0 0 48 48"
            aria-hidden="true"
            focusable="false"
            className="arco-icon arco-icon-check"
          >
            <path d="M41.678 11.05 19.05 33.678 6.322 20.95"></path>
          </svg>
        </span>
      </div>
    </div>
  );
});
interface AttachmentProps {
  value?: BotResponse<BotResponseBaseContent>;
  onChange?: (value: BotResponse<BotResponseBaseContent>) => void;
  disabled?: boolean;
}
const Attachment = ({ value, onChange, disabled }: AttachmentProps) => {
  const t = useLocale(i18n);
  const handleDelayChange = (delay) => {
    onChange({ ...value, delay });
  };
  const itemProps = useMemo(
    () => decodeAttachmentText(value?.content.text),
    [value]
  );
  return (
    <Card
      size="small"
      bodyStyle={{ padding: 2 }}
      headerStyle={{ padding: 2, height: 'max-content' }}
    >
      <Space direction="vertical" className="w-full">
        <FileItem {...itemProps} />
        <InputNumber
          prefix={
            <Space size="small">
              <IconClockCircle />
              <span>{`${t['conversation.botForm.delay.prefix']}:`}</span>
            </Space>
          }
          value={value.delay}
          onChange={handleDelayChange}
          disabled={disabled}
          suffix={
            <div className="px-2">{t['conversation.botForm.delay.suffix']}</div>
          }
          style={{ width: 260 }}
        />
      </Space>
    </Card>
  );
};

export default Attachment;
