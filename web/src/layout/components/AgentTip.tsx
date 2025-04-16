import { addAgent } from '@/api/agent';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { Link, Modal, Spin, Typography } from '@arco-design/web-react';
import { useMount, useRequest } from 'ahooks';
import React from 'react';
import { IconInfoCircle } from '@arco-design/web-react/icon';
import AgentInstallCommand from './AgentInstallCommand';
import useAgentStore from './agent-store';

const AgentTip = () => {
  const t = useLocale(i18n);
  const { loading, available, visible, init, toggle } = useAgentStore();

  const { loading: initialing, data: { cmd } = { cmd: '' } } = useRequest(
    () => addAgent({ init: true }),
    {
      manual: !available && !visible,
      refreshDeps: [visible],
    }
  );
  useMount(init);

  if (available) return <></>;
  return (
    <div>
      <Typography.Text style={{ marginRight: 32 }}>
        <Link
          hoverable={false}
          onClick={toggle}
          style={{ color: 'lightcoral' }}
        >
          <IconInfoCircle />
          {t['agent.tip']}
        </Link>
      </Typography.Text>
      <Modal
        visible={visible}
        style={{ width: '50%' }}
        title={
          <div style={{ textAlign: 'left' }}>
            <Typography.Title heading={4} style={{ margin: 0 }}>
              {t[`agent.tip.title`]}
            </Typography.Title>
          </div>
        }
        autoFocus={false}
        focusLock={true}
        footer={(cancel) => cancel}
        cancelText={t['agent.close']}
        onCancel={toggle}
      >
        <Spin loading={loading || initialing} className="w-full">
          <div style={{ backgroundColor: 'rgb(var(--gray-1))', padding: 16 }}>
            <AgentInstallCommand cmd={cmd} />
          </div>
        </Spin>
      </Modal>
    </div>
  );
};

export default AgentTip;
