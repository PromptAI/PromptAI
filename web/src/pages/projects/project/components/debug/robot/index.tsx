import { BackendStepProgress } from './StepProgress';
import RobotNext, { RoBotProps } from '@/components/RobotNext';
import {
  Button,
  Steps,
  Tag,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import { IconClose, IconLoading, IconRobot } from '@arco-design/web-react/icon';
import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { useDebugState } from './hook';

import './index.css';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import Move from '@/components/Move';
import store, { RunningPublishRecordObserver } from '../listenerStore';

interface DebugRobotProps {
  current?: string;
  projectId: string;
  componentIds?: string[];
}
const WIDTH = 430;

// const DEFAULT_REDEADY_DATA_TIME = 10 * 1000;
// const DEFAULT_DEPLOY_DATA_TIME = 30 * 1000;
const DebugRobot = ({ current, projectId, componentIds }: DebugRobotProps) => {
  const t = useLocale(i18n);
  const [height, setHeight] = useState(0);

  const cancelTask = useCallback(
    (taskId: string) => store.getInstance('debug').triggerStop(taskId),
    []
  );

  const cancelRecord = useCallback(
      (recordId: string) => store.getInstance('debug').triggerStop(recordId),
      []
  );
  useEffect(() => {
    setHeight(Math.max((window.innerHeight - 80) * 0.74, 440));
    function resize() {
      setHeight(Math.max((window.innerHeight - 80) * 0.74, 440));
    }
    window.addEventListener('resize', resize);
    return () => {
      window.removeEventListener('resize', resize);
    };
  }, []);

  const { recordId, checkLoading, open, close, cancel, readyData } =
    useDebugState(current, projectId, componentIds);

  const [cancelLoading, setCancelLoading] = useState(false);
  useEffect(() => {
    const observer = new RunningPublishRecordObserver();
    observer.on('create_model', () => open().then(() => setDisplay(true)));
    observer.on('ready_model', () => open().then(() => setDisplay(true)));
    observer.on('cancel_task', () => cancel());
    observer.on('cancel_state_change', (value: any) => setCancelLoading(value));
    const listener = store.getInstance('debug');
    listener.addObserver(observer);
    listener.subscription();
    return () => store.removeInstance('debug');
  }, [cancel, open]);

  const sessionParams = useMemo<RoBotProps['sessionParams']>(
    () => ({ componentId: current, scene: 'debug' }),
    [current]
  );
  const [display, setDisplay] = useState(false);

  const handleClose = () => {
    close();
    setDisplay(false);
  };
  const handleOpen = () => {
    open().then(() => setDisplay(true));
  };
  const mask = useMemo(() => {
    if (readyData.status ==="cancel") {
      return (
        <div className="debug-robot-mask">
          <Tag color="orange" style={{ marginBottom: 14, fontSize: 18 }}>
            {t['debug.robot.canceled']}
          </Tag>
        </div>
      );
    }
    if (readyData.status === 'failed') {
      return (
        <div className="debug-robot-mask">
          <Tag color="red" style={{ marginBottom: 14, fontSize: 18 }}>
            {t['debug.robot.train.error']}
          </Tag>
          <div className="debug-robot-error">
            <Tooltip content={readyData.message} style={{ zIndex: 1001 }}>
              <Typography.Text copyable ellipsis type="error">
                {readyData.message}
              </Typography.Text>
            </Tooltip>
          </div>
        </div>
      );
    }
    if (readyData.status === 'running') {
      return (
          <div className="debug-robot-mask">

            <div className="debug-robot-mask-title">
              {readyData.message}
            </div>

            {recordId && (
                <Button
                    type="primary"
                    icon={<IconClose/>}
                    status="warning"
                    onClick={() => cancelTask(recordId)}
                    loading={cancelLoading}
                    style={{marginTop: 32}}
                >
                  {t['debug.robot.cancel']}
                </Button>
            )}
          </div>
      );
    }
    return null;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    recordId,
    cancelLoading,
    readyData.status,
    readyData.message,
    t,
  ]);
  return (
    <>
      <Move visible className="debug-robot-move" style={{ zIndex: 1000 }}>
        <div className="debug-robot-container-right">
          {!!height && (
            <RobotNext
              width={WIDTH}
              height={height}
              authtication={readyData.authentication}
              sessionParams={sessionParams}
              className={display ? 'debug-robot-right' : 'debug-robot-hidden'}
              onClose={handleClose}
              disabled={readyData.disabled}
              mask={mask}
            />
          )}
        </div>
      </Move>
      <Button
        className="debug-robot-trigger"
        shape="circle"
        size="large"
        status={display ? 'success' : 'default'}
        type="primary"
        icon={<IconRobot fontSize={32} />}
        loading={checkLoading}
        onClick={display ? handleClose : handleOpen}
      />
    </>
  );
};

export default DebugRobot;
