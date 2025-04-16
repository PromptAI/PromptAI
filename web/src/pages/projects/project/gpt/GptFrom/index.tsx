import i18n from '../locale';
import useLocale from '@/utils/useLocale';
import {Form, FormInstance, Input,} from '@arco-design/web-react';
import React, {useMemo} from 'react';
import TextArea from '../../components/TextArea';
import {MultiMappingFormItem, SlotsProvider} from '../../components/multivariable';

import useFormRef from "@/hooks/useFormRef";
import {nanoid} from "nanoid";

interface GptFormProps {
    disabled?: boolean;
    initialValues?: any;
    form?: FormInstance;
    slots?: any;
}

const GptForm = (props: GptFormProps) => {
    const {initialValues, form} = props;
    const t = useLocale(i18n);
    const formValues = useMemo(() => {
        return {
            name: initialValues.data.name,
            description: initialValues.data.description,
            prompt: initialValues.data.prompt,
        };
    }, [initialValues]);
    const slots =initialValues.data.slots ? initialValues.data.slots.map((item) => {
        return {
            id:nanoid(),
            slotId: item.id
        }
    }): [];

    const formRef = useFormRef(form);
    formRef.current = form;
    return (
        <Form
            layout="vertical"
            form={form}
            initialValues={formValues}
        >
            <Form.Item
                label={t['gpt.form.field.name']}
                field="name"
                rules={[{required: true, message: t['gpt.form.field.rule.required']}]}
            >
                <Input placeholder={t['gpt.form.field.name.placeholder']}/>
            </Form.Item>
            <Form.Item
                label={t['gpt.form.field.description']}
                field="description"
                rules={[{required: true, message: t['gpt.form.field.rule.required']}]}
            >
                <TextArea style={{minHeight: 60}} placeholder={t['gpt.form.field.description.placeholder']}/>
            </Form.Item>
            <Form.Item
                label={t['gpt.form.field.prompt']}
                field="prompt"
                rules={[{required: true, message: t['gpt.form.field.rule.required']}]}
            >
                <TextArea style={{minHeight: 520}} placeholder={t['gpt.form.field.prompt.placeholder']}/>
            </Form.Item>
            <Form.Item
                label={t['gpt.slots']}
                field="slots"
            >
                <SlotsProvider needMap>
                    <Form.Item shouldUpdate noStyle>
                        <div className="mt-4">
                            <MultiMappingFormItem
                                formRef={formRef}
                                field="slots"
                                multiple={true}
                                partType={true}
                                rules={[
                                    {
                                        minLength: 1,
                                        message: t['form.mappings.rule'],
                                    },
                                ]}
                                initialMappings={slots}
                                onFromEntityMappingsChange={(param) => console.log("entity change," ,JSON.stringify(param))}
                            />
                        </div>
                    </Form.Item>
                </SlotsProvider>
            </Form.Item>

        </Form>
    );
};



export default GptForm;
