import { publish } from '@/api/projects';
import Move from '@/components/Move';
import RobotNext, { RoBotProps } from '@/components/RobotNext';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { IconRobot } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import React, { useEffect, useRef, useState } from 'react';
import useUrlParams from '../../hooks/useUrlParams';
import './index.css';
import i18n from './locale';

interface PublishRobotProps {
  disabled?: boolean;
}

const WIDTH = 430;
const DEFAULT_SESSION_PARAMS: RoBotProps['sessionParams'] = {
  scene: 'publish_snapshot',
};
const PublishRobot = ({ disabled }: PublishRobotProps) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const authtication = useRef<any>();
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const [height, setHeight] = useState(0);
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

  const handle = async () => {
    if (!disabled) {
      setLoading(true);
      try {
        if (!authtication.current) {
          const { id, token } = await publish(projectId);
          if (isEmpty(token)) {
            Message.warning(t['publish.robot.warning']);
            setVisible(false);
            setLoading(false);
            return;
          }
          authtication.current = {
            'X-published-project-id': id,
            'X-published-project-token': token,
            'X-project-id': projectId,
          };
        }
        setVisible(true);
      } catch (e) {
        Message.error(t['publish.robot.error']);
      }
      setLoading(false);
    }
  };
  return (
    <>
      <Move visible className="publish-robot-move" style={{ zIndex: 1000 }}>
        <div className="publish-robot-container-right">
          {!!height && (
            <RobotNext
              width={WIDTH}
              height={height}
              authtication={authtication.current}
              sessionParams={DEFAULT_SESSION_PARAMS}
              className={
                visible ? 'publish-robot-right' : 'publish-robot-hidden'
              }
              onClose={() => setVisible(false)}
            />
          )}
        </div>
      </Move>
      {!visible && (
        <Button
          loading={loading}
          className="publish-robot-trigger"
          type={disabled ? 'secondary' : 'primary'}
          disabled={disabled}
          icon={<IconRobot style={{ fontSize: 32 }} />}
          shape="circle"
          size="large"
          onClick={handle}
        />
      )}
    </>
  );
};

export default PublishRobot;
