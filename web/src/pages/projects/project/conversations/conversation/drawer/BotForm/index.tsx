import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphBot } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useLocale from '@/utils/useLocale';
import { Form, Input, Space, Spin, Tag } from '@arco-design/web-react';
import { IconStar } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import React, { Ref, useImperativeHandle, useMemo } from 'react';
import { SelectionProps } from '../../types';
import i18n from './locale';
import { updateResponse } from '../operator';
import { ComponentHandle } from '../type';
import LinkShare from './LinkShare';
import Share from './Share';
import { useSlots } from './hook';
import { BotResponseFormItem } from '@/pages/projects/project/components/BotResponseFormItem';
import ConditionsFormItem from '@/pages/projects/project/components/ConditionsFormItem';
import SetSlotsFormItem from '@/pages/projects/project/components/SetSlotsFormItem';
import ReplyVariables from '@/pages/projects/project/components/ReplyVariables';

const { Item } = Form;
const BotForm = (
  {
    projectId,
    selection,
    onChange,
    onChangeEditSelection,
  }: SelectionProps<GraphBot>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);
  const baseResponseRules = useMemo(
    () => [
      {
        required: true,
        minLength: 1,
        message: t['conversation.botForm.rule.base'],
      },
    ],
    [t]
  );
  const formRef = useFormRef(selection.data);

  const onValuesChange = (_, values) => {
    onChangeEditSelection({
      ...selection,
      data: values,
    });
  };
  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        await formRef.current.validate();
        const { id, relations, parentId, data, linkedFrom } = selection;
        const webhookIds = data.responses
          .filter((r) => r.type === 'webhook')
          .map((r) => (r.content as any).id);
        return updateResponse({
          projectId,
          id,
          parentId,
          relations: Array.from(new Set([...relations, ...webhookIds])),
          data,
          linkedFrom,
          callback: (node) => {
            onChange((vals) =>
              ObjectArrayHelper.update(vals, node, (f) => f.id === id)
            );
          },
        });
      },
    }),
    [formRef, onChange, projectId, selection]
  );

  const isGlobal = useMemo(
    () => !isEmpty(selection.linkedFrom),
    [selection.linkedFrom]
  );
  const handleShare = (nodeData, linkedFrom) => {
    onChangeEditSelection({ ...selection, data: nodeData, linkedFrom });
  };
  const { loading, slots } = useSlots(projectId);
  const replyOptions = useMemo(
    () =>
      slots?.map(({ id, name, display }) => ({
        label: display || name,
        value: id,
      })) || [],
    [slots]
  );
  return (
    <Spin loading={loading} className="w-full">
      <Form
        layout="vertical"
        ref={formRef}
        initialValues={selection.data}
        onValuesChange={onValuesChange}
      >
        <Item
          label={
            <div className="inline-flex justify-center items-center w-[90%]">
              <span>{t['conversation.botForm.title']}</span>
              <Space>
                {selection.linkedFrom && (
                  <Tag size="small" color="orange" icon={<IconStar />}>
                    {selection.linkedFrom.name}
                  </Tag>
                )}
                <LinkShare
                  nodeId={selection.id}
                  share={selection.linkedFrom}
                  onChange={handleShare}
                />
                {!isGlobal && (
                  <Share
                    nodeId={selection.id}
                    share={selection.linkedFrom}
                    onChange={handleShare}
                    valueFormRef={formRef}
                  />
                )}
              </Space>
            </div>
          }
          required
          field="responses"
          rules={baseResponseRules}
          onChange={() => {
            formRef.current.validate();
          }}
        >
          <Form.List field="responses">
            {(fields, operation) => (
              <BotResponseFormItem
                fields={fields}
                operation={operation}
                responses={selection.data.responses}
              />
            )}
          </Form.List>
        </Item>
        <Item label={t['conversation.botForm.description']} field="description">
          <Input.TextArea
            autoSize
            placeholder={t['conversation.botForm.description.placeholder']}
          />
        </Item>
        <Item
          label={t['conversation.botForm.more.actions.condition']}
          extra={t['conversation.botForm.more.actions.condition.help']}
        >
          <Form.List field="conditions">
            {(fields, operation) => (
              <ConditionsFormItem
                fields={fields}
                operation={operation}
                slots={slots}
              />
            )}
          </Form.List>
        </Item>
        <Item
          label={t['conversation.botForm.more.actions.reset']}
          extra={t['conversation.botForm.more.actions.reset.help']}
        >
          <Form.List field="setSlots">
            {(fields, operation) => (
              <SetSlotsFormItem
                fields={fields}
                operation={operation}
                slots={slots}
              />
            )}
          </Form.List>
        </Item>
        <Item
          label={t['conversation.botForm.variable.reply.list']}
          field="entityReplies"
        >
          <ReplyVariables options={replyOptions} />
        </Item>
      </Form>
    </Spin>
  );
};

export default BotForm;
