import React from 'react';
import { Message, Switch } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import { defaultAgent } from '@/api/agent';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

interface AgentDefaultSwitchProps {
  row: any;
  onSuccess: () => void;
}
const AgentDefaultSwitch: React.FC<AgentDefaultSwitchProps> = ({
  row,
  onSuccess,
}) => {
  const t = useLocale(i18n);
  const { loading, run } = useRequest(() => defaultAgent(row.id), {
    manual: true,
    onSuccess: () => {
      Message.success(t['agent.default.success']);
      onSuccess();
    },
  });
  return (
    <Switch
      checkedText={t['agent.default.true']}
      uncheckedText={t['agent.default.false']}
      loading={loading}
      checked={row.default}
      onChange={run}
      disabled={row.default}
    />
  );
};

export default AgentDefaultSwitch;
