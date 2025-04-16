import { hasTaskRunning, newStartTrain } from '@/api/rasa';
import useLocale from '@/utils/useLocale';
import { Message } from '@arco-design/web-react';
import { useRequest, useSetState } from 'ahooks';
import { isEmpty } from 'lodash';
import { useCallback, useRef, useState } from 'react';
import i18n from './locale';

const POLLING_INTERVAL = 3000;
export default function useRunTask() {
  const t = useLocale(i18n);
  const [state, setState] = useSetState({
    isRunning: false,
    componentIds: [],
  });
  const [startLoading, setStartLoading] = useState(false);
  // 统计开始触发次数，可用于start的回调
  const [seed, setSeed] = useState(0);
  const triggerRunRef = useRef(false);

  const start = useCallback(
    (componentIds: string[], projectId: string) => {
      setStartLoading(true);
      newStartTrain({ projectId, componentIds })
        .then((data) => {
          if (isEmpty(data?.task)) {
            Message.info(t['project.hooks.useRunTask.readyTask']);
            setStartLoading(false);
            setSeed((s) => s + 1);
          } else {
            setTimeout(() => {
              setStartLoading(false);
              setSeed((s) => s + 1);
            }, POLLING_INTERVAL);
          }
          triggerRunRef.current = true;
        })
        .catch(() => {
          setStartLoading(false);
        });
    },
    [t]
  );

  useRequest(() => hasTaskRunning(), {
    pollingInterval: POLLING_INTERVAL,
    pollingWhenHidden: true,
    onSuccess: (data) => {
      if (isEmpty(data)) {
        setState({ isRunning: false, componentIds: [] });
        if (triggerRunRef.current) {
          setSeed(s => s + 1);
          triggerRunRef.current = false;
        }
      } else if (data[0].status === 2) {
        setState({
          isRunning: true,
          componentIds: data[0]?.properties.componentIds || [],
        });
      }
    },
  });

  return { ...state, start, startLoading, seed, setSeed };
}
