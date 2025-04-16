import { del } from '@/utils/request';
import useLocale from '@/utils/useLocale';
import { Button, Modal } from '@arco-design/web-react';
import React from 'react';
import i18n from './locale';

const DeleteCorpus = ({ slotId, onSuccess }) => {
  const t = useLocale(i18n);
  const handle = () => {
    Modal.confirm({
      title: t['slot.corpus.delete'],
      content: t['slot.corpus.delete.content'],
      onOk: async () => {
        return del(`/api/project/component/entity/dictionary/${slotId}`).then(
          onSuccess
        );
      },
      footer: (c, o) => [o, c],
    });
  };
  return (
    <Button type="outline" size="mini" status="danger" onClick={handle}>
      {t['slot.corpus.delete']}
    </Button>
  );
};

export default DeleteCorpus;
