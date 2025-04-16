import * as React from 'react';
import { TbMathFunction } from 'react-icons/tb';
import View, { ViewProps } from '../../components/View';
import { NodeProps } from '../../types';
import {
  createNodeFetch,
  getNodeLabel,
  updateNodeFetch,
} from '../../utils/node';
import { useNodeDrop } from '../../../dnd';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../../components/DeleteNodeTrigger';
import FavoriteNodeTrigger from '../../components/FavoriteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../../components/DrawerBox';
import {
  Button,
  Card,
  Divider,
  Form,
  FormInstance,
  Input,
  Select,
} from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { useGraphStore } from '../../../store/graph';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../../i18n';
import { IconClose, IconEdit, IconPlus } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import { listSlotComponent } from '@/api/components';
import TextArea from '@/pages/projects/project/components/TextArea';
import { findParentForm } from '../../../util';
import { isEmpty, keyBy } from 'lodash';

export const GptFunctionIcon = TbMathFunction;

const emptyRules = [];
export interface GptFunctionViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const GptFunctionView: React.FC<GptFunctionViewProps> = ({
  node,
  ...props
}) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, emptyRules);
  return (
    <View
      icon={<GptFunctionIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const GptFunctionNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox
      trigger={<GptFunctionView node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateGptFunctionDrawerTrigger node={props} />
      <DeleteNodeTrigger node={props} />
      <MenuBoxDivider />
       {/*<FavoriteNodeTrigger node={props} />*/}
    </MenuBox>
  );
};

const FUNCTION_NAME_REGX = /^[a-zA-Z0-9_-]{1,64}$/;

export interface GptFunctionDrawerTriggerProps
  extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
}
export const GptFunctionDrawerTrigger: React.FC<
  GptFunctionDrawerTriggerProps
> = ({ mode, node, ...props }) => {
  const formRef = React.useRef<FormInstance>();
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));

  const onFinish = React.useCallback(
    async (data) => {
      if (mode === 'update') {
        await updateNodeFetch(node, data, 'function-gpt', {
          projectId,
          flowId,
        });
      }
      if (mode === 'create') {
        await createNodeFetch(node, data, 'function-gpt', {
          flowId,
          projectId,
        });
      }
    },
    [flowId, node, projectId, mode]
  );
  const { loading, data } = useRequest(() => listSlotComponent(projectId), {
    refreshDeps: [projectId],
  });
  const definedOptions = React.useMemo(() => {
    const form = findParentForm(node);
    const slots = form.children?.find((c) => c.type === 'slots-gpt');
    if (slots?.children) {
      const origins =
        data?.map(({ id, display, name }) => ({
          label: display || name,
          value: id,
          disabled: false,
        })) || [];
      const definedSlots = keyBy(
        slots.children.map((s) => ({ value: s.data?.slotId })).filter(Boolean),
        'value'
      );
      return origins.filter((o) => definedSlots[o.value]);
    }
    return [];
  }, [data, node]);

  return (
    <DrawerFormBoxTrigger
      ref={formRef}
      title={t['title']}
      initialValues={node.data}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item
        label={t['form.name']}
        field="name"
        rules={[
          { required: true, message: t['rule.required'] },
          {
            validator(value, callback) {
              if (isEmpty(value)) callback(t['rule.required']);
              if (!FUNCTION_NAME_REGX.test(value))
                callback(t['rule.function.name']);
            },
          },
        ]}
      >
        <Input placeholder={t['form.name']} />
      </Form.Item>
      <Form.Item
        label={t['form.description']}
        field="description"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <TextArea style={{ minHeight: 128 }} />
      </Form.Item>
      <Divider>{t['form.params']}</Divider>

      <Form.Item shouldUpdate noStyle>
        {({ params }) => {
          const used = keyBy(params, 'slotId');
          const options = definedOptions.map((d) => ({
            ...d,
            disabled: !!used[d.value],
          }));
          const disabledAdd = params?.length === definedOptions.length;
          return (
            <Form.List
              field="params"
              rules={[
                {
                  required: true,
                  type: 'array',
                  min: 1,
                  message: t['rule.array.minLenght'],
                },
              ]}
            >
              {(fields, { add, remove }) => (
                <div className="space-y-2">
                  {fields.map((field) => (
                    <Card
                      title={t['form.param'] + (field.key + 1)}
                      size="small"
                      key={field.key}
                      extra={
                        <Button
                          shape="circle"
                          status="danger"
                          type="secondary"
                          icon={<IconClose />}
                          onClick={() => remove(field.key)}
                        />
                      }
                    >
                      <Form.Item
                        label={t['form.param.name']}
                        field={field.field + 'slotId'}
                        rules={[
                          { required: true, message: t['rule.required'] },
                        ]}
                      >
                        <Select loading={loading} options={options} />
                      </Form.Item>
                      <Form.Item
                        label={t['form.param.description']}
                        field={field.field + 'description'}
                        rules={[
                          { required: true, message: t['rule.required'] },
                        ]}
                      >
                        <TextArea style={{ minHeight: 64 }} />
                      </Form.Item>
                    </Card>
                  ))}
                  <Button
                    type="outline"
                    status="success"
                    long
                    icon={<IconPlus />}
                    onClick={() => add()}
                    disabled={disabledAdd}
                  >
                    {t['form.params.add']}
                  </Button>
                </div>
              )}
            </Form.List>
          );
        }}
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateGptFunctionDrawerTriggerProps
  extends Omit<
    GptFunctionDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateGptFunctionDrawerTrigger: React.FC<
  CreateGptFunctionDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GptFunctionDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<GptFunctionIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateGptFunctionDrawerTrigger: React.FC<
  Omit<GptFunctionDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <GptFunctionDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
