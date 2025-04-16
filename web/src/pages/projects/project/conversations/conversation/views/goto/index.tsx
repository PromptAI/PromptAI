import { GrDirections } from 'react-icons/gr';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import React, { createElement, useCallback, useMemo } from 'react';
import {
  findClosestUserNode,
  findGotoFilterTargets,
  getAllNodes,
  getNodeIcon,
  getNodeLabel,
  parseResponseOfCreate,
  unwrap,
} from '../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { useGraphStore } from '../../store/graph';
import { createComponent, updateComponent } from '@/api/components';
import ru, { RU_TYPE } from '../../features/ru';
import { Button, Form, Select } from '@arco-design/web-react';
import TextArea from '@/pages/projects/project/components/TextArea';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../i18n';
import { IconEdit } from '@arco-design/web-react/icon';
import { keyBy } from 'lodash';

export const GotoIcon = GrDirections;

export interface GotoViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const GotoView: React.FC<GotoViewProps> = ({ node, ...props }) => {
  const nodesMap = useGraphStore((s) => keyBy(s.nodes, 'id'));
  const linked = useMemo(
    () => nodesMap[node.data?.linkId],
    [node.data?.linkId, nodesMap]
  );
  const label = useMemo(() => {
    const prefix = getNodeLabel(node);
    const linkedLabel = getNodeLabel(linked || {});
    return (
      <span>
        <span>{`${prefix} --> `}</span>
        {createElement(getNodeIcon(linked || {}), { className: 'mr-1' })}
        <span>{linkedLabel}</span>
      </span>
    );
  }, [node, linked]);
  const handleMouseEnter = () => {
    if (linked) {
      document
        .querySelector(`.${linked.id}`)
        ?.classList.add('flow-goto-highlight');
    }
  };
  const handleMouseLeave = () => {
    if (linked) {
      document
        .querySelector(`.${linked.id}`)
        ?.classList.remove('flow-goto-highlight');
    }
  };
  return (
    <View
      icon={<GotoIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    />
  );
};

export const GotoNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox
      trigger={<GotoView node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateGotoDrawerTrigger node={props} />
      <DeleteNodeTrigger node={props} />
    </MenuBox>
  );
};

export const getCanLinkUserNode = (node) => {
  const closestUserNode = findClosestUserNode(node);
  const list = getAllNodes(node);
  return list.filter(
    (n) => ['user', 'option'].includes(n.type) && n.id !== closestUserNode.id
  );
};
export interface GotoDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any> | (() => Partial<any>);
}
export const GotoDrawerTrigger: React.FC<GotoDrawerTriggerProps> = ({
  mode,
  node,
  initialValues: originInitialValues,
  ...props
}) => {
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));

  const initialValues = useMemo(() => {
    if (mode === 'update') return node.data;
    if (typeof originInitialValues === 'function') return originInitialValues();
    return originInitialValues;
  }, [mode, node.data, originInitialValues]);

  const onFinish = useCallback(
    async (data) => {
      if (mode === 'update') {
        const after = await updateComponent(projectId, 'goto', node.id, {
          ...unwrap(node),
          data,
        });
        ru.push({
          type: RU_TYPE.UPDATE_NODE,
          changed: { after, before: unwrap(node) },
          dependencies: { flowId, projectId },
        });
      }
      if (mode === 'create') {
        const nodes = await createComponent(projectId, 'goto', {
          data,
          parentId: node.id,
        });
        const [changed, children] = parseResponseOfCreate(nodes, 'goto');
        ru.push({
          type: RU_TYPE.ADD_NODE,
          changed,
          dependencies: {
            flowId,
            projectId,
            children,
          },
        });
      }
    },
    [flowId, mode, node, projectId]
  );
  // const userNodes = useMemo(() => getCanLinkUserNode(node), [node]);

  const handleMouseEnter = (id) => {
    document.querySelector(`.${id}`)?.classList.add('flow-goto-highlight');
  };
  const handleMouseLeave = (id) => {
    document.querySelector(`.${id}`)?.classList.remove('flow-goto-highlight');
  };

  const options = useGraphStore((s) => {
    const filters = findGotoFilterTargets(node);
    return s.nodes
      .filter(
        (n) => !['conversation', 'goto', 'condition', 'break'].includes(n.type)
      )
      .filter((n) => !filters.some((f) => f.id === n.id))
      .map((n) => ({
        id: n.id,
        label: getNodeLabel(n),
        icon: getNodeIcon(n),
      }));
  });
  return (
    <DrawerFormBoxTrigger
      title={t['title']}
      initialValues={initialValues}
      onFinish={onFinish}
      mode={mode}
      {...props}
    >
      <Form.Item
        label={t['form.linkId']}
        field="linkId"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <Select
          showSearch
          allowClear
          filterOption={(inputValue, option) =>
            option.props.extra
              .toLowerCase()
              .indexOf(inputValue.toLowerCase()) >= 0
          }
          prefix={t['form.linkId.prefix']}
        >
          {options.map((o) => {
            return (
              <Select.Option
                onMouseLeave={() => handleMouseLeave(o.id)}
                onMouseEnter={() => handleMouseEnter(o.id)}
                key={o.id}
                value={o.id}
                extra={o.label}
              >
                <div className="flex items-center gap-4">
                  {createElement(o.icon)}
                  <div className="truncate flex-1" title={o.label}>
                    {o.label}
                  </div>
                </div>
              </Select.Option>
            );
          })}
        </Select>
      </Form.Item>
      {/*<Form.Item label={t['form.description']} field="description">*/}
      {/*  <TextArea />*/}
      {/*</Form.Item>*/}
    </DrawerFormBoxTrigger>
  );
};

export interface CreateGotoDrawerTriggerProps
  extends Omit<
    GotoDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateGotoDrawerTrigger: React.FC<
  CreateGotoDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GotoDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<GotoIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateGotoDrawerTrigger: React.FC<
  Omit<GotoDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GotoDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
