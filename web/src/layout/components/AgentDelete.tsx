import React, { PropsWithChildren, useState } from 'react';
import DeleteColumn from '@/components/DeleteColumn';
import { deleteAgent } from '@/api/agent';
import { isEmpty } from 'lodash';
import { Modal, Typography } from '@arco-design/web-react';
import { CopyButton } from './AgentInstallCommand';
import { useToggle } from 'ahooks';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { IconQuestionCircle } from '@arco-design/web-react/icon';
import useDocumentLinks from '@/hooks/useDocumentLinks';

interface AgentDeleteProps {
  title: string;
  row: any;
  onSuccess: (val: any) => void;
}
const AgentDelete: React.FC<PropsWithChildren<AgentDeleteProps>> = ({
  title,
  row,
  onSuccess,
  children,
}) => {
  const t = useLocale(i18n);
  const [visible, { toggle }] = useToggle(false);
  const [unInstallCommand, setUnInstallCommand] = useState('');
  const onSuccessWrap = (data) => {
    if (isEmpty(data)) {
      onSuccess(data);
      return;
    }
    if (data.cmd) {
      setUnInstallCommand(data.cmd);
      toggle();
    }
  };
  const docs = useDocumentLinks();
  return (
    <>
      <DeleteColumn
        row={row}
        promise={deleteAgent}
        onSuccess={onSuccessWrap}
        title={title}
        size="small"
        icon={false}
      >
        {children}
      </DeleteColumn>
      <Modal
        visible={visible}
        style={{ width: '50%' }}
        title={
          <div style={{ textAlign: 'left' }}>
            <Typography.Title heading={5} style={{ margin: 0 }}>
              {t['agent.command.uninstall.title']}{' '}
              <a href={docs.installAgent}>
                <IconQuestionCircle />
              </a>
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
        <div className="flex items-center">
          <Typography.Paragraph
            style={{
              display: 'flex',
              margin: 0,
              lineHeight: 1,
              alignItems: 'center',
            }}
            code
          >
            {unInstallCommand}
          </Typography.Paragraph>
          <div
            style={{
              backgroundColor: 'rgb(var(--gray-2))',
              marginLeft: 8,
              height: '100%',
            }}
          >
            <CopyButton text={unInstallCommand.split("'")[1] || 'error'} />
          </div>
        </div>
      </Modal>
    </>
  );
};

export default AgentDelete;
