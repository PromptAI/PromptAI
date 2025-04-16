import { ObjectArrayHelper } from '@/graph-next/helper';
import { BotResponse, BotResponseImageContent } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import { Card, InputNumber, Space } from '@arco-design/web-react';
import { IconClockCircle } from '@arco-design/web-react/icon';
import { nanoid } from 'nanoid';
import React from 'react';
import i18n from './locale';
import EnterBlurInput from '@/pages/projects/project/components/EnterBlurInput';
import CropImageUpload from '@/pages/projects/project/components/CropImageUpload';

interface ImageProps {
  value: BotResponse<BotResponseImageContent>;
  onChange: (value: BotResponse<BotResponseImageContent>) => void;
  disabled?: boolean;
}

const Image = ({ value, onChange, disabled }: ImageProps) => {
  const t = useLocale(i18n);
  const handleImageChange = (key, v) => {
    onChange({
      ...value,
      content: {
        ...value.content,
        image: ObjectArrayHelper.update(
          value.content.image,
          { url: v },
          (f) => f.id === key
        ),
      },
    });
  };
  const handleDelImage = (key) => {
    if (value.content.image.length > 1) {
      onChange({
        ...value,
        content: {
          ...value.content,
          image: ObjectArrayHelper.del(
            value.content.image,
            (f) => f.id === key
          ),
        },
      });
    } else {
      onChange({
        ...value,
        content: {
          ...value.content,
          image: ObjectArrayHelper.update(
            value.content.image,
            { id: nanoid(), url: '' },
            (f) => f.id === key
          ),
        },
      });
    }
  };
  const handleTextChange = (text: string) => {
    onChange({
      ...value,
      content: {
        ...value.content,
        text,
      },
    });
  };
  const handleImageAdd = () => {
    onChange({
      ...value,
      content: {
        ...value.content,
        image: ObjectArrayHelper.add(value.content.image, {
          id: nanoid(),
          url: '',
        }),
      },
    });
  };
  const handleDelayChange = (delay) => {
    onChange({
      ...value,
      delay,
    });
  };
  return (
    <Card size="small" bodyStyle={{ padding: 2 }}>
      <Space direction="vertical" className="w-full">
        <EnterBlurInput
          value={value?.content?.text}
          placeholder={t['conversation.botForm.image.placeholder']}
          onChange={handleTextChange}
          autoFocus
          disabled={disabled}
        />
        {value?.content?.image?.map(({ id, url }) => (
          <CropImageUpload
            key={id}
            value={url}
            onChange={(v) => handleImageChange(id, v)}
            cropSize={{ width: 420, height: 260 }}
            onDel={() => handleDelImage(id)}
            onAdd={handleImageAdd}
            disabled={disabled}
          />
        ))}
        <InputNumber
          prefix={
            <Space size="small">
              <IconClockCircle />
              <span>{t['conversation.botForm.delay.prefix']}</span>
            </Space>
          }
          min={0}
          max={5000}
          value={value.delay}
          disabled={disabled}
          onChange={handleDelayChange}
          suffix={
            <div className="px-2">{t['conversation.botForm.delay.suffix']}</div>
          }
          style={{ width: 260 }}
        />
      </Space>
    </Card>
  );
};

export default Image;
