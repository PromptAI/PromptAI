import { Checkbox } from '@arco-design/web-react';
import React from 'react';

const EnableColumn = ({ item }) => {
  return <Checkbox disabled checked={item.user.data.enable} />;
};

export default EnableColumn;
