import request from './request';
import { decodeAttachmentText } from './attachment';
import { store } from './store';

async function createChat(headers, body) {
  return request('/chat/api/chat', {
    method: 'POST',
    headers,
    body
  });
}
export async function sendMessage(authentication, { payload, sessionId, content, scene }) {
  const result = request('/chat/api/message', {
    body: {
      message: payload,
      chatId: sessionId,
      content,
      scene
    },
    headers: authentication,
    method: 'POST'
  });
  if (result.error) {
    return Promise.reject(result.error);
  }

  return result;
}

function transformPrimaryButtonsMap(buttons) {
  if (!buttons) return {};
  return buttons.reduce((p, c) => ({ ...p, [c.id]: c.hidden }), {});
}
export async function createSession(
  authentication,
  { engine = 'rasa', welcome, scene = 'publish_snapshot' },
  injectVars
) {
  return createChat(authentication, { ...injectVars, scene }).then(({ data: chat, error }) => {
    if (error) return Promise.reject(error);
    const primaryButtonsMap = transformPrimaryButtonsMap(chat.properties?.buttons);
    let primaryCount = chat.properties?.showSubNodesCount ?? Infinity;
    // merge origin get
    const initialSettings = store.getState().settings;
    store.setState({ settings: { ...initialSettings, ...chat.properties.chatBotSettings } });
    switch (engine) {
      case 'chat':
        return {
          sessionId: chat.id,
          initialMessages: welcome
            ? [{ text: welcome }]
            : [{ text: chat.properties?.welcome || '' }],
          msgId: 'initialize-welcome',
          canEvaluate: false,
          locale: chat.properties.locale || 'zh'
        };
      default:
        return sendMessage(authentication, {
          payload: '/init',
          sessionId: chat.id,
          content: '/init',
          scene
        }).then(
          ({
            data: {
              answers: messages,
              id: msgId,
              properties: { canEvaluate }
            },
            error
          }) => {
            if (error) return Promise.reject(error);
            let initialMessages = (
              !messages || messages.length === 0 ? [{ text: chat.welcome }] : messages
            ).filter((m) => !m.custom || !m.rasaRes);
            primaryCount = Math.min(
              initialMessages
                .filter((i) => !!i.buttons)
                .map((i) => i.buttons.length)
                .reduce((p, c) => p + c, 0),
              primaryCount
            );
            // handle primary buttons
            initialMessages = initialMessages.map((m) => {
              if (m.buttons) {
                let primaried = 0;
                return {
                  ...m,
                  buttons: m.buttons.map((b) => {
                    let primary = !primaryButtonsMap[b.payload];
                    if (primary) {
                      primaried++;
                      primary = primaried <= primaryCount;
                    }
                    return {
                      ...b,
                      primary
                    };
                  })
                };
              }
              return m;
            });
            const result = {
              sessionId: chat.id,
              initialMessages: welcome ? [{ text: welcome }, ...initialMessages] : initialMessages,
              msgId,
              canEvaluate,
              locale: chat.properties.locale || 'zh'
            };

            return result;
          }
        );
    }
  });
}

export async function evaluateService(data, authentication) {
  const result = request('/chat/api/message/evaluate', {
    body: data,
    headers: authentication,
    method: 'PUT'
  });
  if (result.error) {
    return Promise.reject(result.error);
  }
  return result;
}

export async function uploadFile(file, authentication, chatId) {
  const form = new FormData();
  form.append('file', file);
  form.append('chatId', chatId);
  const response = await fetch('/chat/api/message/file', {
    headers: {
      ...authentication
    },
    method: 'POST',
    body: form
  });
  if (response.status >= 200 && response.status < 300) {
    const {
      dialog: {
        input: { send }
      }
    } = await response.json();
    const data = decodeAttachmentText(send);
    return data;
  }
  const error = new Error(response.statusText);
  error.response = response;
  throw error;
}
