import { NodeDefinedProps } from '@/core-next/types';
import IconText from '@/graph-next/components/IconText';
import Wrapper from '@/graph-next/Wrapper';
import useLocale from '@/utils/useLocale';
import { IconSync } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from './locale';

const ComplexNode = (props: NodeDefinedProps) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { defaultProps, ...node } = props;
  const t = useLocale(i18n);
  return (
    <Wrapper selected={node.selected} validatorError={null}>
      <IconText icon={<IconSync />}>{t['complex.name']}</IconText>
    </Wrapper>
  );
};

export default ComplexNode;
