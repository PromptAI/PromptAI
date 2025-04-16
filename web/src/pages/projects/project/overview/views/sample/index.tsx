import {
  IconEdit,
  IconLink,
  IconOrderedList,
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
import { Button, Form } from '@arco-design/web-react';
import TextArea from '../../../components/TextArea';
import i18n from './i18n';
import common from '../i18n/common';
import Visible from '../../../components/Visible';
import { updateFaq } from '@/api/components';
import { Link } from 'react-router-dom';

export const SampleIcon = IconOrderedList;

export interface SampleViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const SampleView: React.FC<SampleViewProps> = ({ node, ...props }) => {
  const t = useLocale(i18n);
  return (
    <View
      id={node.id}
      label={t['sample.form.name']}
      icon={<SampleIcon />}
      {...props}
    />
  );
};
export const SampleNode: React.FC<NodeProps> = (props) => {
  const projectId = useGraphStore((s) => s.projectId);
  const c = useLocale(common);
  return (
    <MenuBox trigger={<SampleView node={props} />}>
      <UpdateSampleDrawerTrigger node={props} />
      <Link to={`/projects/${projectId}/overview/knowledge/sample`}>
        <Button icon={<IconLink />}>{c['node.link']}</Button>
      </Link>
    </MenuBox>
  );
};

export interface SampleDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  node: Partial<NodeProps>;
}
export const SampleDrawerTrigger: React.FC<SampleDrawerTriggerProps> = ({
  node,
  ...props
}) => {
  const projectId = useGraphStore((s) => s.projectId);
  const t = useLocale(i18n);
  const onFinish = React.useCallback(
    async (data) => {
      const { initMessage, endMessage, ...rest } = data;
      await updateFaq(
        projectId,
        node.id,
        {
          ...node.data,
          initMessage: initMessage || t['sample.form.start.placeholder'],
          endMessage: endMessage || t['sample.form.end.placeholder'],
          ...rest,
        },
        [projectId]
      );
    },
    [node.data, node.id, projectId, t]
  );
  return (
    <DrawerFormBoxTrigger
      title={t['sample.title']}
      initialValues={node.data}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item label={t['sample.form.start']} field="initMessage">
        <TextArea autoSize placeholder={t['sample.form.start.placeholder']} />
      </Form.Item>
      <Form.Item label={t['sample.form.end']} field="endMessage">
        <TextArea autoSize placeholder={t['sample.form.end.placeholder']} />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};
export const UpdateSampleDrawerTrigger: React.FC<
  Omit<SampleDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <SampleDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
