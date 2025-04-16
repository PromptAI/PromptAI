import IconText from '@/graph-next/components/IconText';
import useLocale from '@/utils/useLocale';
import { Space, Typography } from '@arco-design/web-react';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';
import i18n from '../locale';
import { getCanLinkUserNode } from '../../drawer/GotoForm';
import { IconUser } from '@arco-design/web-react/icon';

const GotoView = (props) => {
  const t = useLocale(i18n);
  const icon = useMemo(
    () =>
      cloneElement(nodeIconsMap['goto'], {
        className: 'app-icon',
        style: { color: props?.data?.color },
      }),
    [props?.data?.color]
  );
  const nodeMap = useMemo(
    () =>
      getCanLinkUserNode(props).reduce(
        (acc, cur) => ({ ...acc, [cur.id]: cur }),
        {}
      ),
    [props]
  );
  const linkNode = nodeMap[props.data.linkId];
  const handleMouseEnter = () => {
    if (linkNode) {
      document
        .querySelector(`.${linkNode.id}`)
        .classList.add('flow-goto-highlight');
    }
  };
  const handleMouseLeave = () => {
    if (linkNode) {
      document
        .querySelector(`.${linkNode.id}`)
        .classList.remove('flow-goto-highlight');
    }
  };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
      <IconText
        icon={icon}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        color={props.data?.color}
      >
        {props.data?.name || t['flow.node.goto']}
      </IconText>
      {linkNode && (
        <Space>
          <Typography.Text>{'-->'}</Typography.Text>
          <IconUser />
          <Typography.Text>
            {' '}
            {linkNode.data.examples?.[0]?.text ||
              linkNode.data.examples?.[0] ||
              // linkNode.data.responses?.[0]?.content?.text ||
              linkNode.id}
          </Typography.Text>
        </Space>
      )}
    </div>
  );
};

export default GotoView;
