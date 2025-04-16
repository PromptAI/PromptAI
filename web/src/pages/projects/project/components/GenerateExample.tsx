import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { IconThunderbolt } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from '@/pages/projects/locale';
import { useRequest } from 'ahooks';
import { GenerateExampleParams, generateExample } from '@/api/generate';

export interface GenerateExampleProps {
  disabled: boolean;
  onGetParams: () => GenerateExampleParams;
  onGenerated: (data: string[]) => void;
}

const GenerateExample: React.FC<GenerateExampleProps> = ({
  disabled,
  onGetParams,
  onGenerated,
}) => {
  const t = useLocale(i18n);
  const { loading, run } = useRequest(() => generateExample(onGetParams()), {
    manual: true,
    onSuccess: ({ intents }) => {
      onGenerated(intents);
    },
    onError: (e: any) => {
      Message.error(e?.data?.message || 'Error');
    },
  });
  return (
    <Button
      loading={loading}
      size="mini"
      type="outline"
      disabled={disabled}
      onClick={run}
    >
      <IconThunderbolt />
      {t['conversation.intentForm.examples.generate']}
    </Button>
  );
};

export default GenerateExample;
