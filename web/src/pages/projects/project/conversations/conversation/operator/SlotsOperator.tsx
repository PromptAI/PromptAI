import { sampleSelect } from '@/graph-next';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphNode } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import { Button, Space, Trigger } from '@arco-design/web-react';
import { IconCodeBlock, IconPlus } from '@arco-design/web-react/icon';
import { useSafeState } from 'ahooks';
import { nanoid } from 'nanoid';
import React from 'react';
import { SelectionProps } from '../types';
import i18n from './locale';
import { newField, newRhetoricalNext } from './operator';

const SlotsOperator = ({
  projectId,
  selection,
  onChange,
  onChangeSelection,
  onChangeEditSelection,
}: SelectionProps<GraphNode>) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useSafeState(false);
  const handleAddSlot = () => {
    const { id, relations } = selection;
    setLoading(true);
    newField(
      projectId,
      id,
      relations,
      { slotId: undefined, slotName: '' },
      (node) => {
        onChange((vals) =>
          sampleSelect(ObjectArrayHelper.add(vals, node), node)
        );
        onChangeSelection(node);
        onChangeEditSelection(node);
        newRhetoricalNext(
          projectId,
          node.id,
          relations,
          [{ id: nanoid(), type: 'text', content: { text: '' }, delay: 500 }],
          (rNode) => {
            onChange((vals) => ObjectArrayHelper.add(vals, rNode));
          }
        );
      }
    ).finally(() => setLoading(false));
  };
  return (
    <Trigger
      position="rt"
      mouseEnterDelay={100}
      mouseLeaveDelay={100}
      popup={() => (
        <Space direction="vertical" className="app-operator-menu">
          <Button size="mini" type="text" onClick={handleAddSlot}>
            <IconCodeBlock className="mr-1" />
            {t['SlotsOperator.title']}
          </Button>
        </Space>
      )}
      trigger="click"
      updateOnScroll
    >
      <Button
        loading={loading}
        size="mini"
        type="text"
        shape="circle"
        icon={<IconPlus />}
      />
    </Trigger>
  );
};

export default SlotsOperator;
