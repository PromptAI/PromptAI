import { NodeDefinedProps } from '@/core-next/types';
import IconText from '@/graph-next/components/IconText';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import useLocale from '@/utils/useLocale';
import { IconBranch, IconEdit, IconLink } from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import { useHistory } from 'react-router';
import i18n from './locale';

const BranchNode = (props: NodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const t = useLocale(i18n);
  const history = useHistory();
  const menus = useMemo<PopupMenu[]>(() => {
    const { projectId, onChangeEditSelection } = defaultProps;
    return [
      {
        key: 'edit-node',
        title: t['flow.node.edit'],
        icon: <IconEdit />,
        onClick: () => onChangeEditSelection(node),
      },
      {
        key: 'link',
        title: t['flow.node.link'],
        icon: <IconLink />,
        onClick: () => {
          history.push(
            `/projects/${projectId}/overview/complexs/${node.id}/branch/complex`
          );
        },
      },
    ];
  }, [defaultProps, t, node, history]);
  return (
    <Wrapper menus={menus} selected={node.selected} validatorError={null}>
      <IconText icon={<IconBranch />}>{node.data?.name}</IconText>
    </Wrapper>
  );
};

export default BranchNode;
