import { BotResponse, BotResponseBaseContent } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import { Card, Input, InputNumber, Space } from '@arco-design/web-react';
import { IconClockCircle } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from './locale';

interface TextProps {
  value?: BotResponse<BotResponseBaseContent>;
  onChange?: (values: BotResponse<BotResponseBaseContent>) => void;
  placeholder?: string;
  disabled?: boolean;
}

const Text = ({ value, onChange, placeholder, disabled }: TextProps) => {
  const t = useLocale(i18n);
  const handleTextChange = (text: string) => {
    onChange({ ...value, content: { text } });
  };

  const handleDelayChange = (delay) => {
    onChange({
      ...value,
      delay,
    });
  };
  return (
    <Card
      size="small"
      bodyStyle={{ padding: 2 }}
      headerStyle={{ padding: 2, height: 'max-content' }}
    >
      <Space direction="vertical" className="w-full">
        <Input.TextArea
          autoSize
          value={value.content.text}
          onChange={handleTextChange}
          placeholder={
            placeholder || t['conversation.botForm.text.placeholder']
          }
          autoFocus
          disabled={disabled}
        />
        {/*<InputNumber*/}
        {/*  prefix={*/}
        {/*    <Space size="small">*/}
        {/*      <IconClockCircle />*/}
        {/*      <span>{`${t['conversation.botForm.delay.prefix']}:`}</span>*/}
        {/*    </Space>*/}
        {/*  }*/}
        {/*  value={value.delay}*/}
        {/*  onChange={handleDelayChange}*/}
        {/*  disabled={disabled}*/}
        {/*  suffix={*/}
        {/*    <div className="px-2">{t['conversation.botForm.delay.suffix']}</div>*/}
        {/*  }*/}
        {/*  style={{ width: 260 }}*/}
        {/*/>*/}
      </Space>
    </Card>
  );
};

export default Text;
