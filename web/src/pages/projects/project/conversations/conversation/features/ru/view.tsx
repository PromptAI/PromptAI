import useLocale from '@/utils/useLocale';
import { Button } from '@arco-design/web-react';
import { IconRedo, IconUndo } from '@arco-design/web-react/icon';
import React, { ReactNode, useEffect, useState } from 'react';
import ru, {
  CHANGE_STATE_EVENT,
  CHANGE_MANAGER_EVENT,
  initialRedoUndoState,
} from '.';
import { useGraphStore } from '../../store/graph';

const i18n = {
  'en-US': {
    redo: 'Redo',
    undo: 'Undo',
  },
  'zh-CN': {
    redo: '重做',
    undo: '撤销',
  },
};

interface WrapProps {
  type: 'redo' | 'undo';
  icon: ReactNode;
}
const Wrap: React.FC<WrapProps> = ({ type, icon }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  const [state, setState] = useState(initialRedoUndoState);
  useEffect(() => {
    ru.eventer.addListener({
      key: CHANGE_MANAGER_EVENT,
      callback: (manager) => {
        manager.eventer.addListener({
          key: CHANGE_STATE_EVENT,
          callback: (st) => setState(st),
        });
      },
    });
  }, []);
  return (
    <Button
      loading={state[`${type}Loading`]}
      type="text"
      size="small"
      icon={icon}
      disabled={state[`${type}Length`] === 0}
      onClick={() => ru[type]().then(refresh)}
    >
      {t[type]}
    </Button>
  );
};

export const Redo: React.FC = () => <Wrap icon={<IconRedo />} type="redo" />;

export const Undo: React.FC = () => <Wrap icon={<IconUndo />} type="undo" />;
