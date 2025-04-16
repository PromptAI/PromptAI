import { IDName, IntentNextData } from '@/graph-next/type';
import { FormInstance } from '@arco-design/web-react';
import React, { RefObject } from 'react';
import IntentLinkShare from './components/IntentLinkShare';
import ShareLinkIntent from './components/ShareLinkIntent';

export interface MainExampleExtraProps {
  label: string;
  id: string;
  linkedFrom: IDName;
  onLinkedFrom?: (data: IntentNextData, linkedFrom: IDName) => void;
  formRef: RefObject<FormInstance>;
  mode: 'update' | 'create';
}

const MainExampleExtra: React.FC<MainExampleExtraProps> = ({
  label,
  id,
  linkedFrom,
  onLinkedFrom,
  mode,
  formRef,
}) => {
  return (
    <div className="flex justify-between items-center w-full">
      <span>{label}</span>
      <div className="flex items-center space-x-2">
        <IntentLinkShare
          nodeId={id}
          share={linkedFrom}
          onChange={onLinkedFrom}
        />
        {mode === 'update' && !linkedFrom && (
          <ShareLinkIntent
            nodeId={id}
            share={linkedFrom}
            onChange={onLinkedFrom}
            valueFormRef={formRef}
          />
        )}
      </div>
    </div>
  );
};

export default MainExampleExtra;
