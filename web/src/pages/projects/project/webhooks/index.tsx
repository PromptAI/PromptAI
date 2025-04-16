import useLocale, { useDefaultLocale } from '@/utils/useLocale';
import {
  Button,
  Card,
  Empty,
  Grid,
  Message,
  Modal,
  Space,
  Typography,
} from '@arco-design/web-react';
import React, { useMemo } from 'react';
import { useHistory, useParams } from 'react-router';
import i18n from '@/pages/projects/locale';
import nProgress from 'nprogress';
import {
  IconCopy,
  IconDelete,
  IconPlus,
  IconShake,
  IconSync,
} from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import { deleteWebhook, listWebhooks } from '@/api/components';
import Yun from '@/assets/menu-icon/yun.svg';
import { Tool, useTools } from '@/components/Layout/tools-context';

const { Row, Col } = Grid;
const Webhooks = () => {
  const history = useHistory();
  const t = useLocale(i18n);
  const dt = useDefaultLocale();

  const { id: pId } = useParams<{ id: string }>();
  const {
    loading,
    data = [],
    refresh,
  } = useRequest(() => listWebhooks(pId), {
    refreshDeps: [pId],
  });

  const linkTo = (wId: string) => {
    nProgress.start();
    history.push(`/projects/${pId}/view/webhooks/info/${wId}`);
    nProgress.done();
  };

  const onDel = (wId: string, text: string) => {
    Modal.confirm({
      title: dt['common.delete.confirm.title'],
      content: `${t['webhookView.card.delete.tooltip']} ${text}?`,
      onConfirm: async () => {
        await deleteWebhook(pId, [wId]);
        Message.success(dt['message.delete.success']);
        refresh();
      },
      footer: (cancel, ok) => (
        <>
          {ok}
          {cancel}
        </>
      ),
    });
  };

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const cloneWebhook = ({ id, ...rest }) => {
    window.localStorage.setItem('clone_webhook', JSON.stringify(rest));
    nProgress.start();
    history.push(`/projects/${pId}/view/webhooks/create`);
    nProgress.done();
  };

  const tools = useMemo<Tool[]>(() => {
    const linkToCreate = () => {
      nProgress.start();
      history.push(`/projects/${pId}/view/webhooks/create`);
      nProgress.done();
    };
    return [
      {
        key: 'add',
        component: (
          <Button type="text" icon={<IconPlus />} onClick={linkToCreate}>
            {t['webhooks.create']}
          </Button>
        ),
      },
      {
        key: 'refresh',
        component: (
          <Button
            type="text"
            icon={<IconSync />}
            loading={loading}
            onClick={refresh}
          >
            {t['webhooks.refresh']}
          </Button>
        ),
      },
    ];
  }, [history, loading, pId, refresh, t]);
  useTools(tools);
  return (
    <Card size="small" title={t['webhooks.title']} loading={loading}>
      <Row>
        {data.map((d) => (
          <Col key={d.id} span={6}>
            <div style={{ padding: 4 }}>
              <Card
                size="small"
                hoverable
                onClick={() => linkTo(d.id)}
                title={
                  <div
                    style={{
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      maxWidth: 200,
                      overflow: 'hidden',
                    }}
                    title={d.text}
                  >
                    <Yun className="icon-size" />
                    <span>{d.text}</span>
                  </div>
                }
                extra={
                  <div
                    onClick={(e) => {
                      e.stopPropagation();
                    }}
                  >
                    <Button
                      size="small"
                      icon={<IconDelete />}
                      status="danger"
                      type="text"
                      onClick={() => onDel(d.id, d.text)}
                    >
                      {t['webhookView.card.delete']}
                    </Button>
                  </div>
                }
                actions={[
                  <div
                    key="action"
                    onClick={(e) => {
                      e.stopPropagation();
                    }}
                  >
                    <Space size="mini">
                      <Button
                        size="small"
                        icon={<IconCopy />}
                        onClick={() => cloneWebhook(d)}
                      >
                        {t['webhookView.card.copy']}
                      </Button>
                      <Button
                        size="small"
                        icon={<IconShake />}
                        type="text"
                        onClick={() => linkTo(d.id)}
                      >
                        {t['webhookView.card.detail']}
                      </Button>
                    </Space>
                  </div>,
                ]}
              >
                <Typography.Text ellipsis style={{ minHeight: 22 }}>
                  {d.description}
                </Typography.Text>
              </Card>
            </div>
          </Col>
        ))}
      </Row>
      {data.length === 0 && <Empty description={dt['common.empty']} />}
    </Card>
  );
};

export default Webhooks;
