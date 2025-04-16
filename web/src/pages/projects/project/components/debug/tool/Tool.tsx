import useLocale from '@/utils/useLocale';
import {
  Dropdown,
  Menu,
  Button,
  Message,
  Space,
  Tooltip,
  Spin,
} from '@arco-design/web-react';
import {
  IconCloseCircle,
  IconDown,
  IconLoading,
  IconPlayCircle,
  IconPlayCircleFill,
} from '@arco-design/web-react/icon';
import React, { useEffect, useMemo, useState } from 'react';
import i18n from './locale';
import { DebugAll, DebugCombination, DebugCurrent } from './triggers';
import store, {
  RUNNING_TASK_DEFAULT_STATE,
  RunningPublishRecordObserver,
} from '../listenerStore';
import { useRequest } from 'ahooks';
import { listConversations } from '@/api/components';
import useUrlParams from '../../../hooks/useUrlParams';
import { keyBy } from 'lodash';

interface ToolProps {
  current: string;
  disabledCurrent?: boolean;
  disabledAll?: boolean;
  disabledCombination?: boolean;
}
const Tool = ({
  current,
  disabledAll,
  disabledCombination,
  disabledCurrent,
}: ToolProps) => {
  const t = useLocale(i18n);
  const [state, setState] = useState(RUNNING_TASK_DEFAULT_STATE);
  const { projectId } = useUrlParams();

  const { loading: flowsLoading, data } = useRequest(() =>
    listConversations(projectId)
  );

  const flowMap = useMemo(() => (data ? keyBy(data, 'id') : {}), [data]);

  useEffect(() => {
    const observer = new RunningPublishRecordObserver();
    observer.on('state_change', (st: any) => setState(st));
    observer.on('ready_model', () => Message.info(t['sample.run.readyTask']));
    const listener = store.getInstance('debug');
    listener.addObserver(observer);
    listener.subscription();
    return () => store.removeInstance('debug');
  }, [t]);
  const isRunCurrent = useMemo(
    () => !current || state.componentIds.includes(current),
    [current, state.componentIds]
  );
  const runFlowName = useMemo(
    () =>
      (flowMap[state.componentIds[0]]?.data?.name || 'FAQ') +
      `${state.componentIds.length > 1 ? '...' : ''}`,
    [flowMap, state.componentIds]
  );
  return (
    <Space>
      <Spin loading={flowsLoading}>
        <Dropdown
          disabled={!!state.componentIds.length}
          unmountOnExit={false}
          droplist={
            <Menu>
              {!disabledCurrent && (
                <Menu.Item key="curren">
                  <DebugCurrent
                    start={(components, pid) =>
                      store.getInstance('debug').triggerRun(components, pid)
                    }
                    current={current}
                    icon={<IconPlayCircle />}
                    title={t['sample.run.current']}
                  />
                </Menu.Item>
              )}
              {!disabledAll && (
                <Menu.Item key="all">
                  <DebugAll
                    start={(components, pid) =>
                      store.getInstance('debug').triggerRun(components, pid)
                    }
                    current={current}
                    icon={<IconPlayCircle />}
                    title={t['sample.run.all']}
                  />
                </Menu.Item>
              )}
              {!disabledCombination && (
                <Menu.Item key="select">
                  <DebugCombination
                    title={t['sample.run.combination']}
                    current={current}
                    icon={<IconPlayCircle />}
                    start={(components, pid) =>
                      store.getInstance('debug').triggerRun(components, pid)
                    }
                  />
                </Menu.Item>
              )}
            </Menu>
          }
          trigger="click"
        >
          <Button
            type="text"
            icon={state.operating ? <IconLoading /> : <IconPlayCircleFill />}
            loading={state.running}
            status="success"
            size="small"
          >
            <Tooltip
              popupVisible={!!state.componentIds.length}
              content={
                isRunCurrent
                  ? t['tools.debug.run.running']
                  : `${runFlowName} ${t['tools.debug.run.running']}`
              }
            >
              <span>
                {t['sample.run']} <IconDown />
              </span>
            </Tooltip>
          </Button>
        </Dropdown>
      </Spin>
      {state.running && (
        <Button
          type="text"
          status="warning"
          size="small"
          icon={<IconCloseCircle />}
          onClick={() => store.getInstance('debug').triggerStop()}
          loading={state.canceling}
        >
          {t['sample.cancel']}
        </Button>
      )}
    </Space>
  );
};

export default Tool;
