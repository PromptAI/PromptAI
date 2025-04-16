import React, { useMemo } from 'react';

interface ValidateColumnProps {
  validatorError: any;
  children: (error: { error: boolean; message: string }) => React.ReactElement;
}
const ValidateColumn = ({ validatorError, children }: ValidateColumnProps) => {
  const error = useMemo(
    () => ({
      error:
        !!validatorError &&
        validatorError.errorCode !== undefined &&
        validatorError.errorCode !== 0,
      message: validatorError?.errorMessage,
    }),
    [validatorError]
  );
  return children(error);
};

export default ValidateColumn;
