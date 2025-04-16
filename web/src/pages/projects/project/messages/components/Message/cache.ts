import { fetchMessage } from '@/api/rasa';
import { decodeAttachmentText, isAttachment } from '@/utils/attachment';
import useLocale from '@/utils/useLocale';
import { useRequest } from 'ahooks';
import { nanoid } from 'nanoid';
import i18n from './locale';
import { useRef } from 'react';

function handleAttachment(content) {
  return content.map((c) => {
    const { text, ...other } = c;
    return isAttachment(text)
      ? { ...other, attachment: decodeAttachmentText(text) }
      : c;
  });
}
async function getMessage(msgId, cache, t) {
  return new Promise((resolve, reject) => {
    if (cache[msgId]) {
      resolve(cache[msgId]);
    } else {
      fetchMessage(msgId)
        .then((messages) => {
          const data = messages
            ?.map((c) => ({ ...c.dialog, evaluation: c.evaluation }))
            .map((item) => {
              let user,
                bot = null;
              if (item.input && item.input.send !== '/init') {
                user = {
                  avatar: '/user.png',
                  position: 'right',
                  content: [
                    {
                      text: item.input.send,
                      attachment: isAttachment(item.input.send)
                        ? decodeAttachmentText(item.input.send)
                        : '',
                    },
                  ],
                  id: nanoid(),
                  time: item.input.sendTime,
                };
              }
              if (item.output && item.output.answers) {
                let contents: any[] = [{ text: t['message.error.timeout'] }];
                if (item.output.type !== 'error') {
                  contents =
                    JSON.parse(item.output.answers).length > 0
                      ? JSON.parse(item.output.answers)
                          .map((p, i, array) => {
                            const a = {
                              ...p,
                              customTemp: array[i + 1]?.custom || {},
                            };
                            return a;
                          }, {})
                          .filter((c) => !c.custom)
                      : [{ text: t['message.default'] }];
                }
                bot = contents.map((content) => ({
                  avatar: '/robot.png',
                  position: 'left',
                  content: handleAttachment([content]),
                  id: nanoid(),
                  time: item.output.answerTime,
                  evaluation: item.evaluation,
                  custom: content.customTemp,
                }));
              }
              return {
                id: nanoid(),
                data: [...(user ? [user] : []), ...(bot || [])],
                similarQuestions: JSON.parse(item.output.similarQuestions),
                links: JSON.parse(item.output.links),
              };
            });
          cache[msgId] = data;
          resolve(cache[msgId]);
        })
        .catch((e) => reject(e));
    }
  });
}
export default function useCacheMessage(msgId) {
  const cache = useRef(new Map());
  const t = useLocale(i18n);
  const { loading, data } = useRequest(
    () => getMessage(msgId, cache.current, t),
    {
      refreshDeps: [msgId, t],
      debounceWait: 500,
    }
  );
  return [loading, data] as [boolean, any[]];
}
