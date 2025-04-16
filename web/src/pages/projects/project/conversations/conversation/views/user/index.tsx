import { IconEdit, IconStar, IconUser, IconUserAdd } from '@arco-design/web-react/icon';
import React, { useCallback, useMemo, useRef, useState } from 'react';
import View from '../components/View';
import { NodeProps, NodeRule } from '../types';
import useLocale from '@/utils/useLocale';
import common from '../i18n';
import MenuBox, { MenuBoxDivider } from '@/pages/projects/project/components/MenuBox';
import { Alert, Button, Form, FormInstance, Input, Radio, RulesProps, Switch, Tag } from '@arco-design/web-react';
import { DrawerFormBoxTrigger, DrawerFormBoxTriggerProps } from '../../../../components/DrawerBox';
import { useGraphStore } from '../../store/graph';
import i18n from './i18n';
import { createComponent, updateComponent } from '@/api/components';
import {
  computeParentForm,
  findGotoFilterTargets,
  getBreakCount,
  getNodeLabel,
  parseResponseOfCreate,
  unwrap
} from '../utils/node';
import useMarkEnable from './hook/useMarkEnable';
import Example from './components/Example';
import useEntities from './hook/useEntities';
import { isBlank } from '@/utils/is';
import { createMapping, unwrapFormValues, wrapFormValues } from './utils';
import { MainExampleExtraProps } from './components/MainExampleExtra';
import { InputWayEnum } from './enums';
import {
  MultiMappingFormItem,
  SlotsProvider,
  useSlotsContext
} from '@/pages/projects/project/components/multivariable';
import TextArea from '@/pages/projects/project/components/TextArea';
import SetSlotsFormItem, { SetSlotsFormItemProps } from '@/pages/projects/project/components/SetSlotsFormItem';
import ExamplesFormItem from './components/ExamplesFormItem';
import { isEmpty, keyBy } from 'lodash';
import ExamplesExtra from './components/ExamplesExtra';
import ru, { RU_TYPE } from '../../features/ru';
import CustomLableFormItem from '../components/CustomLabelFormItem';
import { CreateBotDrawerTrigger, initialBotValues } from '../bot';
import { IDName } from '@/graph-next/type';
import Visible from '@/pages/projects/project/components/Visible';
import LinkedFromExtra from '../components/LinkedFromTopExtra';
import { VscSymbolField } from 'react-icons/vsc';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import CopyNodeTrigger from '../components/CopyNodeTrigger';
import { useNodeDrop } from '../../dnd';
import { CreateBreakDrawerTrigger } from '../break';
import randomColor from '@/utils/randomColor';
import { useProjectType } from '@/layout/project-layout/context';
import { CreateGotoDrawerTrigger } from '../goto';
import { CreateGptDrawerTrigger } from '../gpt/gpt';

export const UserIcon = IconUser;

export interface UserViewProps {
  rules: NodeRule[];
  node: NodeProps;
}
export const UserView: React.FC<UserViewProps> = ({
  rules,
  node,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  const variables = useMemo(() => {
    if (!node.data.mappingsEnable || isEmpty(node.data.mappings)) return null;
    return node.data.mappings.map(({ id, slotDisplay, slotName }) => ({
      id,
      name: slotDisplay || slotName,
    }));
  }, [node.data.mappings, node.data.mappingsEnable]);
  const dropProps = useNodeDrop(node, rules);
  return (
    <View
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      topExtra={
        node.linkedFrom && <LinkedFromExtra linkedFrom={node.linkedFrom} />
      }
      bottomExtra={variables && <VariableExtra variables={variables} />}
      icon={<UserIcon />}
      {...props}
      {...dropProps}
    />
  );
};

interface VariableExtraProps {
  variables: any[];
}
const VariableExtra: React.FC<VariableExtraProps> = ({ variables }) => {
  return (
    <div className="space-x-1 flex items-center">
      {variables.map((v) => (
        <div
          key={v.id}
          className="bg-blue-100 text-blue-500 leading-none text-sm p-[2px] flex-shrink-0 rounded"
          title={v.name}
        >
          <VscSymbolField />
          {v.name}
        </div>
      ))}
    </div>
  );
};

export const UserNode: React.FC<NodeProps> = (props) => {
  const type = useProjectType();
  const nodes = useGraphStore((s) =>
    s.nodes.filter((s) => !['conversation'].includes(s.type))
  );
  const rules = useMemo<NodeRule[]>(() => {
    const form = computeParentForm(props);
    const emptyChildren = isEmpty(props.children);
    const filters = findGotoFilterTargets(props);
    const targets = nodes.filter((n) => !filters.some((f) => f.id === n.id));
    return [
      {
        key: 'bot',
        show:
          !props.afterRhetorical &&
          !props.children?.some((n) => n.type !== 'bot'),
      },
      {
        key: 'goto',
        show: targets.length > 0 && emptyChildren,
      },
      {
        key: 'form',
        show: isEmpty(props.children) && isEmpty(props.afterRhetorical),
      },
      {
        key: 'form-gpt',
        show: isEmpty(props.children) && isEmpty(props.afterRhetorical),
      },
      {
        key: 'break',
        show: isEmpty(props.children) && isEmpty(props.afterRhetorical) && form,
      },
      {
        key: 'gpt',
        show: emptyChildren
      },

    ];
  }, [nodes, props]);
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  const showDivider = useMemo(
    () => Boolean(rules.map((r) => r.show).filter(Boolean).length),
    [rules]
  );
  const initialValues = useCallback(() => {
    const form = computeParentForm(props);
    const breakCount = getBreakCount(form);
    return {
      name: `break-${breakCount + 1}`,
      color: randomColor(),
      formId: form.id,
    };
  }, [props]);
  return (
    <MenuBox
      trigger={<UserView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateUserDrawerTrigger node={props} />
      <CopyNodeTrigger node={props} />
      <DeleteNodeTrigger node={props} />
      <MenuBoxDivider />
      {ruleMap['bot'].show && (
        <CreateBotDrawerTrigger
          parent={props}
          initialValues={initialBotValues}
        />
      )}
      {ruleMap['goto'].show && <CreateGotoDrawerTrigger parent={props} />}
      {/*{type === 'rasa' && ruleMap['form'].show && (*/}
      {/*  <CreateFormDrawerTrigger parent={props} />*/}
      {/*)}*/}
      {/*{type === 'llm' && ruleMap['form'].show && (*/}
      {/*  <CreateGptFormDrawerTrigger parent={props} />*/}

      {/*)}*/}

      {type === 'llm' && ruleMap['gpt'].show && <CreateGptDrawerTrigger parent={props} />}

      {ruleMap['break'].show && (
        <CreateBreakDrawerTrigger
          parent={props}
          initialValues={initialValues}
        />
      )}
      {showDivider && <MenuBoxDivider />}
       {/*<FavoriteNodeTrigger node={props} />*/}
    </MenuBox>
  );
};

const SetSlots: React.FC<Omit<SetSlotsFormItemProps, 'slots'>> = (props) => {
  const { slots } = useSlotsContext();
  return <SetSlotsFormItem slots={slots} {...props} />;
};

export interface UserDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any> | (() => Partial<any>);
  afterRhetorical?: string;
}
export const UserDrawerTrigger: React.FC<UserDrawerTriggerProps> = ({
  mode,
  node,
  initialValues: originInitialValues,
  afterRhetorical,
  ...props
}) => {
  const formRef = useRef<FormInstance>();
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));
  const t = useLocale(i18n);
  const initialValues = useMemo(() => {
    if (mode === 'update') return wrapFormValues(node.data);
    if (typeof originInitialValues === 'function')
      return wrapFormValues(originInitialValues());
    return wrapFormValues(originInitialValues as any);
  }, [mode, node.data, originInitialValues]);

  const [markEnable, onComputeMarkEnableChange] = useMarkEnable(initialValues);
  const [entities, setEntities] = useEntities(initialValues);
  const [linkedFrom, setLinkedFrom] = useState<IDName>(() =>
    mode === 'update' ? node.linkedFrom : null
  );
  const [base, setBase] = useState(initialValues?.mainExample?.text || '');

  const exampleRules = useMemo<RulesProps[]>(
    () => [
      {
        validator(value, callback) {
          if (!value || isBlank(value.text)) {
            callback(t['rule.required']);
          }
        },
      },
    ],
    [t]
  );
  const displayOptions = useMemo(
    () =>
      Object.entries(InputWayEnum).map(([k, v]) => ({
        label: t[`form.display.options.${k}`],
        value: v,
      })),
    [t]
  );

  const onChangeLinkFrom = useCallback<MainExampleExtraProps['onLinkedFrom']>(
    (data, linkedFrom) => {
      formRef.current.setFieldsValue(wrapFormValues(data));
      setLinkedFrom(linkedFrom);
    },
    []
  );
  const onValuesChange = useCallback<
    DrawerFormBoxTriggerProps['onValuesChange']
  >(
    (field, values) => {
      if (field.mappingsEnable !== undefined) {
        onComputeMarkEnableChange(values.mappings, values.mappingsEnable);
        if (isEmpty(formRef.current.getFieldValue('mappings'))) {
          // init a mapping
          formRef.current.setFieldValue('mappings', [createMapping()]);
        }
      }
      if (
        field.mappings !== undefined ||
        Object.keys(field).some((k) => k.startsWith('mappings['))
      ) {
        onComputeMarkEnableChange(values.mappings, values.mappingsEnable);
      }
      if (field.mainExample !== undefined) {
        setBase(field.mainExample.text);
      }
    },
    [onComputeMarkEnableChange]
  );
  const onFinish = useCallback(
    async (data) => {
      data = unwrapFormValues(data);
      if (mode === 'update') {
        const after = await updateComponent(projectId, 'user', node.id, {
          ...unwrap(node),
          data,
          linkedFrom,
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
        const nodes = await createComponent(projectId, 'user', {
          data,
          parentId: node.id,
          linkedFrom,
          afterRhetorical,
        });
        const [changed, children] = parseResponseOfCreate(nodes, 'user');
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
    [afterRhetorical, flowId, linkedFrom, mode, node, projectId]
  );

  return (
    <DrawerFormBoxTrigger
      ref={formRef}
      title={
        <div className="space-x-2">
          <span>{t['title']}</span>
          {linkedFrom && (
            <Tag size="small" color="orange" icon={<IconStar />}>
              {linkedFrom.name}
            </Tag>
          )}
        </div>
      }
      initialValues={initialValues}
      onFinish={onFinish}
      onValuesChange={onValuesChange}
      {...props}
    >
      {/*<Form.Item label={t['form.name']} field="name">*/}
      {/*  <Input placeholder={t['form.name']} />*/}
      {/*</Form.Item>*/}
      {/*<Form.Item label={t['form.description']} field="description">*/}
      {/*  <TextArea placeholder={t['form.description']} />*/}
      {/*</Form.Item>*/}
      <CustomLableFormItem
        label={t['form.examples.0']}
        // label={
        //   <MainExampleExtra
        //     id={node.id}
        //     label={t['form.examples.0']}
        //     linkedFrom={linkedFrom}
        //     formRef={formRef}
        //     onLinkedFrom={onChangeLinkFrom}
        //     mode={mode}
        //   />
        // }
        field="mainExample"
        required
        rules={exampleRules}
      >
        <Example
          markEnable={markEnable}
          entities={entities}
          placeholder={t['form.examples.0']}
        />
      </CustomLableFormItem>
      {/*<Form.Item*/}
      {/*  label={t['form.display']}*/}
      {/*  field="display"*/}
      {/*  rules={[{ required: true, message: t['rule.required'] }]}*/}
      {/*>*/}
      {/*  <Radio.Group options={displayOptions} />*/}
      {/*</Form.Item>*/}
      <Alert
        content={t['form.mappingsEnable']}
        action={
          <Form.Item field="mappingsEnable" noStyle triggerPropName="checked">
            <Switch size="small" type="round" />
          </Form.Item>
        }
      />
      <SlotsProvider needMap>
        <Form.Item shouldUpdate noStyle>
          {({ mappingsEnable }) => (
            <div className="mt-4">
              {mappingsEnable && (
                <MultiMappingFormItem
                  formRef={formRef}
                  field="mappings"
                  multiple={!afterRhetorical}
                  partType={!!afterRhetorical}
                  rules={[
                    {
                      minLength: 1,
                      message: t['form.mappings.rule'],
                    },
                  ]}
                  initialMappings={node.data?.mappings}
                  onFromEntityMappingsChange={setEntities}
                />
              )}
            </div>
          )}
        </Form.Item>
        {/*<CustomLableFormItem*/}
        {/*  label={*/}
        {/*    <ExamplesExtra*/}
        {/*      label={t['form.examples']}*/}
        {/*      formRef={formRef}*/}
        {/*      generateProps={{ disabled: !base }}*/}
        {/*    />*/}
        {/*  }*/}
        {/*  field="examples"*/}
        {/*>*/}
        {/*  <Form.List field="examples">*/}
        {/*    {(fields, operation) => (*/}
        {/*      <ExamplesFormItem*/}
        {/*        fields={fields}*/}
        {/*        operation={operation}*/}
        {/*        markEnable={markEnable}*/}
        {/*        entities={entities}*/}
        {/*        placeholder={t['form.examples.placeholder']}*/}
        {/*      />*/}
        {/*    )}*/}
        {/*  </Form.List>*/}
        {/*</CustomLableFormItem>*/}

        <Form.Item label={t['form.setSlots']}>
          <Form.List field="setSlots">
            {(fields, operation) => (
              <SetSlots fields={fields} operation={operation} />
            )}
          </Form.List>
        </Form.Item>
      </SlotsProvider>
      <Form.Item field="name" className="!hidden">
        <Input />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateUserDrawerTriggerProps
  extends Omit<
    UserDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateUserDrawerTrigger: React.FC<
  CreateUserDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <UserDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<IconUserAdd />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateUserDrawerTrigger: React.FC<
  Omit<UserDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <UserDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
