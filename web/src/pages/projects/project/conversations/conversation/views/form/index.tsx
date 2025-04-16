import * as React from 'react';
import View, { ViewProps } from '../components/View';
import { IconEdit, IconFile } from '@arco-design/web-react/icon';
import { NodeProps } from '../types';
import { getNodeLabel, parseResponseOfCreate, unwrap } from '../utils/node';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import FavoriteNodeTrigger from '../components/FavoriteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { Button, Form, Input } from '@arco-design/web-react';
import TextArea from '@/pages/projects/project/components/TextArea';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../i18n';
import { useGraphStore } from '../../store/graph';
import { createComponent, updateComponent } from '@/api/components';
import ru, { RU_TYPE } from '../../features/ru';
import { useNodeDrop } from '../../dnd';
import GraphCore from '@/core-next';
import { AiOutlineMinusSquare, AiOutlinePlusSquare } from 'react-icons/ai';
import { NodeDefined } from '@/core-next/types';
import { SlotsNode } from '../slots';
import { InterruptNode } from '../interrupt';
import { FieldNode } from '../field';
import { RhetoricalNode } from '../rhetorical';
import { BreakNode } from '../break';
import { UserNode } from '../user';
import { BotNode } from '../bot';
import { GotoNode } from '../goto';
import { omit } from 'lodash';

export const FormIcon = IconFile;

const emptyRules = [];
export interface FormInnerViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  node: NodeProps;
}
export const FormInnerView: React.FC<FormInnerViewProps> = ({
  node,
  ...props
}) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, emptyRules);
  return (
    <View
      icon={<FormIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

interface ExpandContextValue {
  expand: boolean;
  setExpand: React.Dispatch<React.SetStateAction<boolean>>;
}
const ExpandContext = React.createContext<ExpandContextValue | undefined>(
  undefined
);

export const FormInnerNode: React.FC<NodeProps> = (props) => {
  const { expand, setExpand } = React.useContext(ExpandContext);
  return (
    <div className="flex items-center">
      <MenuBox
        trigger={<FormInnerView node={props} />}
        validatorError={props.validatorError}
      >
        <UpdateFormDrawerTrigger node={props} />
        <DeleteNodeTrigger node={props} />
        <MenuBoxDivider />
         {/*<FavoriteNodeTrigger node={props} />*/}
      </MenuBox>
      {expand && (
        <AiOutlineMinusSquare
          className="ml-1 mr-2 w-6 h-6 hover:text-blue-600 hover:ring-1 rounded-sm cursor-pointer"
          title="expand"
          onClick={() => setExpand((e) => !e)}
        />
      )}
      {!expand && (
        <AiOutlinePlusSquare
          className="ml-1 mr-2 w-6 h-6 hover:text-blue-600 hover:ring-1 rounded-sm cursor-pointer"
          title="expand"
          onClick={() => setExpand((e) => !e)}
        />
      )}
    </div>
  );
};

export interface FormViewProps {
  node: NodeProps;
}
export const FormView: React.FC<FormViewProps> = ({ node }) => {
  const { expand } = React.useContext(ExpandContext);
  const value = React.useMemo(
    () => [
      omit(node, ['children']),
      ...(expand ? node.data?.children || [] : []),
    ],
    [node, expand]
  );
  const nodesConfigs = React.useMemo<Record<string, NodeDefined>>(
    () => ({
      slots: { component: SlotsNode },
      interrupt: { component: InterruptNode },
      field: { component: FieldNode },
      rhetorical: { component: RhetoricalNode },
      break: { component: BreakNode },
      user: { component: UserNode },
      bot: { component: BotNode },
      goto: { component: GotoNode },
      form: {
        component: FormInnerNode,
        props: {
          nodeClassClassName: '!gap-0',
          childrenClassName:
            "relative border rounded bg-gray-50/50 pl-[80px] before:content-['Form'] before:absolute before:top-0 before:left-2",
        },
      },
    }),
    []
  );
  return (
    <GraphCore
      name={`form-${node.id}`}
      width="100%"
      height="100%"
      value={value}
      nodes={nodesConfigs}
      disabledMoveAndZoom
      canvasClassName="p-0"
    />
  );
};

export const FormNode: React.FC<NodeProps> = (props) => {
  const refreshNodes = useGraphStore((s) => s.refreshNodes);
  const [expand, setExpand] = React.useState(true);
  const oldExpandRef = React.useRef(expand);
  React.useEffect(() => {
    if (oldExpandRef.current !== expand) {
      setTimeout(() => {
        oldExpandRef.current = expand;
        // recompute position
        refreshNodes();
      });
    }
  }, [expand, refreshNodes]);
  return (
    <ExpandContext.Provider
      value={React.useMemo(() => ({ expand, setExpand }), [expand])}
    >
      <FormView node={props} />
    </ExpandContext.Provider>
  );
};

export interface FormDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any>;
}
export const FormDrawerTrigger: React.FC<FormDrawerTriggerProps> = ({
  mode,
  node,
  initialValues: originInitialValues,
  ...props
}) => {
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));
  const t = useLocale(i18n);
  const initialValues = React.useMemo(
    () => (mode === 'update' ? node.data : originInitialValues),
    [mode, node.data, originInitialValues]
  );

  const onFinish = React.useCallback(
    async (data) => {
      if (mode === 'update') {
        const after = await updateComponent(projectId, 'form', node.id, {
          ...unwrap(node),
          data,
        });
        ru.push({
          type: RU_TYPE.UPDATE_NODE,
          changed: {
            after,
            before: unwrap(node),
          },
          dependencies: {
            flowId,
            projectId,
          },
        });
      }
      if (mode === 'create') {
        const nodes = await createComponent(projectId, 'form', {
          data,
          parentId: node.id,
        });
        const [changed, children] = parseResponseOfCreate(nodes, 'form');
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
  return (
    <DrawerFormBoxTrigger
      title={t['title']}
      initialValues={initialValues}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item label={t['form.name']} field="name">
        <Input placeholder={t['form.name']} />
      </Form.Item>
      <Form.Item label={t['form.description']} field="description">
        <TextArea placeholder={t['form.description']} />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateFormDrawerTriggerProps
  extends Omit<
    FormDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateFormDrawerTrigger: React.FC<
  CreateFormDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <FormDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<FormIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
export const UpdateFormDrawerTrigger: React.FC<
  Omit<FormDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <FormDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
