import React from 'react';
import { nodeIconsMap } from '../../../conversations/conversation/nodes/config';

const RootTypeColumn = ({ item }) => {
  return React.cloneElement(nodeIconsMap[item.rootType], {
    className: 'app-icon',
  });
};

export default RootTypeColumn;
