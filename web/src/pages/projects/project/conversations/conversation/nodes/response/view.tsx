import { RobotText } from '@/graph-next/components/IconText';
import { decodeAttachmentText } from '@/utils/attachment';
import { Tag } from '@arco-design/web-react';
import { IconStar } from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';

const ResponseView = (props) => {
  const name = useMemo(() => {
    if (!props.data.responses.length) return '-';
    const { content, type } = props.data.responses[0];
    const { text } = content || {};
    if (type === 'attachment') {
      return decodeAttachmentText(text).name;
    }
    if (type === 'action') return text || 'action';
    return text || '-';
  }, [props.data.responses]);
  return (
    <div className="flex gap-2" style={{ position: 'relative' }}>
      <RobotText>{name}</RobotText>
      <div className="node-extra-bot">
        {props.linkedFrom && (
          <Tag
            size="small"
            color="orange"
            icon={<IconStar />}
            style={{ fontSize: 10 }}
          >
            {props.linkedFrom.name}
          </Tag>
        )}
      </div>
    </div>
  );
};

export default ResponseView;
