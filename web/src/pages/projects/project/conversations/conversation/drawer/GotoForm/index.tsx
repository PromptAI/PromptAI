import { GraphGoto } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { Form, Input, Select, Space, Typography } from '@arco-design/web-react';
import React, { useMemo, Ref, useImperativeHandle } from 'react';
import { SelectionProps } from '../../types';
import { updateGoto } from '../operator';
import { ComponentHandle } from '../type';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { IconUser } from '@arco-design/web-react/icon';

const { Item } = Form;

const findRoot = (node) => {
  let p = node;
  while (p.parent) {
    p = p.parent;
  }

  return p;
};

// // 包含其他bot节点
// const findClosestNode = (node) => {
//   const parentPathNodes = [];
//   let p = node;
//   while ((p = p.parent)) {
//     parentPathNodes.push(p);
//     if (['user', 'option'].includes(p.type)) {
//       return [...parentPathNodes, p];
//     }
//   }
//   return [];
// };

const findClosestUserNode = (node) => {
  let p = node;
  while ((p = p.parent)) {
    if (['user', 'option'].includes(p.type)) return p;
  }
  return null;
};

const getAllNodes = (node) => {
  const root = findRoot(node);
  const stack = [root],
    result = [];
  let n;
  while ((n = stack.pop())) {
    if (n.children) {
      stack.unshift(...n.children);
      result.push(n);
    }
  }

  return result;
};

// export const getCanLinkNode = (node) => {
//   const closestNodes = findClosestNode(node).map((c) => c.id);
//   const list = getAllNodes(node);
//   return list.filter(
//     (n) =>
//       ['user', 'option', 'bot'].includes(n.type) && !closestNodes.includes(n.id)
//   );
// };
export const getCanLinkUserNode = (node) => {
  const closestUserNode = findClosestUserNode(node);
  const list = getAllNodes(node);
  return list.filter(
    (n) => ['user', 'option'].includes(n.type) && n.id !== closestUserNode.id
  );
};

export default (
  {
    projectId,
    selection,
    onChange,
    onChangeEditSelection,
  }: SelectionProps<GraphGoto>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);
  const formRef = useFormRef(selection.data);
  const userNodes = useMemo(() => getCanLinkUserNode(selection), [selection]);
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
        const { id, relations, parentId, data } = selection;
        return updateGoto({
          projectId,
          id,
          parentId,
          relations,
          data,
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
  const handleMouseEnter = (id) => {
    document.querySelector(`.${id}`).classList.add('flow-goto-highlight');
  };
  const handleMouseLeave = (id) => {
    document.querySelector(`.${id}`).classList.remove('flow-goto-highlight');
  };
  return (
    <Form
      layout="vertical"
      ref={formRef}
      initialValues={selection.data}
      onValuesChange={onValuesChange}
    >
      <Item label={t['goto.link']} field="linkId" rules={[{ required: true }]}>
        <Select
          showSearch
          allowClear
          filterOption={(inputValue, option) =>
            option.props.extra
              .toLowerCase()
              .indexOf(inputValue.toLowerCase()) >= 0
          }
          prefix={t['goto.link.prefix']}
        >
          {userNodes.map((o) => {
            const label =
              o.data.examples?.[0]?.text ||
              o.data.examples?.[0] ||
              // o.data.responses?.[0]?.content?.text ||
              o.id;
            return (
              <Select.Option
                onMouseLeave={() => handleMouseLeave(o.id)}
                onMouseEnter={() => handleMouseEnter(o.id)}
                key={o.id}
                value={o.id}
                extra={label}
              >
                <Space>
                  <IconUser />
                  <Typography.Text>{label}</Typography.Text>
                </Space>
              </Select.Option>
            );
          })}
        </Select>
      </Item>
      <Item label={t['goto.description']} field="description">
        <Input.TextArea />
      </Item>
    </Form>
  );
};
