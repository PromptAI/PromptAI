import { listBuiltinActions } from '@/api/action';
import { listAction } from '@/api/components';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import {
  Link,
  Select,
  SelectProps,
  Space,
  Tag,
  Typography,
} from '@arco-design/web-react';
import { IconCode, IconLink } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import React from 'react';
import { useHistory } from 'react-router';
import i18n from './locale';

interface ActionSelectorProps {
  value?: any;
  onChange?: (val: any) => void;
  builtin?: boolean;
}
const ActionSelector = ({
  value,
  onChange,
  builtin,
  ...rest
}: Omit<SelectProps, 'value' | 'onChange' | 'loading'> &
  ActionSelectorProps) => {
  const history = useHistory();
  const { projectId } = useUrlParams();
  const t = useLocale(i18n);
  const { loading, data } = useRequest(
    () => (builtin ? listBuiltinActions() : listAction(projectId)),
    {
      refreshDeps: [builtin, projectId],
    }
  );
  const onSelectChange = (_, opt) => {
    onChange?.(opt.extra);
  };
  return (
    <Select
      {...rest}
      value={value?.id}
      loading={loading}
      onChange={onSelectChange}
    >
      {data?.map((d) => (
        <Select.Option key={d.id} value={d.id} extra={d}>
          <div className="flex justify-between items-center">
            <Space size="mini">
              <IconCode />
              {builtin && <Tag color="blue">{t['action.builtin']}</Tag>}
              <Typography.Text bold>{d.data?.text || 'Action'}</Typography.Text>
            </Space>
            {!builtin && (
              <Link
                type="text"
                icon={<IconLink />}
                onClick={(evt) => {
                  evt.stopPropagation();
                  history.push(`/projects/${projectId}/view/action`);
                }}
              >
                {t['action.detail']}
              </Link>
            )}
          </div>
        </Select.Option>
      ))}
    </Select>
  );
};

export default ActionSelector;
