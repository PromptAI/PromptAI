import { IDName } from '@/graph-next/type';
import { IconStar } from '@arco-design/web-react/icon';
import React from 'react';
interface LinkedFromExtraProps {
  linkedFrom: IDName;
}
const LinkedFromExtra: React.FC<LinkedFromExtraProps> = ({ linkedFrom }) => {
  return (
    <span
      className="bg-orange-100 text-orange-400 truncate leading-none text-sm p-[2px] rounded"
      title={linkedFrom.name}
    >
      <IconStar />
      {linkedFrom.name}
    </span>
  );
};

export default LinkedFromExtra;
