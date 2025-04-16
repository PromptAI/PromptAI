import * as React from 'react';
import View, { ViewProps } from '../../components/View';
import { NodeProps } from '../../types';
import {
  createNodeFetch,
  getNodeLabel,
  updateNodeFetch,
} from '../../utils/node';
import { useNodeDrop } from '../../../dnd';
import { IconDriveFile, IconEdit, IconPlus } from '@arco-design/web-react/icon';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import FavoriteNodeTrigger from '../../components/FavoriteNodeTrigger';
import DeleteNodeTrigger from '../../components/DeleteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../../components/DrawerBox';
import { useGraphStore } from '../../../store/graph';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { Button, Form, Input } from '@arco-design/web-react';
import TextArea from '@/pages/projects/project/components/TextArea';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../../i18n';
import GraphCore from '@/core-next';
import { NodeDefined } from '@/core-next/types';
import { GptFunctionsNode } from '../gpt-functions';
import { GptFunctionNode } from '../gpt-function';
import { GptSlotsNode } from '../gpt-slots';
import { GptSlotNode } from '../gpt-slot';
import { omit } from 'lodash';
import { AiOutlineMinusSquare, AiOutlinePlusSquare } from 'react-icons/ai';

export const GptFormIcon = IconDriveFile;

const emptyRules = [];
export interface GptFormInnerViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  node: NodeProps;
}
export const GptFormInnerView: React.FC<GptFormInnerViewProps> = ({
  node,
  ...props
}) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, emptyRules);
  return (
    <View
      icon={<GptFormIcon className={'app-icon'}/>}
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

export const GptFormInnerNode: React.FC<NodeProps> = (props) => {
  const { expand, setExpand } = React.useContext(ExpandContext);
  return (
    <div className="flex items-center">
      <MenuBox
        trigger={<GptFormInnerView node={props} />}
        validatorError={props.validatorError}
      >
        <UpdateGptFormDrawerTrigger node={props} />
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

export interface GptFormViewProps {
  node: NodeProps;
}
export const GptFormView: React.FC<GptFormViewProps> = ({ node }) => {
  const { expand } = React.useContext(ExpandContext);
  const value = React.useMemo(
    () => [
      omit(node, ['children']),
      ...(expand ? node.data?.children || [] : []),
    ],
    [node, expand]
  );
  const nodesConfig = React.useMemo<Record<string, NodeDefined>>(
    () => ({
      'functions-gpt': { component: GptFunctionsNode },
      'function-gpt': { component: GptFunctionNode },
      'slots-gpt': { component: GptSlotsNode },
      'slot-gpt': { component: GptSlotNode },
      'form-gpt': {
        component: GptFormInnerNode,
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
      name={`form-gpt-${node.id}`}
      width="100%"
      height="100%"
      value={value}
      nodes={nodesConfig}
      disabledMoveAndZoom
      canvasClassName="p-0"
    />
  );
};

export const GptFormNode: React.FC<NodeProps> = (props) => {
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
      <GptFormView node={props} />
    </ExpandContext.Provider>
  );
};

export interface GptFormDrawerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any>;
}
export const GptFormDrawer: React.FC<GptFormDrawerProps> = ({
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
    [node, mode, originInitialValues]
  );
  const onFinish = React.useCallback(
    async (data) => {
      if (mode === 'update') {
        await updateNodeFetch(node, data, 'form-gpt', { projectId, flowId });
      }
      if (mode === 'create') {
        await createNodeFetch(node, data, 'form-gpt', { projectId, flowId });
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
      <Form.Item
        label={t['form.name']}
        field="name"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <Input placeholder={t['form.name']} />
      </Form.Item>
      <Form.Item
        label={t['form.description']}
        field="description"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <TextArea
          style={{ minHeight: 520 }}
          placeholder={t['form.description']}
        />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateGptFormDrawerTriggerProps
  extends Omit<GptFormDrawerProps, 'mode' | 'trigger' | 'node' | 'refresh'> {
  parent: Partial<NodeProps>;
}
export const CreateGptFormDrawerTrigger: React.FC<
  CreateGptFormDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GptFormDrawer
        mode="create"
        node={parent}
        trigger={<Button icon={<GptFormIcon className={'app-icon'}/>}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateGptFormDrawerTrigger: React.FC<
  Omit<GptFormDrawerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GptFormDrawer
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
