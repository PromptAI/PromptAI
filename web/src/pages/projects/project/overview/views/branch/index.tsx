import {
  IconEdit,
  IconBranch,
  IconQuestionCircle,
  IconLink,
} from '@arco-design/web-react/icon';
import * as React from 'react';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import MenuBox from '../../../components/MenuBox';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../components/DrawerBox';
import { useGraphStore } from '../../store';
import useLocale from '@/utils/useLocale';
import { Button, Form, Input, Space } from '@arco-design/web-react';
import TextArea from '../../../components/TextArea';
import i18n from './i18n';
import common from '../i18n/common';
import Visible from '../../../components/Visible';
import { updateComponent } from '@/api/components';
import useRules from '@/hooks/useRules';
import useDocumentLinks from '@/hooks/useDocumentLinks';
import { useDynamicMenu } from '@/components/Layout/dynamic-menu-context';
import { Link } from 'react-router-dom';

export const BranchIcon = IconBranch;

export interface BranchViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const BranchView: React.FC<BranchViewProps> = ({ node, ...props }) => {
  return (
    <View
      id={node.id}
      label={node.data?.name || 'branch'}
      icon={<BranchIcon />}
      {...props}
    />
  );
};
export const BranchNode: React.FC<NodeProps> = (props) => {
  const c = useLocale(common);
  const projectId = useGraphStore((s) => s.projectId);
  return (
    <MenuBox trigger={<BranchView node={props} />}>
      <UpdateBranchDrawerTrigger node={props} />
      <Link
        to={`/projects/${projectId}/overview/complexs/${props.id}/branch/complex`}
      >
        <Button icon={<IconLink />}>{c['node.link']}</Button>
      </Link>
    </MenuBox>
  );
};

export interface BranchDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  node: Partial<NodeProps>;
}
export const BranchDrawerTrigger: React.FC<BranchDrawerTriggerProps> = ({
  node,
  ...props
}) => {
  const projectId = useGraphStore((s) => s.projectId);
  const t = useLocale(i18n);
  const { refresh } = useDynamicMenu();
  const onFinish = React.useCallback(
    async (values) => {
      await updateComponent(projectId, 'conversation', node.id, {
        data: { ...node.data, ...values, hidden: !values.hidden },
      });
      refresh();
    },
    [node.data, node.id, projectId, refresh]
  );
  const rules = useRules();
  const docs = useDocumentLinks();
  return (
    <DrawerFormBoxTrigger
      title={t['branch.title']}
      initialValues={{ ...node.data, hidden: !node.data?.hidden }}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item label={t['branch.form.name']} field="name" rules={rules}>
        <Input autoFocus placeholder={t['branch.form.name.placeholder']} />
      </Form.Item>
      <Form.Item label={t['branch.form.description']} field="description">
        <TextArea
          placeholder={t['branch.form.description.placeholder']}
          autoSize
          style={{ minHeight: 64 }}
        />
      </Form.Item>
      <Form.Item
        label={
          <Space>
            <span>{t['branch.form.welcome']}</span>
            <a target="_blank" href={docs.projectSettings} rel="noreferrer">
              <IconQuestionCircle />
            </a>
          </Space>
        }
        field="welcome"
      >
        <TextArea
          placeholder={t['branch.form.welcome.placeholder']}
          autoSize
          style={{ minHeight: 64 }}
        />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};
export const UpdateBranchDrawerTrigger: React.FC<
  Omit<BranchDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <BranchDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
