import React from 'react';
import { HuePicker } from 'react-color';

export interface ColorInputProps {
  value?: string;
  onChange?: (value: string) => void;
}
export default function ColorInput({ value, onChange }: ColorInputProps) {
  const handleChange = (color) => {
    onChange(color.hex);
  };
  return (
    <HuePicker width="100%" color={value} onChangeComplete={handleChange} />
  );
}
