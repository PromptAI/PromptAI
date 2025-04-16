import React, { useMemo, useState } from 'react';
import { Button, Card, Drawer, Tooltip } from '@arco-design/web-react';
import { IconClose, IconInfoCircle } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import FaqForm from '@/pages/projects/project/sample/FaqFrom';

function Detail({ row, trigger }: { row: any; trigger?: any }) {
  const t = useLocale(i18n);

  const [visible, setVisible] = useState(false);

  const tirggerElement = useMemo(
    () =>
      React.cloneElement(
        React.isValidElement(trigger) ? (
          trigger
        ) : (
          <Button size="small" type="text" icon={<IconInfoCircle />} />
        ),
        { onClick: () => setVisible(true) }
      ),
    [trigger]
  );
  const initialValues = React.useMemo(
    () => ({
      ...row.user.data,
      responses: row.bot.data.responses || [],
    }),
    [row]
  );
  return (
    <>
      <Tooltip content={t['favorites.faq.detail.title']}>
        {tirggerElement}
      </Tooltip>
      <Drawer
        width={540}
        height={'calc(100vh - 51px'}
        bodyStyle={{ padding: 0 }}
        headerStyle={{ display: 'none' }}
        visible={visible}
        maskStyle={{ opacity: 0.1, cursor: 'not-allowed' }}
        maskClosable={false}
        escToExit={false}
        closable={false}
        footer={null}
        unmountOnExit
      >
        <Card
          title={t['favorites.faq.detail.title']}
          style={{ height: '100%', background: 'var(--color-bg-1)' }}
          bordered={false}
          headerStyle={{
            borderBottom: '1px solid #ccc',
            padding: '0 10px',
            height: 51,
          }}
          extra={
            <Button
              icon={<IconClose />}
              onClick={() => {
                setVisible(false);
              }}
            >
              {t['favorites.faq.detail.close']}
            </Button>
          }
        >
          <div style={{ height: 'calc(100vh - 100px)', overflowY: 'auto' }}>
            <FaqForm initialValues={initialValues} disabled />
          </div>
        </Card>
      </Drawer>
    </>
  );
}

export default Detail;
