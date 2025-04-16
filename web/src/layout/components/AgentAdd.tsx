import { addAgent } from '@/api/agent';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import i18n from './locale';

interface AgentAddProps {
  onSuccess: () => void;
}
const AgentAdd: React.FC<AgentAddProps> = ({ onSuccess }) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  const onOk = async () => {
    setLoading(true);
    try {
      await addAgent({});
      Message.success(t['agent.add.success']);
      onSuccess();
    } catch (error) {
      Message.error(error?.data?.message || 'Unknown Error');
    }
    setLoading(false);
  };
  return (
    <div>
      <Button loading={loading} type="primary" size="small" onClick={onOk}>
        <IconPlus />
        Agent
      </Button>
    </div>
  );
};

export default AgentAdd;
