import { NodeDefinedProps } from '@/core-next/types';
import IconText from '@/graph-next/components/IconText';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import useLocale from '@/utils/useLocale';
import { IconApps, IconEdit } from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import i18n from './locale';

const ProjectNode = (props: NodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const t = useLocale(i18n);
  const menus = useMemo<PopupMenu[]>(() => {
    const { onChangeEditSelection } = defaultProps;
    return [
      {
        key: 'edit-node',
        title: t['flow.node.edit'],
        icon: <IconEdit />,
        onClick: () => onChangeEditSelection(node),
      },
    ];
  }, [defaultProps, t, node]);
  return (
    <Wrapper menus={menus} selected={node.selected} validatorError={null}>
      <IconText icon={<IconApps />}>{node.data?.name}</IconText>
    </Wrapper>
  );
};

export default ProjectNode;
