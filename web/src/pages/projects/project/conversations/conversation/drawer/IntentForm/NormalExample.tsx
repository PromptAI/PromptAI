import EnterBlurInput from '@/pages/projects/project/components/EnterBlurInput';
import React from 'react';

const NormalExample = (props) => {
  const handleChange = (val) => {
    props.onChange({ text: val, marks: [] });
  };
  return (
    <EnterBlurInput
      autoFocus
      value={props.value?.text}
      placeholder={props.placeholder}
      onChange={handleChange}
      disabled={props.disabled}
    />
  );
};

export default NormalExample;
