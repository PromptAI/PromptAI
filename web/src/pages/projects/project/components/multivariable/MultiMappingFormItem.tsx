import React from 'react';
import { Form, FormInstance, FormItemProps } from '@arco-design/web-react';
import MultiMappings from './MultiMappings';
import { IntentMapping } from '@/graph-next/type';

interface MultiMappingFormItemProps extends FormItemProps {
  multiple?: boolean;
  formRef: React.MutableRefObject<FormInstance>;
  initialMappings?: IntentMapping[];
  onFromEntityMappingsChange?: (mappings: IntentMapping[]) => void;
  partType: boolean;
}
const MultiMappingFormItem: React.FC<MultiMappingFormItemProps> = ({
  multiple,
  formRef,
  initialMappings,
  onFromEntityMappingsChange,
  partType,
    field,
  ...rest
}) => {
  return (
    <Form.Item {...rest}>
      <Form.List field={field ? field : 'mappings'}>
        {(fields, operation) => (
          <MultiMappings
            multiple={multiple}
            formRef={formRef}
            fields={fields}
            operation={operation}
            initialMappings={initialMappings || []}
            onFromEntityMappingsChange={onFromEntityMappingsChange}
            partType={partType}
          />
        )}
      </Form.List>
    </Form.Item>
  );
};

export default MultiMappingFormItem;
