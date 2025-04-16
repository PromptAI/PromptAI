import { IconApps, IconEdit } from '@arco-design/web-react/icon';
import * as React from 'react';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import MenuBox from '../../../components/MenuBox';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../components/DrawerBox';
import { useGraphStore } from '../../store';
import useLocale, { useLocaleLang } from '@/utils/useLocale';
import { Button, Form, Input, Radio, Select } from '@arco-design/web-react';
import CropImageUpload from '../../../components/CropImageUpload';
import TextArea from '../../../components/TextArea';
import ShowBranshAsButtonView from '../../../components/ShowBranshAsButtonView';
import useRules from '@/hooks/useRules';
import { updateProject } from '@/api/projects';
import { useProjectContext } from '@/layout/project-layout/context';
import i18n from './i18n';
import common from '../i18n/common';
import Visible from '../../../components/Visible';

export const ProjectIcon = IconApps;

export interface ProjectViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const ProjectView: React.FC<ProjectViewProps> = ({ node, ...props }) => {
  return (
    <View
      id={node.id}
      label={node.data?.name || 'project'}
      icon={<ProjectIcon />}
      {...props}
    />
  );
};
export const ProjectNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox trigger={<ProjectView node={props} />}>
      <UpdateProjectDrawerTrigger node={props} />
    </MenuBox>
  );
};

export interface ProjectDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  node: Partial<NodeProps>;
}
export const ProjectDrawerTrigger: React.FC<ProjectDrawerTriggerProps> = ({
  node,
  ...props
}) => {
  const nodes = useGraphStore((s) => s.nodes);
  const t = useLocale(i18n);
  const lang = useLocaleLang();
  const { refresh, ...project } = useProjectContext();
  const onFinish = React.useCallback(
    async (data) => {
      await updateProject({
        ...project,
        ...data,
        locale: lang.startsWith('en') ? 'en' : 'zh',
      });
      refresh();
    },
    [project, refresh, lang]
  );

  const defaultOptions = React.useMemo(
    () =>
      nodes
        ?.filter(
          (g) => ['conversation', 'sample'].includes(g.type) && !g.data.hidden
        )
        .map((g) => g.id) || [],
    [nodes]
  );
  const rules = useRules();
  const flowOptions = React.useMemo(
    () =>
      nodes
        ?.filter((g) => ['conversation', 'sample'].includes(g.type))
        .map((g) => ({ label: g.data.name, value: g.id })) || [],
    [nodes]
  );
  const showSubNodesAsOptions = React.useMemo(
    () =>
      ['none', 'all', 'custom'].map((value) => ({
        value,
        label: t[`project.form.optional.${value}`],
      })),
    [t]
  );
  return (
    <DrawerFormBoxTrigger
      title={t['project.title']}
      initialValues={{
        ...node.data,
        showShbNodesAsOptionalIds: defaultOptions,
      }}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item label={t['project.form.name']} field="name" rules={rules}>
        <Input placeholder="" />
      </Form.Item>
      <Form.Item label={t['project.form.welcome']} field="welcome">
        <TextArea
          autoSize={{ minRows: 2, maxRows: 4 }}
          placeholder={t['project.form.welcome.placeholder']}
        />
      </Form.Item>
      <Form.Item
        label={t['project.form.optional']}
        rules={rules}
        field="showSubNodesAsOptional"
      >
        <Radio.Group options={showSubNodesAsOptions} />
      </Form.Item>
      <Form.Item shouldUpdate noStyle>
        {({ showSubNodesAsOptional }) =>
          showSubNodesAsOptional === 'custom' && (
            <Form.Item rules={rules} field="showShbNodesAsOptionalIds">
              <Select
                options={flowOptions}
                placeholder={t['complex.form.optional.custom.selection']}
                mode="multiple"
                allowClear
              />
            </Form.Item>
          )
        }
      </Form.Item>
      <Form.Item shouldUpdate noStyle>
        {({ showSubNodesAsOptional, showShbNodesAsOptionalIds }) => (
          <ShowBranshAsButtonView
            mode={showSubNodesAsOptional}
            buttons={flowOptions}
            shows={showShbNodesAsOptionalIds}
          />
        )}
      </Form.Item>
      <Form.Item
        label={t['project.form.desc']}
        field="description"
        className="mt-4"
      >
        <TextArea autoSize />
      </Form.Item>
      <Form.Item label={t['project.form.image']} field="image">
        <CropImageUpload cropSize={{ width: 420, height: 260 }} />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};
export const UpdateProjectDrawerTrigger: React.FC<
  Omit<ProjectDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <ProjectDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
