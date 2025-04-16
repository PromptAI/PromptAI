import { useEffect, useState } from 'react';
import React from 'react';
import { Select, Input } from '@arco-design/web-react';

function CustomInput(props) {
  const [stateValue, setValue] = useState(props.value);
  const value = props.value || stateValue || {};
  useEffect(() => {
    if (props.value !== stateValue && props.value === undefined) {
      setValue(props.value);
    }
  }, [props.value, stateValue]);

  const handleChange = (newValue) => {
    if (!('value' in props)) {
      setValue(newValue);
    }

    props.onChange && props.onChange(newValue);
  };

  return (
    <Input
      value={value.api}
      onChange={(v) => {
        handleChange({ ...value, api: v });
      }}
      required
      allowClear
      addBefore={
        <Select
          placeholder="Please select ..."
          style={{ width: 100 }}
          value={value.agreement}
          options={['http://', 'https://']}
          onChange={(v) => {
            handleChange({ ...value, agreement: v });
          }}
        />
      }
    />
  );
}

export default CustomInput;
