import { Tooltip, Typography } from '@arco-design/web-react';
import React from 'react';
import ValidateColumn from '../ValidateColumn';

const ExamplesColumn = ({ row, color }) => {
  return (
    <ValidateColumn validatorError={row?.user?.validatorError}>
      {({ error, message }) => (
        <Tooltip
          position={'tl'}
          content={
            error ? message : row?.user?.data?.examples?.[0]?.text || '-'
          }
        >
          <Typography.Text style={{ color: error && color }}>
            {row?.user?.data?.examples?.[0]?.text || '-'}
          </Typography.Text>
        </Tooltip>
      )}
    </ValidateColumn>
  );
};

export default ExamplesColumn;
