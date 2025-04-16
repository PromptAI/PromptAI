import { NodeDefinedProps } from '@/core-next/types';
import IconText from '@/graph-next/components/IconText';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import useLocale from '@/utils/useLocale';
import { IconEdit, IconQuestionCircle } from '@arco-design/web-react/icon';
import React, { useMemo } from 'react';
import i18n from './locale';

const nameFuncMap = {
  undefined: (data, builtInName) => data?.fallback || builtInName,
  text: (data, builtInName) => data?.fallback || builtInName,
  webhook: (data) => data?.webhooks?.[0]?.content.text || '-',
  action: (data) => data?.text || 'Action',
  action_promptai: (_, builtInName, defaultName) => defaultName,
  talk2bits: () => 'deprecated',
};
const FallbackNode = (props: NodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const t = useLocale(i18n);
  const menus = useMemo<PopupMenu[]>(() => {
    const { onChangeEditSelection } = defaultProps;
    function handleEdit() {
      onChangeEditSelection(node);
    }
    return [
      {
        key: 'edit-node',
        title: t['flow.node.edit'],
        icon: <IconEdit />,
        onClick: handleEdit,
      },
    ];
  }, [defaultProps, node, t]);
  const name = useMemo(() => {
    return nameFuncMap[node.data?.fallbackType](
      node.data,
      t['fallback.title.unknown'],
      t['fallback.title.action_promptai']
    );
  }, [node.data, t]);
  return (
    <Wrapper menus={menus} selected={node.selected} validatorError={null}>
      <IconText icon={<IconQuestionCircle />}>
        {`${t[`fallback.title.${node.data?.fallbackType}`]}:${name}`}
      </IconText>
    </Wrapper>
  );
};

export default FallbackNode;
