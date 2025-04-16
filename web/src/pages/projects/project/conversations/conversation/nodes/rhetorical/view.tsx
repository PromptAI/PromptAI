import { RobotText } from '@/graph-next/components/IconText';
import React from 'react';

const RhetoricalView = (props) => {
  if (!props.data.responses.length) return <RobotText>unknown</RobotText>;
  const { id, content } = props.data.responses[0];
  return <RobotText key={id}>{(content as any).text || '-'}</RobotText>;
};

export default RhetoricalView;
