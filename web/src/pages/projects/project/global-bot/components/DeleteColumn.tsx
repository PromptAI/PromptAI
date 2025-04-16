import { delGlobalBot } from '@/api/global-component';
import useLocale from '@/utils/useLocale';
import { Button, Message, Modal, Tooltip } from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import React from 'react';
import useUrlParams from '../../hooks/useUrlParams';
import i18n from '../locale';

const DeleteColumn = ({ item, onSuccess }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const handleDeleteItem = (id: string) => {
    Modal.confirm({
      title: t['globalBots.detele.modelTitle'],
      content: (
        <div className="flex justify-center">
          <span>{t['globalBots.detele.modelDescription']}</span>
        </div>
      ),
      onOk: async () => {
        await delGlobalBot(projectId, [id]);
        Message.success(t['globalBots.detele.success']);
        onSuccess();
      },
      footer: (cancel, ok) => (
        <>
          {ok}
          {cancel}
        </>
      ),
    });
  };
  return (
    <Tooltip content={t['globalBots.table.delete']}>
      <Button
        type="text"
        size="mini"
        status="danger"
        onClick={() => handleDeleteItem(item.id)}
      >
        <IconDelete />
      </Button>
    </Tooltip>
  );
};

export default DeleteColumn;
