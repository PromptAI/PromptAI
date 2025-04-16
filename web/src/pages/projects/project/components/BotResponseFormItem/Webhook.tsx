import { listWebhooks } from '@/api/components';
import { BotResponse, BotResponseWebhookContent } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Select,
  Space,
  Typography,
} from '@arco-design/web-react';
import { IconCloud, IconLink, IconPlus } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import { cloneDeep, keyBy } from 'lodash';
import nProgress from 'nprogress';
import React from 'react';
import { useHistory, useParams } from 'react-router';
import i18n from './locale';

interface WebhookProps {
  value?: BotResponse<BotResponseWebhookContent>;
  onChange?: (value: BotResponse<BotResponseWebhookContent>) => void;
  disabled?: boolean;
  prefix?: React.ReactNode;
}
const Webhook = ({ value, onChange, disabled, prefix }: WebhookProps) => {
  const t = useLocale(i18n);
  const history = useHistory();
  const { id: projectId } = useParams<{ id: string }>();

  const { loading, data = [] } = useRequest<BotResponseWebhookContent[], []>(
    () => listWebhooks(projectId),
    {
      refreshDeps: [projectId],
    }
  );

  const handleWehookChange = (key: string) => {
    const map = keyBy(data, (d) => d.id);
    if (map[key]) {
      const webhook = cloneDeep(map[key]);
      onChange({ ...value, content: { ...value?.content, ...webhook } });
    }
  };
  const handleToDetail = (id) => {
    nProgress.start();
    history.push(`/projects/${projectId}/view/webhooks/info/${id}`);
    nProgress.done();
  };
  return (
    <Card size="small" bodyStyle={{ padding: 2 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Select
          className="flex-1"
          placeholder={t['conversation.botForm.webhook.placeholder']}
          loading={loading}
          value={value?.content?.id}
          onChange={handleWehookChange}
          showSearch
          allowClear
          disabled={disabled}
          filterOption={(inputValue, option) =>
            option.props.extra
              .toLowerCase()
              .indexOf(inputValue.toLowerCase()) >= 0
          }
          prefix={prefix}
        >
          {data.map((d) => (
            <Select.Option key={d.id} value={d.id} extra={d.text}>
              <div className="flex justify-between items-center">
                <Space size="mini">
                  <IconCloud />
                  <Typography.Text bold>{d.text}</Typography.Text>
                </Space>
                <Button
                  size="mini"
                  type="text"
                  icon={<IconLink />}
                  onClick={() => handleToDetail(d.id)}
                >
                  {t['conversation.botForm.webhook.detail']}
                </Button>
              </div>
            </Select.Option>
          ))}
        </Select>
        {!disabled && (
          <Button
            type="secondary"
            icon={<IconPlus />}
            onClick={() =>
              history.push(`/projects/${projectId}/view/webhooks/create`)
            }
          />
        )}
      </div>
    </Card>
  );
};

export default Webhook;
