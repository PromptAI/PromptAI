import { Button } from '@arco-design/web-react';
import { IconCopy } from '@arco-design/web-react/icon';
import React, { useCallback } from 'react';

import i18n from './i18n';
import useLocale from '@/utils/useLocale';
import { NodeProps } from '../../types';
import { useCopy } from '../../../copy/context';
import { unwrap } from '../../utils/node';

interface CopyNodeTriggerProps {
  node: NodeProps;
}
const CopyNodeTrigger: React.FC<CopyNodeTriggerProps> = ({ node }) => {
  const t = useLocale(i18n);
  const { submit } = useCopy();
  const onClick = useCallback(() => {
    submit({
      key: node.id,
      type: node.type,
      data: { breakpoint: unwrap(node) },
    });
  }, [submit, node]);
  return (
    <Button icon={<IconCopy />} onClick={onClick}>
      {t['copy']}
    </Button>
  );
};

export default CopyNodeTrigger;
