import * as React from 'react';
import { useMemo, useRef } from 'react';
import View, { ViewProps } from '../../components/View';
import { NodeProps, NodeRule } from '../../types';
import { createNodeFetch, findGotoFilterTargets, getNodeLabel, updateNodeFetch } from '../../utils/node';
import { useNodeDrop } from '../../../dnd';
import { IconEdit } from '@arco-design/web-react/icon';
import MenuBox, { MenuBoxDivider } from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../../components/DeleteNodeTrigger';
import { DrawerFormBoxTrigger, DrawerFormBoxTriggerProps } from '../../../../../components/DrawerBox';
import { useGraphStore } from '../../../store/graph';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { Button, Form, FormInstance, Input } from '@arco-design/web-react';
import TextArea from '@/pages/projects/project/components/TextArea';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../../i18n';
import GraphCore from '@/core-next';
import { NodeDefined } from '@/core-next/types';
import { isEmpty, keyBy, omit } from 'lodash';
import Openai from '@/assets/openai_icon.svg';
import {
  CreateBotDrawerTrigger,
  CreateGotoDrawerTrigger,
  CreateUserDrawerTrigger,
  initialBotValues
} from '@/pages/projects/project/conversations/conversation/views';
import { MultiMappingFormItem, SlotsProvider } from '@pcom/multivariable';
import { nanoid } from 'nanoid';
import CodeList from './component/CodeList';

export const GptIcon = Openai;

const emptyRules = [];

export interface GptInnerViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  node: NodeProps;
}

export const GptInnerView: React.FC<GptInnerViewProps> = ({
                                                            node,
                                                            ...props
                                                          }) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, emptyRules);
  return (
    <View
      icon={<GptIcon />}
      id={node.id}
      firstComponent={node.first}
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

export const GptInnerNode: React.FC<NodeProps> = (props) => {
  const nodes = useGraphStore((s) =>
    s.nodes.filter((s) => !['conversation'].includes(s.type))
  );


  // 如果是 llmAgent，那么只会有一个gpt节点
  // 此时不需要各种按钮，编辑后 refresh 整个页面(同步刷新左侧 agent 菜单的名称）
  const inLlmAgent = useGraphStore().originNodes.length === 1;
  const notInLlmAgent = !inLlmAgent;
  const rules = useMemo<NodeRule[]>(() => {
    const emptyChildren = isEmpty(props.children);
    const filters = findGotoFilterTargets(props);
    const targets = nodes.filter((n) => !filters.some((f) => f.id === n.id));
    return [
      {
        key: 'user',
        show: !props.children?.some((n) => ['bot', 'break'].includes(n.type))
      },
      { key: 'bot', show: emptyChildren },
      {
        key: 'goto',
        show: targets.length > 0 && emptyChildren
      },
      { key: 'gpt', show: emptyChildren }
    ];
  }, [props, nodes]);
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);

  return (
    <div className="flex items-center">
      <MenuBox
        trigger={<GptInnerView node={props} />}
        validatorError={props.validatorError}
      >
        <UpdateGptDrawerTrigger node={props} reloadPage={true} />
        {notInLlmAgent && <DeleteNodeTrigger node={props} />}
        {notInLlmAgent && <MenuBoxDivider />}
        {notInLlmAgent && ruleMap['user'].show && <CreateUserDrawerTrigger parent={props} />}
        {notInLlmAgent && ruleMap['bot'].show && (
          <CreateBotDrawerTrigger
            parent={props}
            initialValues={initialBotValues}
          />
        )}
        {notInLlmAgent && ruleMap['goto'].show && <CreateGotoDrawerTrigger parent={props} />}
        {/*{ruleMap['gpt'].show && <CreateGptDrawerTrigger parent={props} />}*/}
        {notInLlmAgent && <MenuBoxDivider />}
        {/*<FavoriteNodeTrigger node={props} />*/}
      </MenuBox>
    </div>
  );
};

export interface GptViewProps {
  node: NodeProps;
}

export const GptView: React.FC<GptViewProps> = ({ node }) => {
  const { expand } = React.useContext(ExpandContext);
  const value = React.useMemo(
    () => [
      omit(node, ['children']),
      ...(expand ? node.data?.children || [] : [])
    ],
    [node, expand]
  );
  const nodesConfig = React.useMemo<Record<string, NodeDefined>>(
    () => ({
      gpt: {
        component: GptInnerNode,
        props: {
          nodeClassClassName: '!gap-0',
          childrenClassName:
            'relative border rounded bg-gray-50/50 pl-[80px] before:content-[\'Form\'] before:absolute before:top-0 before:left-2'
        }
      }
    }),
    []
  );
  return (
    <GraphCore
      name={`gpt-${node.id}`}
      width="100%"
      height="100%"
      value={value}
      nodes={nodesConfig}
      disabledMoveAndZoom
      canvasClassName="p-0"
    />
  );
};

export const GptNode: React.FC<NodeProps> = (props) => {
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
      <GptView node={props} />
    </ExpandContext.Provider>
  );
};

export interface GptDrawerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any>;
  reloadPage?: boolean;
}

export const GptDrawer: React.FC<GptDrawerProps> = ({
                                                      mode,
                                                      node,
                                                      initialValues: originInitialValues,
                                                      ...props
                                                    }) => {
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId
  }));
  const t = useLocale(i18n);
  const initialValues = React.useMemo(
    () => (mode === 'update' ? node.data : originInitialValues),
    [node, mode, originInitialValues]
  );
  const onFinish = React.useCallback(
    async (data) => {
      if (mode === 'update') {
        await updateNodeFetch(node, data, 'gpt', { projectId, flowId });
      }
      if (mode === 'create') {
        await createNodeFetch(node, data, 'gpt', { projectId, flowId });
      }
    },
    [flowId, mode, node, projectId]
  );

  const formRef = useRef<FormInstance>();

  const functionCalling = React.useMemo(() => {
    return initialValues?.functionCalling || [];
  }, [initialValues]);

  const slots = initialValues?.slots ? initialValues.slots.map((item) => {
    return {
      id: nanoid(),
      slotId: item.slotId
    };
  }) : [];

  /**
   * 将名称中的空格转换为下划线，提高输入体验，不用手动来回替换空格
   * @param value
   */
  const handleNameChange = (value: string) => {
    const formattedValue = value.replace(/\s/g, '_');
    formRef.current?.setFieldValue('name', formattedValue);
  };

  return (
    <DrawerFormBoxTrigger
      ref={formRef}
      title={t['title']}
      initialValues={initialValues}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item
        label={t['name']}
        field="name"
        rules={[
          { required: true, message: t['rule.required'] },
          {
            validator: async (value, callback) => {
              const regex = /^[a-zA-Z0-9_-]+$/; // 正则表达式

              // 空值检查
              if (!value) {
                callback(t['applying.form.name.required']);
                return;
              }

              // 正则检查
              if (!regex.test(value)) {
                callback(t['nameRule']);
                return;
              }

              // 验证通过
              callback(); // 继续验证
            }
          }
        ]}
        validateTrigger="onChange" // 在输入时触发验证
        extra={t['nameRule']}
      >
        <Input placeholder={t['name']} onChange={handleNameChange} />
      </Form.Item>
      <Form.Item
        label={t['description']}
        field="description"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <TextArea style={{ minHeight: 60 }} placeholder={t['description']} />
      </Form.Item>
      <Form.Item
        label={t['prompt']}
        field="prompt"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <TextArea style={{ minHeight: 350 }} placeholder={t['prompt']} />
      </Form.Item>
      <Form.Item
        label={t['slots']}
        field="slots"
      >
        <SlotsProvider needMap>
          <Form.Item shouldUpdate noStyle>
            <div className="mt-4 flex  flex-col">
              <span className={'mb-2'}>{t['extract.slots']}</span>
              <MultiMappingFormItem
                formRef={formRef}
                field="slots"
                multiple={true}
                partType={true}
                rules={[
                  {
                    minLength: 1,
                    message: t['form.mappings.rule']
                  }
                ]}
                initialMappings={slots}
              />
            </div>
          </Form.Item>
        </SlotsProvider>
      </Form.Item>
      <Form.Item
        label={t['function.calling']}
        field="functionCalling"
      >
        <div className={'flex flex-col w-full'}>
          <CodeList formRef={formRef} fileName={'functionCalling'} initialValue={functionCalling} />
        </div>
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateGptDrawerTriggerProps
  extends Omit<GptDrawerProps, 'mode' | 'trigger' | 'node' | 'refresh'> {
  parent: Partial<NodeProps>;
}

export const CreateGptDrawerTrigger: React.FC<CreateGptDrawerTriggerProps> = ({
                                                                                parent,
                                                                                ...props
                                                                              }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GptDrawer
        mode="create"
        node={parent}
        trigger={<Button icon={<GptIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateGptDrawerTrigger: React.FC<
  Omit<GptDrawerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const storeRefresh = useGraphStore((s) => s.refresh);

  const refresh = props.reloadPage
    ? () => window.location.reload()
    : storeRefresh;
  return (
    <Visible>
      <GptDrawer
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
