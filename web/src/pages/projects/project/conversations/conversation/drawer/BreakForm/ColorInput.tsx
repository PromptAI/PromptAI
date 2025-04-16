import React from 'react';
import { HuePicker } from 'react-color';

export default ({ value, onChange }: any) => {
  const handleChange = (color) => {
    onChange(color.hex);
  };
  return (
    <HuePicker width="100%" color={value} onChangeComplete={handleChange} />
  );
};
