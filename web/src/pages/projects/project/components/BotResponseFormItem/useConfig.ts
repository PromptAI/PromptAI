import { isBlank } from '@/utils/is';
import useLocale from '@/utils/useLocale';
import { useMemo } from 'react';
import Action from './Action';
import Attachment from './Attachment';
import Image from './Image';
import i18n from './locale';
import Text from './Text';
import { BotResponseConfigParams } from './types';
import Webhook from './Webhook';

const defaultConfig: BotResponseConfigParams = {
  text: true,
  image: false,
  webhook: true,
  attachment: false,
  action: false,
};
export default function useConfig(
  params?: Partial<BotResponseConfigParams>,
  isFaq?: boolean
) {
  const t = useLocale(i18n);
  return useMemo(() => {
    const { text, image, webhook, attachment, action } = {
      ...defaultConfig,
      ...params,
    };
    const textMaxRule = {
      validator: (value, callback) => {
        if (value?.content?.text.length > 512) {
          callback(t['conversation.botForm.rule.text.max']);
        }
      },
    };
    return {
      text: text && {
        rules: [
          {
            validator: (value, callback) => {
              if (isBlank(value?.content?.text || ''))
                callback(t['conversation.botForm.rule.text']);
            },
          },
          ...(isFaq ? [] : [textMaxRule]),
          {
            validator: (value, callback) => {
              if (!value?.delay || value?.delay < 0 || value?.delay > 5000) {
                callback(t['conversation.botForm.rule.delay']);
              }
            },
          },
        ],
        component: Text,
      },
      image: image && {
        rules: [
          {
            validator: (value, callback) => {
              if (isBlank(value?.content?.text || ''))
                callback(t['conversation.botForm.rule.image.text']);
              if (value?.content?.image?.some((i) => isBlank(i.url)))
                callback(t['conversation.botForm.rule.image']);
            },
          },
          ...(isFaq ? [] : [textMaxRule]),
          {
            validator: (value, callback) => {
              if (!value?.delay || value?.delay < 0 || value?.delay > 5000) {
                callback(t['conversation.botForm.rule.delay']);
              }
            },
          },
        ],
        component: Image,
      },
      webhook: webhook && {
        rules: [
          {
            validator: (value, callback) => {
              if (isBlank(value?.content?.text || ''))
                callback(t['conversation.botForm.rule.webhook']);
            },
          },
          ...(isFaq ? [] : [textMaxRule]),
        ],
        component: Webhook,
      },
      attachment: attachment && {
        rules: [
          {
            validator: (value, callback) => {
              if (isBlank(value?.content?.text || ''))
                callback(t['conversation.botForm.rule.attachment']);
            },
          },
          ...(isFaq ? [] : [textMaxRule]),
          {
            validator: (value, callback) => {
              if (!value?.delay || value?.delay < 0 || value?.delay > 5000) {
                callback(t['conversation.botForm.rule.delay']);
              }
            },
          },
        ],
        component: Attachment,
      },
      action: action && {
        rules: [
          {
            validator: (value, callback) => {
              if (isBlank(value?.content?.text || ''))
                callback(t['conversation.botForm.rule.action.text']);
            },
          },
          {
            validator: (value, callback) => {
              if (isBlank(value?.content?.code || ''))
                callback(t['conversation.botForm.rule.action.code']);
            },
          },
        ],
        component: Action,
      },
    };
  }, [t, params, isFaq]);
}
