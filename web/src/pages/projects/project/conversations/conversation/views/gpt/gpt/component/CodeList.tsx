import React, { useState } from 'react';
import { Button, Form } from '@arco-design/web-react';
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import CodeField from '@/pages/projects/project/conversations/conversation/drawer/FieldForm/components/CodeField';
import { DEFAULT_CODE } from '@/utils/constant';

interface CodeListProps {
  fileName: string;
  initialValue: any;
  formRef: any;
}

const functionCallingDefaultCode = DEFAULT_CODE;

const extractFunctionName = (code) => {
  const match = code.match(/def\s+(\w+)\s*\(/);
  return match ? match[1] : 'unknown_function';
};

const CodeList = ({ fileName, initialValue, formRef }: CodeListProps) => {
  const t = useLocale();

  const handleValuesChange = (changedValues, allValues) => {
    if (!allValues[fileName]) return;
    
    // 初始化时设置默认值
    const updatedValues = allValues[fileName].map((item, index) => {
      if (!item) {
        return {
          code: functionCallingDefaultCode,
          name: extractFunctionName(functionCallingDefaultCode)
        };
      }
      return {
        ...item,
        name: extractFunctionName(item.code)
      };
    });

    formRef.current?.setFieldsValue({
      [fileName]: updatedValues
    });
  };
  // 确保 initialValue 的格式正确
  const formInitialValues = {
    [fileName]: Array.isArray(initialValue) ? initialValue : [initialValue]
  };

  return (
    <div>
      <Form
        ref={formRef}
        initialValues={formInitialValues}  // 修改这里
        wrapperCol={{ style: { padding: 0 } }}
        labelCol={{ style: { padding: 0 } }}
        onValuesChange={handleValuesChange}
      >
        <Form.List field={fileName}>
          {(fields, { add, remove }) => {
            return (
              <div>
                {fields.map((item, index) => {
                  return (
                    <div key={item.key}>
                      <Form.Item>
                        <div className={'flex flex-row justify-between w-full gap-2'}>
                          <Form.Item
                            field={item.field + '.code'}
                            rules={[{ required: true }]}
                          >
                            <CodeField
                              value={item.field + '.code'}
                              defaultValue={functionCallingDefaultCode}
                              title={'Function Calling'}
                              name={formRef.current?.getFieldValue([fileName, index, 'name'])}
                            />
                          </Form.Item>
                          <Button
                            icon={<IconDelete />}
                            shape="circle"
                            status="danger"
                            onClick={() => remove(index)}
                          ></Button>
                        </div>
                      </Form.Item>
                    </div>
                  );
                })}
                <Form.Item>
                  <Button
                    type="outline"
                    status="success"
                    long
                    icon={<IconPlus />}
                    onClick={() => {
                      add();
                    }}
                  >
                    {t['add']}
                  </Button>
                </Form.Item>
              </div>
            );
          }}
        </Form.List>
      </Form>
    </div>
  );

};
export default CodeList;