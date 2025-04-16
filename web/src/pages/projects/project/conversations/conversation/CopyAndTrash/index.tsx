import {
  Button,
  Empty,
  Message,
  Space,
  Spin,
  Tabs,
} from '@arco-design/web-react';
import React, { useCallback, useEffect, useState } from 'react';
import { FiClipboard } from 'react-icons/fi';
import { BsRecycle } from 'react-icons/bs';
import CopyContextProvider, { useCopy } from '../copy/context';
import {
  ContextProvider as TrashContextProvider,
  useTrash,
} from '../trash/context';
import { IconDelete, IconMinusCircle } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { default as CopyFrame } from '../copy/Frames/Frame';
import { default as TrashFrame } from '../trash/Frames/Frame';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { clearTrash } from '@/api/trash';
import ru, { RU_TYPE } from '../features/ru';

const TrashWrap = ({ loading, items, refreshTrash, description }) => {
  useEffect(() => {
    refreshTrash();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <Spin loading={loading} className="w-full">
      {items.map((item) => (
        <TrashFrame key={item.key} frame={item} refresh={refreshTrash} />
      ))}
      {!items.length && <Empty description={description} />}
    </Spin>
  );
};

const Frames = ({ onVisibleChange }) => {
  const t = useLocale(i18n);
  const { projectId, flowId } = useUrlParams();

  const [activeTab, setActiveTab] = useState('1');
  const { data: copyData, clear } = useCopy();

  const { loading, items, refreshTrash } = useTrash();
  const [clearTrashLoading, setClearTrashLoading] = useState(false);

  const clearTrashAll = useCallback(() => {
    if (items.length > 0) {
      setClearTrashLoading(true);
      // todo ru
      clearTrash(
        projectId,
        flowId,
        items.map((i) => i.key)
      )
        .then((nodes) => {
          Message.success(t['frame.clear.success']);
          refreshTrash();
          ru.push({
            type: RU_TYPE.TRASH_CLEAR,
            changed: {
              after: nodes,
            },
            dependencies: {
              projectId,
              flowId,
            },
          });
        })
        .finally(() => setClearTrashLoading(false));
    }
  }, [flowId, items, projectId, refreshTrash, t]);

  return (
    <div
      style={{
        borderRadius: 4,
        boxShadow:
          '0 4px 6px -1px rgb(0 0 0 / 10%), 0px -2px 4px -1px rgb(0 0 0 / 6%)',
        background: 'var(--color-bg-3)',
        zIndex: 999,
      }}
    >
      <Tabs
        activeTab={activeTab}
        onChange={setActiveTab}
        destroyOnHide
        extra={
          <Space size="small">
            <Button
              loading={clearTrashLoading}
              type="text"
              size="mini"
              icon={<IconDelete />}
              onClick={activeTab === '1' ? clear : clearTrashAll}
            >
              {t['frame.clear']}
            </Button>
            <Button
              type="text"
              status="warning"
              size="mini"
              icon={<IconMinusCircle />}
              onClick={() => onVisibleChange(false)}
            >
              {t['frame.min']}
            </Button>
          </Space>
        }
      >
        <Tabs.TabPane
          key="1"
          title={
            <span>
              <FiClipboard style={{ marginRight: 6 }} />
              {t['frame.clipboard']}
            </span>
          }
          style={{ paddingLeft: 8, paddingRight: 8, paddingBottom: 8 }}
        >
          {copyData ? (
            <CopyFrame frame={copyData} />
          ) : (
            <Empty description={t['frame.empty.data']} />
          )}
        </Tabs.TabPane>
        <Tabs.TabPane
          key="2"
          title={
            <span>
              <BsRecycle style={{ marginRight: 6 }} />
              {t['frame.recycle']}
            </span>
          }
          style={{ paddingLeft: 8, paddingRight: 8, paddingBottom: 8 }}
        >
          <TrashWrap
            loading={loading}
            items={items}
            refreshTrash={refreshTrash}
            description={t['frame.empty.data']}
          />
        </Tabs.TabPane>
      </Tabs>
    </div>
  );
};

const Content = ({ children, visible, onVisibaleChange }) => {
  return (
    <div style={{ position: 'relative' }}>
      {children}
      {visible && (
        <div style={{ position: 'absolute', top: 24, right: 24 }}>
          <Frames onVisibleChange={onVisibaleChange} />
        </div>
      )}
      <div style={{ position: 'absolute', bottom: 22, right: 98 }}>
        <Button
          type="primary"
          size="large"
          status={visible ? 'success' : 'default'}
          style={{ width: 54, height: 54 }}
          icon={<FiClipboard fontSize={32} />}
          onClick={() => onVisibaleChange(!visible)}
          shape="circle"
        />
      </div>
    </div>
  );
};

const CopyAndTrash = ({ flowId, children }) => {
  const [visible, setVisible] = useState(false);
  return (
    <CopyContextProvider flowId={flowId}>
      <TrashContextProvider>
        <Content visible={visible} onVisibaleChange={setVisible}>
          {children}
        </Content>
      </TrashContextProvider>
    </CopyContextProvider>
  );
};

export default CopyAndTrash;
