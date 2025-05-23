const i18n = {
  'en-US': {
    'ConversationForm.name': 'Process name:',
    'ConversationForm.name.placeholder': 'Please enter the process name',
    'ConversationForm.description': 'Description',
    'ConversationForm.description.placeholder': 'Dialogue process description',
    'IntentForm.mainExample': 'User',
    'IntentForm.mainExample.label': 'Template name',
    'IntentForm.mainExample.mark.placeholder': 'User',
    'IntentForm.slotDisabled.true': 'Need to extract slots',
    'IntentForm.slotDisabled.false': 'No need to extract slots',
    'IntentForm.examples.label': 'More training examples',
    'IntentForm.examples.Empty': 'No more training examples',
    'IntentForm.examples.add': 'Add',
    'IntentForm.description': 'Description',
    'IntentForm.description.placeholder': 'User description',
    'IntentForm.mappingRules.emptySlot': 'Please select or add a slot',
    'IntentForm.mappingRules.emptyType': 'Please select slot value source',
    'IntentForm.mappingRules.empty': 'Please enter a custom value',
    'IntentForm.examples.item.empty': 'please enter example text',
    'Mapping.Select': 'Slot value source',
    'Mapping.Select.placeholder': 'Select source',
    'Mapping.Select.from_text': 'Enter text',
    'Mapping.Select.from_entity': 'Entity extraction',
    'Mapping.Select.from_intent': 'Custom value',
    'Mapping.Select.input': 'Custom slot value',
    'IntentLinkShare.buttonText': 'Select from template',
    'IntentLinkShare.modalTitle': 'Select intention template',
    'IntentLinkShare.id': 'Intention template',
    'IntentLinkShare.id.placeholder':
      'Please select the intention template to be used',
    'ShareIntentModal.bottonText': 'Save as template',
    'ShareIntentModal.modalTitle': 'Save as reusable template',
    'ShareIntentModal.name': 'Reuse template name',
    'ShareIntentModal.name.placeholder': 'Please enter a reuse template name',
    'SlotSelect.prefix': 'Slot name',
    'SlotSelect.placeholder': 'Select slot name or add',
    'SlotSelect.modalTitle': 'Create a slot name',
    'SlotSelect.name': 'Slot name',
    'SlotSelect.name.placeholder': 'Please enter a slot name',
    'ConditionForm.name': 'Jump out status',
    'ConditionForm.color': 'colour',
    'BreakForm.name': 'Jump out status',
    'BreakForm.color': 'colour',
    'BotForm.baseResponseRules.message': 'At least one reply is required',
    'BotForm.text.warning': 'Please fill in',
    'BotForm.delay.warning':
      'The delay should be between 0 and 5000 milliseconds',
    'BotForm.image.description.warning':
      'Please fill in the picture description',
    'BotForm.image.warning': 'Please upload pictures',
    'BotForm.webhook.warning': 'Please select',
    'BotForm.text': 'text',
    'BotForm.image': 'picture',
    'BotForm.webhook': 'webhook',
    'BotForm.title': 'Bot Response',
    'BotForm.description': 'Description',
    'BotForm.description.placeholder': 'Intelligent reply description',
    'BotForm.Image.input.placeholder': 'Simple description of the picture',
    'BotForm.Image.InputNumber.prefix': 'Delayed reply',
    'BotForm.Image.InputNumber.suffix': 'millisecond',
    'BotForm.Text.TextArea.placeholder':
      "Hello, I'm your intelligent question and answer assistant. I can help you answer your questions!",
    'BotForm.Text.InputNumber.prefix': 'Delayed reply',
    'BotForm.Text.InputNumber.suffix': 'millisecond',
    'BotForm.Webhook.select.placeholder': 'Please select',

    'BotForm.operation.addSuccess': 'Add succeeded',
    'BotForm.operation.updateSuccess': 'Update succeeded',
    'BotForm.operation.updateFail': 'Update failed',

    'BotForm.header.add': 'Add',
    'BotForm.dialog.add': 'Add response template',
    'BotForm.dialog.update': 'Edit Response',
    'BotForm.dialog.name': 'Name',
    'BotForm.dialog.name.placeholder': 'please enter a response name',
    'BotForm.dialog.botResponse': 'Response',
    'BotForm.dialog.botResponse.types.text': 'Text',
    'BotForm.dialog.botResponse.types.img': 'Image',
    'BotForm.dialog.description': 'Description',
    'BotForm.dialog.webhook.detail': 'Detail',
  },
  'zh-CN': {
    'ConversationForm.name': '流程名称',
    'ConversationForm.name.placeholder': '请输入流程名称',
    'ConversationForm.description': '描述',
    'ConversationForm.description.placeholder': '对话流程描述',

    'IntentForm.mappingRules.emptySlot': '请选择或者添加一个变量',
    'IntentForm.mappingRules.emptyType': '请选择变量值来源',
    'IntentForm.mappingRules.empty': '请输入自定义值',
    'IntentForm.mainExample': '期待用户输入',
    'IntentForm.mainExample.label': '模版名',
    'IntentForm.mainExample.mark.placeholder': '用户输入',
    'IntentForm.slotDisabled.true': '需要提取变量',
    'IntentForm.slotDisabled.false': '是否需要提取变量',
    'IntentForm.examples.label': '更多训练例句',
    'IntentForm.example': '例句',
    'IntentForm.examples.Empty': '暂无更多训练例句',
    'IntentForm.examples.add': '新增',
    'IntentForm.description': '描述',
    'IntentForm.description.placeholder': '用户输入描述',
    'IntentForm.examples.item.empty': '请输入文本',
    'Mapping.Select': '变量值来源',
    'Mapping.Select.placeholder': '选择来源',
    'Mapping.Select.from_text': '输入文本',
    'Mapping.Select.from_entity': '实体提取',
    'Mapping.Select.from_intent': '自定义值',
    'Mapping.Select.input': '自定义变量值',
    'IntentLinkShare.buttonText': '从模版中选取',
    'IntentLinkShare.modalTitle': '选择意图模版',
    'IntentLinkShare.id': '意图模版',
    'IntentLinkShare.id.placeholder': '请选择需要使用的意图模版',
    'ShareIntentModal.bottonText': '保存为模版',
    'ShareIntentModal.modalTitle': '保存为可重用模版',
    'ShareIntentModal.name': '重用模版名',
    'ShareIntentModal.name.placeholder': '请输入重用模版名',
    'SlotSelect.prefix': '变量名称',
    'SlotSelect.placeholder': '选择变量名称或者增加',
    'SlotSelect.modalTitle': '创建一个变量名',
    'SlotSelect.name': '变量名',
    'SlotSelect.name.placeholder': '请输入变量名',
    'ConditionForm.name': '跳出状态',
    'ConditionForm.color': '颜色',
    'BreakForm.name': '信息收集中断',
    'BreakForm.color': '颜色',
    'BotForm.baseResponseRules.message': '至少需要一个回复',
    'BotForm.text.warning': '请填写',
    'BotForm.delay.warning': '延时应该在0～5000毫秒之间',
    'BotForm.image.description.warning': '请填写图片描述',
    'BotForm.image.warning': '请上传图片',
    'BotForm.webhook.warning': '请选择',
    'BotForm.text': '文本',
    'BotForm.image': '图片',
    'BotForm.webhook': 'webhook',
    'BotForm.title': '机器回复',
    'BotForm.description': '描述',
    'BotForm.description.placeholder': '机器回复描述',
    'BotForm.Image.input.placeholder': '图片的简单描述',
    'BotForm.Image.InputNumber.prefix': '延时回复',
    'BotForm.Image.InputNumber.suffix': '毫秒',
    'BotForm.Text.TextArea.placeholder':
      '您好,我是您的智能问答助手,我可以帮你解答你的疑问！',
    'BotForm.Text.InputNumber.prefix': '延时回复',
    'BotForm.Text.InputNumber.suffix': '毫秒',
    'BotForm.Webhook.select.placeholder': '请选择',

    'BotForm.operation.addSuccess': '新增成功',
    'BotForm.operation.updateSuccess': '更新成功',
    'BotForm.operation.updateFail': '更新失败',
    'BotForm.header.add': '新增',
    'BotForm.dialog.add': '新增机器回复',
    'BotForm.dialog.update': '编辑机器回复',
    'BotForm.dialog.name': '名称',
    'BotForm.dialog.name.placeholder': '请输入名称',
    'BotForm.dialog.botResponse': '机器回复',
    'BotForm.dialog.botResponse.types.text': '文本',
    'BotForm.dialog.botResponse.types.img': '图片',
    'BotForm.dialog.description': '描述',
    'BotForm.dialog.webhook.detail': '详情',
  },
};

export default i18n;
