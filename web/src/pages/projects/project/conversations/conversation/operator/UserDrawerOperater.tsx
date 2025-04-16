import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphIntentNext } from '@/graph-next/type';
import { Button } from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import React, { useState } from 'react';
import { SelectionProps } from '../types';
import { newResponse } from './operator';

const UserDrawerOperater = ({
  projectId,
  selection,
  onChange,
  onChangeSelection,
}: SelectionProps<GraphIntentNext>) => {
  const [loading, setLoading] = useState(false);
  const handlePlus = () => {
    const { id, relations } = selection;
    setLoading(true);
    newResponse(projectId, id, relations, [], (node) => {
      onChangeSelection({ ...selection, children: [node] });
      onChange((vals) => ObjectArrayHelper.add(vals, node));
    }).finally(() => setLoading(false));
  };
  return isEmpty(selection?.children) ? (
    <Button
      loading={loading}
      size="mini"
      type="text"
      shape="circle"
      icon={<IconPlus />}
      onClick={handlePlus}
    />
  ) : null;
};

export default UserDrawerOperater;
