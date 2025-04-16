import { Tooltip, Typography } from '@arco-design/web-react';
import React from 'react';
import ValidateColumn from '../ValidateColumn';

const ResponsesColumn = ({ row, color }) => {
  return (
    <ValidateColumn validatorError={row?.bot?.validatorError}>
      {({ error, message }) => (
        <Tooltip
          position="top"
          content={
            error
              ? message
              : row?.bot?.data?.responses?.[0].content?.text || '-'
          }
        >
          <Typography.Text
            style={{ color: error && color, maxWidth: 380, margin: 0 }}
            ellipsis={{ rows: 3 }}
          >
            {row?.bot?.data?.responses?.[0].content?.text || '-'}
          </Typography.Text>
        </Tooltip>
      )}
    </ValidateColumn>
  );
};

export default ResponsesColumn;
