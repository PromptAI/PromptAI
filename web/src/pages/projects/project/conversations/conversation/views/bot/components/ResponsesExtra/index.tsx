import React, { RefObject } from 'react';
import { IDName, IntentNextData } from '@/graph-next/type';
import BotShare from './components/BotShare';
import ShareBot from './components/ShareBot';
import { FormInstance } from '@arco-design/web-react';

export interface ResponsesExtraProps {
  label?: string;
  nodeId: string;
  linkedFrom: IDName;
  onLinkedFrom: (data: IntentNextData, linkedFrom: IDName) => void;
  formRef: RefObject<FormInstance>;
  mode: 'update' | 'create';
}
const ResponsesExtra: React.FC<ResponsesExtraProps> = ({
  label,
  nodeId,
  linkedFrom,
  onLinkedFrom,
  formRef,
  mode,
}) => {
  return (
    <div className="flex justify-between items-center w-full">
      <span>{label}</span>
      <div className="flex items-center space-x-2">
        <BotShare share={linkedFrom} onChange={onLinkedFrom} />
        {mode === 'update' && !linkedFrom && (
          <ShareBot
            nodeId={nodeId}
            share={linkedFrom}
            onChange={onLinkedFrom}
            valueFormRef={formRef}
          />
        )}
      </div>
    </div>
  );
};

export default ResponsesExtra;
