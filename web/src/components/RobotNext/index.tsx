import { createChat, sendMessage, UPLOAD_INPUT_FILE } from '@/api/rasa';
import {
  Button,
  Card,
  Image,
  Message as ArcoMessage,
  Message as message,
  Progress,
  Space,
  Spin,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import {
  IconCheck,
  IconFile,
  IconFilePdf,
  IconLoading,
  IconMinusCircle,
  IconMore,
  IconRobot,
  IconSend,
  IconShareExternal,
  IconSync,
} from '@arco-design/web-react/icon';
import { nanoid } from 'nanoid';
import React, {
  Fragment,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
} from 'react';

import './index.css';
import { ArrayHelper } from './util';
import type {
  CurrentPage,
  DelayMessageItem,
  Message,
  MessageItem,
  RobotButtonsProps,
  RoBotProps,
  SimilarQuestion,
} from './types';
import moment from 'moment';
import { isEmpty } from 'lodash';
import { useDeepCompareEffect } from 'ahooks';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { decodeAttachmentText, isAttachment } from '@/utils/attachment';
import axios from 'axios';

function handleAttachment(content: MessageItem[]): MessageItem[] {
  return content.map((c) => {
    const { text, ...other } = c;
    return isAttachment(text)
      ? { ...other, attachment: decodeAttachmentText(text) }
      : c;
  });
}

function pageInitialButtonMessages(initialMessages, pageSize) {
  const map = new Map<number, Message>();
  if (pageSize === 0) return map;
  const items = initialMessages
    .map((i) => i.buttons || [])
    .reduce((p, c) => [...p, ...c], [])
    .sort((a, b) => b.order - a.order);
  const total = items.length;
  let temp = 0;
  while (temp * pageSize < total) {
    map.set(temp, {
      id: nanoid(),
      content: [
        {
          buttons: items.slice(temp * pageSize, pageSize * (temp + 1)),
        },
      ],
      avatar: '/robot.png',
      position: 'left',
      time: moment.now(),
    });
    temp = temp + 1;
  }
  return map;
}

function transformPrimaryButtonsMap(buttons) {
  if (!buttons) return {};
  return buttons.reduce((p, c) => ({ ...p, ['/' + c.id]: c.hidden }), {});
}

async function createSession(authtication, params) {
  return new Promise<{
    sessionId: string;
    initialMessages: MessageItem[];
    pageSize: number;
    locale: 'zh' | 'en';
    type: string;
  }>((resolve, reject) => {
    createChat(authtication, params)
      .then((chat) => {
        const primaryButtonsMap = transformPrimaryButtonsMap(
          chat.properties?.buttons
        );
        // let pageSize = chat.properties?.showSubNodesCount ?? Infinity; //隐藏分页
        let pageSize = Infinity;
        sendMessage(
          {
            message: '/init',
            chatId: chat.id,
            scene: params.scene,
            content: '/init',
          },
          authtication
        )
          .then(({ answers: messages, type: type }) => {
            let initialMessages = (
              isEmpty(messages) ? [{ text: chat.welcome }] : messages
            ).filter((m) => !m.custom && !m.rasaRes);
            pageSize = Math.min(
              initialMessages
                .filter((i) => !!i.buttons)
                .map((i) => i.buttons.length)
                .reduce((p, c) => p + c, 0),
              pageSize
            );
            // handle primary buttons
            let isHand = false;
            initialMessages = initialMessages.map((m) => {
              if (m.buttons) {
                if (!isHand) {
                  let primaried = 0;
                  isHand = true;
                  return {
                    ...m,
                    buttons: m.buttons.map((b) => {
                      let primary = primaryButtonsMap[b.payload];
                      if (primary) {
                        primaried++;
                        primary = primaried <= pageSize;
                      }
                      return {
                        ...b,
                        order: Number(primary),
                      };
                    }),
                  };
                }
                return { ...m, buttons: undefined };
              }
              return m;
            });
            resolve({
              sessionId: chat.id,
              initialMessages,
              pageSize,
              locale: chat.properties.locale || 'zh',
              type: type,
            });
          })
          .catch((e) => reject(e));
      })
      .catch((e) => reject(e));
  });
}

const getMinDistanceRightMessage = (messages: any[], start: number) => {
  const clone = messages.slice(0, start).reverse();
  for (let index = 0; index < clone.length; index++) {
    const element = clone[index];
    if (element.position === 'right') {
      return element;
    }
  }
  return null;
};

const RobotButtons = ({
  buttons,
  mask,
  onClick,
  currentPage,
  setMessages,
}: RobotButtonsProps) => {
  const t = useLocale(i18n);
  const [more, setMore] = useState(false);
  const showMore = useMemo(
    () =>
      currentPage.pageMap.size > currentPage.current + 1 &&
      !buttons.some((b) => b.order === -1),
    [currentPage, buttons]
  );
  const handleMore = () => {
    const pageMesage = currentPage.pageMap.get(currentPage.current + 1);
    setMessages((msgs) =>
      ArrayHelper.add(msgs, { ...pageMesage, time: moment.now() })
    );
    setMore(true);
    currentPage.current++;
  };
  return (
    <div className="x-robot-bcc-mb">
      <div className="x-robot-bcc-mb-primary">
        {buttons.map(({ payload, title }, inx) => (
          <div key={payload + inx + 'primary'}>
            <Button
              type="outline"
              onClick={() => !mask && onClick(payload, title)}
              style={{ maxWidth: 140 }}
            >
              <Typography.Text
                ellipsis={{ showTooltip: true }}
                type="primary"
                style={{ margin: 0 }}
              >
                {title}
              </Typography.Text>
            </Button>
          </div>
        ))}
      </div>
      {!more && showMore && (
        <div className="x-robot-bcc-mb-more">
          <Tooltip
            content={t['robotNext.more.action']}
            style={{ zIndex: 1001 }}
          >
            <Button type="text" onClick={handleMore}>
              <IconMore fontSize={20} />
            </Button>
          </Tooltip>
        </div>
      )}
    </div>
  );
};
const AttachmentView = ({ name, href }) => {
  return (
    <div className="x-robot-bcc-ma">
      <IconFilePdf />
      <a href={href} download={name}>
        {name}
      </a>
    </div>
  );
};
class BackendException extends Error {
  data: any;
  constructor(data: any, message?: string) {
    super(message);
    this.data = data;
  }
}
const useInputMode = (key: string) => {
  const [mode, setMode] = useState<'input' | 'multiple-input'>('input');

  const getInputValues = useCallback(() => {
    let values: any = window.localStorage.getItem(key);
    values = values ? JSON.parse(values) : [];
    return values;
  }, [key]);

  const send = useCallback(
    (params) => {
      const values = getInputValues();
      values.push(params);
      window.localStorage.setItem(key, JSON.stringify(values));
    },
    [key, getInputValues]
  );

  const clear = useCallback(() => {
    window.localStorage.removeItem(key);
  }, [key]);

  return {
    mode,
    setMode,
    send,
    clear,
    getInputValues,
  };
};
type OnSendParams = {
  payload: string;
  content?: string;
  onSendSuccess?: () => void;
  pushUserMessage?: boolean;
};
const RobotNext = (
  {
    width,
    height,
    authtication,
    sessionParams,
    mask,
    onAfterSendMessage,
    className,
    maskClassName,
    onClose,
    disabled,
  }: RoBotProps,
  ref
) => {
  const t = useLocale(i18n);
  const inputRef = useRef<HTMLInputElement>();
  const cmRef = useRef<HTMLDivElement>();

  const sessionIdRef = useRef<string>();
  const [sessionLoading, setSessionLoading] = useState(false);

  const [messages, setMessages] = useState<Message[]>([]);
  const [sendLoading, setSendLoading] = useState(false);

  const onSend = useCallback(
    ({
      payload,
      content,
      onSendSuccess,
      pushUserMessage = true,
    }: OnSendParams) => {
      const text = payload?.trim();
      if (text && text !== '' && sessionIdRef.current && !sendLoading) {
        setSendLoading(true);
        const msg = text.trim();
        sendMessage(
          {
            message: msg,
            content: content || msg,
            chatId: sessionIdRef.current,
            scene: sessionParams.scene,
          },
          authtication
        )
          .then((data) => {
            const {
              answers: res,
              properties,
              similarQuestions,
              links,
              type,
            } = data;
            if (pushUserMessage) {
              setMessages((vals) =>
                ArrayHelper.add(vals, {
                  id: nanoid(),
                  content: [{ text: content || text }],
                  avatar: '/user.png',
                  position: 'right',
                  time: Number(properties?.queryTime || moment.now()),
                })
              );
            }
            onSendSuccess?.();

            const end = properties?.answerTime
              ? moment(Number(properties.answerTime))
              : moment();
            let contents: DelayMessageItem[] = [
              {
                content: [{ text: t['robotNext.changeQuestion'] }],
                delay: 0,
                time: end.valueOf(),
              },
            ];

            // 现在，允许answer回来是空数组
            if (res.length == 0) {
                return;
            }

            if (res?.length) {
              const adpterRes = res
                .filter((c) => !c.rasaRes)
                .map((c, i) => {
                  if (c.custom) return null; // 不处理custom
                  let ac = c;
                  if (res[i + 1] && res[i + 1].custom) {
                    ac = {
                      ...c,
                      ...res[i + 1].custom,
                      buttons: c.buttons?.map((b) => ({ ...b, order: -1 })),
                    };
                  }
                  return ac;
                })
                .filter((v) => !!v);
              contents = adpterRes.map((rs) => {
                return {
                  content: [rs],
                  delay: Number(rs.delay || 0),
                  time: end
                    .add('milliseconds', Number(rs.delay || 0))
                    .valueOf(),
                };
              });
            }
            if (type === 'error') {
              contents = contents.map((c) => ({
                ...c,
                content: c.content.map((o) => ({
                  ...o,
                  text: t['robot.backend.error'],
                })),
              }));
            }
            // handle attachment
            contents = contents.map((c) => ({
              ...c,
              content: handleAttachment(c.content),
            }));
            contents
              .reduce((p, c) => {
                return p.then(() => {
                  return new Promise<void>((resolve) => {
                    const bId = nanoid();
                    setMessages((vals) =>
                      ArrayHelper.add(vals, {
                        id: bId,
                        content: [],
                        avatar: '/robot.png',
                        position: 'left',
                        loading: true,
                        time: null,
                      })
                    );
                    setTimeout(() => {
                      // do upate bot message
                      setMessages((vals) =>
                        ArrayHelper.update(
                          vals,
                          { content: c.content, loading: false, time: c.time },
                          (m) => m.id === bId
                        )
                      );
                      resolve();
                    }, c.delay);
                  });
                });
              }, Promise.resolve())
              .then(() => {
                similarQuestions?.length > 0 &&
                  setMessages((vals) =>
                    ArrayHelper.add(vals, {
                      id: nanoid(),
                      content: [{ text: 'extra' }],
                      avatar: '/robot.png',
                      position: 'left',
                      time: Number(properties?.queryTime || moment.now()),
                      extra: {
                        similarQuestions,
                      },
                    })
                  );
                links?.length > 0 &&
                  setMessages((vals) =>
                    ArrayHelper.add(vals, {
                      id: nanoid(),
                      content: [{ text: 'extra' }],
                      avatar: '/robot.png',
                      position: 'left',
                      time: Number(properties?.queryTime || moment.now()),
                      extra: {
                        links,
                      },
                    })
                  );
              });
            if (type === 'error') {
              throw new BackendException(
                data,
                res?.[0]?.text || 'Unknown Backend Error'
              );
            }
          })
          .catch((e) => {
            // 这没生效。
            if (e instanceof BackendException) {
              setMessages((vals) =>
                ArrayHelper.add(vals, {
                  id: nanoid(),
                  content: [{ text: t['robot.backend.error'] }],
                  avatar: '/robot.png',
                  position: 'left',
                  time: Number(e.data.properties?.queryTime || moment.now()),
                  extra: {
                    error: t['robot.backend.error'],
                  },
                })
              );
              return;
            }
            setMessages((vals) =>
              ArrayHelper.add(vals, {
                id: nanoid(),
                content: [{ text: t['robot.network.error'] }],
                avatar: '/robot.png',
                position: 'left',
                time: Date.now(),
              })
            );
          })
          .finally(() => setSendLoading(false));
        onAfterSendMessage?.(text);
      }
    },
    [authtication, onAfterSendMessage, sendLoading, t, sessionParams]
  );

  useEffect(() => {
    // 最新消息至底
    setTimeout(() => {
      cmRef.current.scrollTop = cmRef.current.scrollHeight;
    }, 0);
  }, [messages]);

  const currentPage = useRef<CurrentPage>({ current: 0, pageMap: new Map() });

  const { mode, send, setMode, clear, getInputValues } = useInputMode(
    'multiple-input-value'
  );
  const startSession = useCallback(
    (sessionParams, authtication) => {
      if (sessionParams && authtication) {
        setSessionLoading(true);
        createSession(authtication, sessionParams)
          .then(({ sessionId, initialMessages, pageSize, locale, type }) => {
          setMode('input');
          clear();
          sessionIdRef.current = sessionId;
          // page initial message
          currentPage.current = {
            current: 0,
            pageMap: pageInitialButtonMessages(initialMessages, pageSize),
          };

          let initialPageMessages: Message[];
          if (type === 'error') {
            initialPageMessages =
              initialMessages?.map((msg) => ({
                id: nanoid(),
                content: [
                  {
                    ...msg,
                    text: t['robot.backend.error'],
                  },
                ],
                avatar: '/robot.png',
                position: 'left',
                time: moment.now(),
              })) || [];
          } else {
            initialPageMessages =
              initialMessages?.map((msg) => ({
                id: nanoid(),
                content: msg.buttons
                  ? [
                      {
                        ...msg,
                        buttons: currentPage.current.pageMap
                          .get(0)
                          ?.content.map((c) => c.buttons)
                          .reduce((p, c) => [...p, ...c], []),
                      },
                    ]
                  : [msg],
                avatar: '/robot.png',
                position: 'left',
                time: moment.now(),
              })) || [];
          }
          // handle attachment
          initialPageMessages = initialPageMessages.map((m) => ({
            ...m,
            content: handleAttachment(m.content),
          }));
          setMessages(initialPageMessages);
        })
        .catch((e) => {
          console.error(e);
          ArcoMessage.error(t['robotNext.createFailure']);
        })
        .finally(() => setSessionLoading(false));
      }
    },
    [t, setMode, clear]
  );
  useImperativeHandle(ref, () => ({
    startSession,
  }));
  useDeepCompareEffect(() => {
    startSession(sessionParams, authtication);
  }, [sessionParams, authtication]);

  const onInputSend = () => {
    if (mode === 'multiple-input') {
      const text = inputRef.current.value;
      send({ payload: text });
      setMessages((vals) =>
        ArrayHelper.add(vals, {
          id: nanoid(),
          content: [{ text }],
          avatar: '/user.png',
          position: 'right',
          time: Date.now(),
        })
      );
      inputRef.current.value = '';
      return;
    }
    onSend({
      payload: inputRef.current.value,
      onSendSuccess: () => (inputRef.current.value = ''),
    });
  };
  const onEnterDownSend = (evt) => {
    if (evt.keyCode === 13) {
      if (mode === 'multiple-input') {
        const text = inputRef.current.value;
        send({ payload: text });
        setMessages((vals) =>
          ArrayHelper.add(vals, {
            id: nanoid(),
            content: [{ text }],
            avatar: '/user.png',
            position: 'right',
            time: Date.now(),
          })
        );
        inputRef.current.value = '';
        return;
      }
      onSend({
        payload: inputRef.current.value,
        onSendSuccess: () => (inputRef.current.value = ''),
      });
    }
  };
  const onClickSend = (payload: string, title: string) => {
    if (mode === 'multiple-input') {
      return;
    }
    onSend({ payload, content: title });
  };
  useEffect(() => {
    const last = [...messages].pop();
    if (last && last.content.some((c) => c.multiInput)) {
      // trigger multiInput mode
      setMode('multiple-input');
    }
  }, [messages, setMode, clear]);

  const submitCacheMultipleInput = useCallback(() => {
    const values = getInputValues();
    const payload = values.map((v) => v.payload).join('\n');
    onSend({
      payload,
      pushUserMessage: false,
      onSendSuccess: () => {
        clear();
        setMode('input');
      },
    });
  }, [getInputValues, onSend, clear, setMode]);
  return (
    <Card
      bordered
      className={className}
      title={
        <div className="flex items-center gap-2 move-pointer">
          <Button
            type="primary"
            shape="circle"
            icon={<IconRobot fontSize={19} />}
          />
          <Typography.Text bold>{t['robotNext.debugging']}</Typography.Text>
        </div>
      }
      extra={
        <Space className="move-pointer">
          {!disabled && !mask && (
            <Button
              type="text"
              icon={<IconSync />}
              onClick={() => startSession(sessionParams, authtication)}
            >
              {t['robotNext.restart']}
            </Button>
          )}
          <Button
            type="text"
            status="warning"
            icon={<IconMinusCircle />}
            onClick={onClose}
          >
            {t['robotNext.minimize']}
          </Button>
        </Space>
      }
      headerStyle={{ borderBottom: '1px solid var(--color-border-2)' }}
    >
      <section style={{ width, height }} className="x-robot">
        <div className="x-robot-m" ref={cmRef}>
          {messages.map(
            (
              { id, content, position, avatar, loading, time, extra },
              msgIndex
            ) => (
              <Fragment key={id}>
                {position === 'right' && (
                  <div className="x-robot-u">
                    <div className="x-robot-uc">
                      <span>
                        {time
                          ? moment(Number(time)).format('YYYY-MM-DD HH:mm:ss')
                          : null}
                      </span>
                      <div className="x-robot-ucc">
                        <div style={{ maxWidth: '100%' }}>
                          {content?.map(({ text, upload }, index) =>
                            upload ? (
                              <ChatUpload
                                t={t}
                                messageId={id}
                                key={index}
                                upload={upload}
                                setMessages={setMessages}
                                authtication={authtication}
                                sessionIdRef={sessionIdRef}
                              />
                            ) : (
                              text
                            )
                          ) || '-'}
                        </div>
                      </div>
                    </div>
                    <img src={avatar} alt="U" className="x-robot-a" />
                  </div>
                )}
                {position === 'left' && (
                  <div className="x-robot-b">
                    {!extra && (
                      <img src={avatar} alt="B" className="x-robot-a" />
                    )}
                    <div className="x-robot-bc">
                      {!extra && (
                        <span style={{ paddingLeft: 10 }}>
                          {time
                            ? moment(Number(time)).format('YYYY-MM-DD HH:mm:ss')
                            : null}
                        </span>
                      )}
                      {!extra && (
                        <div className="x-robot-bcc">
                          {loading && <Spin size={20} />}
                          {content?.map(
                            ({ text, image, buttons, attachment }, index) => (
                              <div key={index + text} className="x-robot-bcc-m">
                                {text && (
                                  <span className="x-robot-bcc-mt">{text}</span>
                                )}
                                {image && (
                                  <div className="x-robot-bcc-mi">
                                    {image
                                      ?.split(',')
                                      .filter((i) => i !== '')
                                      .map((i, ind) => (
                                        <Image
                                          width="100%"
                                          height="100%"
                                          key={i + ind}
                                          src={i}
                                          alt="bg"
                                          className="x-robot-bcc-mi-img"
                                        />
                                      ))}
                                  </div>
                                )}
                                {attachment && (
                                  <AttachmentView {...attachment} />
                                )}
                                {buttons && (
                                  <RobotButtons
                                    buttons={buttons}
                                    mask={mask}
                                    onClick={onClickSend}
                                    currentPage={currentPage.current}
                                    setMessages={setMessages}
                                  />
                                )}
                              </div>
                            )
                          )}
                        </div>
                      )}

                      {extra?.similarQuestions &&
                        extra.similarQuestions.length > 0 && (
                          <SimilarQuestions
                            similarQuestions={extra.similarQuestions}
                            onItemClick={(item) =>
                              onClickSend(item.id, item.query)
                            }
                          />
                        )}
                      {extra?.links && extra.links.length > 0 && (
                        <Links links={extra.links} />
                      )}
                      {extra?.error && (
                        <BackendError
                          payload={getMinDistanceRightMessage(
                            messages,
                            msgIndex
                          )}
                          onSend={onSend}
                        />
                      )}
                    </div>
                  </div>
                )}
              </Fragment>
            )
          )}
        </div>
        <div className="x-robot-i">
          <UploadInput setMessages={setMessages} />
          <input
            ref={inputRef}
            className="x-robot-ii"
            placeholder={t['robotNext.input.placeholder']}
            onKeyDown={onEnterDownSend}
          />
          <Button
            loading={sendLoading}
            type="primary"
            shape="circle"
            onClick={onInputSend}
            className="x-robot-s"
            icon={<IconSend />}
          />
          {mode === 'multiple-input' && (
            <Button
              type="primary"
              status="success"
              shape="circle"
              onClick={submitCacheMultipleInput}
              icon={<IconCheck />}
            />
          )}
        </div>
        {mask && (
          <div
            className={
              maskClassName ? `x-robot-mask ${maskClassName}` : 'x-robot-mask'
            }
            onClick={(evt) => {
              evt.stopPropagation();
            }}
          >
            {mask}
          </div>
        )}
      </section>

      {sessionLoading && (
        <div className="x-robot-mask" onClick={(evt) => evt.stopPropagation()}>
          <div className="x-robot-loading">
            <Typography.Text type="primary">
              <IconLoading /> {t['robotNext.creating']}
            </Typography.Text>
          </div>
        </div>
      )}
    </Card>
  );
};

const UploadInput = ({ setMessages }) => {
  const onInputChange = useCallback(
    (evt) => {
      const file = evt.target.files[0];
      setMessages((vals) =>
        ArrayHelper.add(vals, {
          id: nanoid(),
          content: [{ upload: { file, status: 'init' } }],
          avatar: '/robot.png',
          position: 'right',
          loading: false,
          time: Date.now(),
        })
      );
    },
    [setMessages]
  );
  return (
    <>
      <label htmlFor="upload_input_file">
        <span
          className="arco-btn arco-btn-secondary arco-btn-size-default arco-btn-shape-circle arco-btn-icon-only"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <IconShareExternal />
        </span>
      </label>
      <input
        id="upload_input_file"
        type="file"
        style={{ display: 'none' }}
        onChange={onInputChange}
      />
    </>
  );
};

const ChatUpload = ({
  upload,
  setMessages,
  authtication,
  sessionIdRef,
  messageId,
  t,
}) => {
  const [percent, setPercent] = useState(() =>
    upload.status === 'init' ? 0 : 100
  );
  const fetchUpload = useCallback(() => {
    const form = new FormData();
    form.append('file', upload.file);
    form.append('chatId', sessionIdRef.current as string);
    const headers = {};
    Object.entries(authtication).forEach(([k, v]) => {
      headers[k] = v;
    });
    axios({
      method: 'post',
      headers: {
        'Content-Type': 'multipart/form-data',
        ...headers,
      },
      url: UPLOAD_INPUT_FILE,
      data: form,
      onUploadProgress: (progressEvent) => {
        setPercent(
          Number(
            ((progressEvent.loaded / progressEvent.total) * 100).toFixed(1)
          )
        );
      },
    })
      .then(() => {
        setMessages((vals) =>
          ArrayHelper.update(
            vals,
            { content: [{ upload: { ...upload, status: 'success' } }] },
            (m: any) => m.id === messageId
          )
        );
      })
      .catch((err) => {
        message.error(err.data?.messsage || 'Upload input file error');
        setMessages((vals) =>
          ArrayHelper.update(
            vals,
            { content: [{ upload: { ...upload, status: 'error' } }] },
            (m: any) => m.id === messageId
          )
        );
      });
  }, [authtication, messageId, sessionIdRef, setMessages, upload]);
  useEffect(() => {
    if (upload.status === 'init') {
      fetchUpload();
    }
  }, [fetchUpload, upload.status]);
  const handleClick = useCallback(() => {
    setMessages((vals) =>
      ArrayHelper.add(vals, {
        id: nanoid(),
        content: [{ upload: { file: upload.file, status: 'init' } }],
        avatar: '/robot.png',
        position: 'right',
        loading: false,
        time: Date.now(),
      })
    );
  }, [setMessages, upload.file]);
  const preview = useMemo(() => {
    if (upload.file.type.startsWith('image')) {
      const pre = window.URL.createObjectURL(upload.file);
      return (
        <Image
          src={pre}
          width="100%"
          style={{ marginBottom: 4, background: 'white' }}
        />
      );
    }
    return <></>;
  }, [upload.file]);
  return (
    <>
      <div>
        {preview}
        <IconFile style={{ width: 16, height: 16 }} />
        <span> {(upload.file as File).name}</span>
        <span> ({t[`robotNext.upload.status.${upload.status}`]})</span>
      </div>
      <Progress
        percent={percent}
        width={'100%'}
        color={progressColor[upload.status]}
        showText={false}
      />
      {upload.status === 'error' && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            cursor: 'pointer',
            fontSize: 14,
          }}
        >
          <span onClick={handleClick}>
            {t['robotNext.upload.status.error.retry']}
            <IconSync />
          </span>
        </div>
      )}
    </>
  );
};
const progressColor = {
  success: '#4ade80',
  error: '#ef4444',
};

interface SimilarQuestionsProps
  extends Pick<Message['extra'], 'similarQuestions'> {
  onItemClick: (item: SimilarQuestion) => void;
}
const SimilarQuestions = ({
  similarQuestions,
  onItemClick,
}: SimilarQuestionsProps) => {
  const t = useLocale(i18n);
  return (
    <div className="x-robot-bcc-extra-similar">
      <span className="x-robot-bcc-extra-similar-title">
        {t['robotNext.relations']}:
      </span>
      <div className="x-robot-bcc-extra-similar-content">
        {similarQuestions.map((item, index) => (
          <a key={item.id} onClick={() => onItemClick(item)} title={item.query}>
            {index + 1}.{item.query}
          </a>
        ))}
      </div>
    </div>
  );
};
interface LinkProps {
  links: { url: string }[];
}
const Links = ({ links }: LinkProps) => {
  const t = useLocale(i18n);
  return (
    <div className="x-robot-bcc-extra-similar">
      <span className="x-robot-bcc-extra-similar-title">
        {t['robotNext.links']}:
      </span>
      <div className="x-robot-bcc-extra-similar-content">
        {links.map((item) => (
          <a key={item.url} href={item.url} target="_blank" rel="noreferrer">
            {item.url}
          </a>
        ))}
      </div>
    </div>
  );
};

const BackendError = ({ onSend, payload }) => {
  const t = useLocale(i18n);

  const onClick = () => {
    if (payload) {
      const { content } = payload;
      if (content[0].text) {
        onSend({
          payload: content[0].text,
          onSendSuccess: () => void 0,
        });
      }
    }
  };
  return (
    <div className="x-robot-bcc-extra-similar">
      <Button type="outline" size="default" onClick={onClick}>
        {t['robotNext.retry']}
      </Button>
    </div>
  );
};
export default React.memo(React.forwardRef(RobotNext));

export { RoBotProps };
