import { getAgentCommand } from '@/api/agent';
import useLocale from '@/utils/useLocale';
import { Link, Modal, Spin, Typography } from '@arco-design/web-react';
import { useRequest, useToggle } from 'ahooks';
import React from 'react';
import i18n from './locale';
import AgentInstallCommand from './AgentInstallCommand';

interface AgentCommandProps {
  row: any;
}
const AgentCommand: React.FC<AgentCommandProps> = ({ row }) => {
  const t = useLocale(i18n);
  const [visible, { toggle }] = useToggle(false);
  const { loading, data = { cmd: '' } } = useRequest(
    () => getAgentCommand({ agentId: row.id }),
    {
      manual: !visible,
      refreshDeps: [visible, row.id],
    }
  );
  return (
    <div>
      <Link onClick={toggle}>{t['agent.command.install']}</Link>
      <Modal
        visible={visible}
        style={{ width: '50%' }}
        title={
          <div style={{ textAlign: 'left' }}>
            <Typography.Title heading={5} style={{ margin: 0 }}>
              {t['agent.command.install.title']}
            </Typography.Title>
          </div>
        }
        autoFocus={false}
        focusLock={true}
        footer={(cancel) => cancel}
        onOk={toggle}
        onCancel={toggle}
        cancelText={t['agent.close']}
      >
        <Spin loading={loading} className="w-full">
          <AgentInstallCommand cmd={data.cmd} />
        </Spin>
      </Modal>
    </div>
  );
};

export default AgentCommand;
