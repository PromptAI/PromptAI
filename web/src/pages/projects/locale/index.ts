const i18n = {
  'en-US': {
    'project.import': 'No data yet, you can try to import',
    'project.create': 'Create Project',
    'project.yaml.import': 'Import Yaml',
    'project.zip.import': 'Import Mica',
    'project.image.tooltip': 'Click to enter the project',
    'project.state.deployment': 'Deployment in progress',
    'project.state.Published': 'Published',
    'project.state.Unpublished': 'Unpublished',
    'project.create.title': 'Create Project',
    'project.update.title': 'Update Project',
    'project.form.name': 'Name',
    'project.form.locale': 'Locale',
    'project.form.description': 'Description',
    'project.form.image': 'Cover',
    'faqs.title': 'Faqs',
    'faqs.create': 'Create Faq',
    'faqs.update': 'Update Faq',
    'faqs.form.name': 'Name',
    'faqs.form.description': 'Description',
    'conversation.title': 'Flows',
    'conversation.create': 'Create Flows',
    'conversation.update': 'Update Flow',
    'conversation.form.name': 'Name',
    'conversation.form.description': 'Description',
    'entities.title': 'Entities',
    'entity.title': 'Entity',
    'entities.create': 'Create Entity',
    'entities.update': 'Update Entity',
    'entities.info': 'Entity Info',
    'entities.form.name': 'Name',
    'entities.form.description': 'Description',
    'entities.form.name.error': 'The same name already exists',
    'entities.form.subumit': 'Subumit',
    'entities.form.update': 'Update',
    'intents.title': 'Intents',
    'intents.create': 'Create Intent',
    'intents.update': 'Update',
    'intents.info': 'Intent Info',
    'intents.form.Keyword': 'Keyword',
    'intents.form.Keyword.help': 'You can use the keyword to search this',
    'intents.form.Keyword.error': 'The same name already exists',
    'intents.form.examples.help': 'You need at least two values',
    '"detele"': 'Delete',
    'intents.form.examples.reset': 'Reset',
    'intents.form.submit': 'Subumit',
    'intents.form.update': 'Update',
    'bots.title': 'Answer',
    'bots.item.title': 'Answer',
    'webhooks.title': 'Webhook',
    'webhooks.create': 'Add',
    'webhooks.refresh': 'Refresh',
    'webhooks.info': 'Webhook Info',
    'webhooks.form.name': 'Name',
    'webhooks.form.description': 'Description',
    'webhooks.form.url': 'Webhook URL',
    'webhooks.form.header': 'Header Optional',
    'webhooks.form.params': 'Params Optional',
    'webhooks.form.request-type': 'Request Method',
    'webhooks.form.request-type.option.get': 'Get',
    'webhooks.form.request-type.option.get-with-url-encode':
      'Get with url encode',
    'webhooks.form.request-type.option.post-form': 'Post Form',
    'webhooks.form.request-type.option.post-json': 'Post Json',
    'webhooks.form.request-header-type': 'Request Header',
    'webhooks.form.request.settings': 'Request Settings',
    'webhooks.form.response.settings': 'Response Settings',
    'webhooks.form.response-type': 'Response Type',
    'webhooks.form.response-type.option.not': 'No Response',
    'webhooks.form.response-type.option.direct': 'Original Response',
    'webhooks.form.response-type.option.sample': 'Default Display',
    'webhooks.form.response-type.option.custom': 'Customized Response',
    'webhooks.form.response-type.custom': 'Custom Display Handle',
    'webhooks.form.response-handle.parse':
      'Parse the result returned by the webhook',
    'webhooks.form.response-handle.parse.help': 'Parse Response with JSON path',
    'webhooks.form.response-handle.success':
      'Returning message if the call is completed successfully',
    'webhooks.form.response-handle.success.help':
      'Display fommatting with "{key}" content',
    'webhooks.form.response-handle.error':
      'Returning message if the call fails or does not receive any result',
    'webhooks.form.response-handle.error.help': 'Display error content',
    'webhooks.form.submit': 'Submit',
    'webhooks.form.cancel': 'Cancel',
    'webhooks.form.update': 'Update',
    'stores.title': 'Stores',
    'messages.title': 'Message history',
    'messages.Tips': 'No message record',
    'messages.SelectorPlaceholder': 'select project',
    'train.error': ' The Train is error.',
    'train.success': 'Congratulations, successful training.',
    'train.cancel': 'Cancel',
    'forms.title': 'Form',
    'forms.create': 'Create Form',
    'forms.update': 'Update Form',
    'forms.form.name': 'Name',
    'forms.form.description': 'Description',
    'project.delete.warning': 'Are you sure to delete the item',
    'project.form.welcome': 'Welcome',
    'project.from.welcome.placeholder':
      "Hello, I'm your smart assistant. What can I do for you?",
    'project.form.Unknown': 'Fallback',
    'project.form.Unknown.placeholder':
      'Lame! We got a glitch, please try again later.',
    'update.time': 'Update time',
    'faq.component.delete': 'Confirm to delete simple session (FAQ)',
    'flow.component.delete': 'Are you sure to delete the process session',
    knowledgeGraph: 'Knowledge Graph',
    knowledgeBase: 'Knowledge Base',
    save: 'Save',
    close: 'Close',
    refresh: 'Refresh',
    bot: 'Bot',
    'save.success': 'Save Success',
    'Dialogue.process': 'Dialogue Process',
    'User.input': 'User Question',
    'Smart.reply': 'Bot Response',
    unknown: 'Unknown',
    mainExample: 'Expecting user question',
    examples: 'More training examples',
    notExamples: 'No more training examples',
    'examples.placeholder': 'example sentence',
    'examples.add': 'Add',
    description: 'Description',
    'description.placeholder': 'User Question description',
    hello: 'Hello',

    'picture.description': 'Picture Description:',
    'delayed.warning': 'The delay should be between 0 and 5000 milliseconds',
    baseResponseRulesMessage: 'At least one reply is required',
    'text.warning': 'Please fill in',
    'delayed.reply': 'Delayed reply',
    millisecond: 'Millisecond',
    'welcome.text':
      "Hello, I'm your intelligent question and answer assistant. I can help you answer your questions!",
    text: 'Text',
    image: 'Image',
    'form.out': 'Jump out of form',
    'user.options': 'User Options',
    informationGathering: 'information gathering',
    doAsk: 'Bot Asks',
    'doAsk.placeholder': 'Please enter the missing query',

    'doAsk.description': 'Missing query description',
    variable: 'slot',
    goto: 'goto',
    'intents.table.text': 'Name',
    'intents.table.used': 'Referred In',
    'intents.table.used.number': 'second',
    'intents.table.use': 'Process references',
    'intents.table.length': 'Quantity',
    'intents.table.delete': 'Delete',
    'intents.table.refresh': 'Refresh',
    'intents.table.option': 'Operation',
    'intents.detele.modelTitle': 'Delete intention template',
    'intents.detele.modelDescription':
      'Are you sure to delete this intention template?',
    'intents.detele.success': 'Delete succeeded',
    'intents.modal.create': 'Add',
    'intents.modal.edit': 'edit',
    'intents.modal.createTitle': 'Add intention template',
    'intents.modal.editTitle': 'Edit intent template',
    'intents.form.name': 'Template name',
    'intents.form.name.placeholder': 'Please enter a template name',
    'intents.form.mainExample': 'Expecting user question',
    'intents.form.mask.placeholder': 'User Question',
    'intents.form.slotVisible.true': 'Need to extract slots',
    'intents.form.slotVisible.false': 'No need to extract slots',
    'intents.form.examples': 'More training examples',
    'intents.form.examples.empty': 'No more training examples',
    'intents.form.examples.add': 'Add',
    'intents.form.description': 'Description',
    'intents.form.description.placeholder': 'User question description',

    'bots.table.text': 'Reply',
    'bots.table.from': 'Come From',
    'bots.table.flow': 'Flow',
    'bots.table.type': 'Type',
    'bots.table.type.text': 'Text',
    'bots.table.type.button': 'User options',
    'bots.table.type.webhook': 'Webhook',
    'bots.table.type.image': 'Image',
    'bots.table.type.images.close': 'Close',
    'bots.table.type.images.preview': 'Preview',
    'bots.table.type.images.previewEmpty': 'No preview',
    'bots.table.refresh': 'Refresh',
    'slot.title': 'slot list',
    'slot.table.name': 'slot name',
    'slot.table.refresh': 'Refresh',
    'slot.table.option': 'Optional',
    'slot.table.delete': 'Delete',
    'slot.table.delete.title': 'Delete this Slot',
    'slot.table.delete.content': 'Are you sure to delete this Slot ?',
    'slot.table.delete.success': 'Delete Success',

    'webHook.from.url.select': 'Please select',
    'webHook.from.Header': 'Request header',
    'webHook.from.url.notHeader': 'No header',
    'webHook.from.url.customHeader': 'Custom header',
    'webHook.from.header.pairs.key': 'Header name',
    'webHook.from.header.pairs.val': 'Header value',
    'webHook.from.empty': 'Please enter',
    'webHook.from.request_body_type': 'Request Body',
    'webHook.from.request_body_type.none': 'No Data',
    'webHook.from.request_body_type.from': 'Form Data',
    'webHook.from.request_body_type.json': 'JSON Data',
    'webHook.from.request_body_type.text': 'Text Data',
    'webHook.from.request_body_type.json.placeholder': 'JSON data',
    'webHook.from.request_body_type.text.placeholder': 'Text data',
    'webHook.from.response_body.placeholder': '(parse by',
    'webHook.from.response_body.url': 'jsonpath expression',
    'webHook.from.response_body.content.placeholder': ')',
    'webHook.from.response_body.pairs.val': 'Items from returned json',
    'webHook.from.response_body.pairs.key': 'Slot',

    'trainLoader.newest': 'The latest model has been loaded!',
    'trainLoader.trainError':
      'The current model training failed, and the conversation may be inaccurate!',
    'trainLoader.trainSuccess': 'Successful training',
    'trainLoader.cancel.error': 'Failed to cancel training',
    'trainLoader.notModal': 'There is no trained model, please train the model',
    'trainLoader.trainCompentNot':
      'The current model has not been trained, and the conversation may not be accurate!',

    'faq.table.intent': 'Intent',
    'faq.table.response': 'Response',
    'faq.table.moreIntent': 'More training examples',
    'faq.faqFrom.flowName': 'Process name:',
    'faq.faqFrom.flowName.placeholder': 'Please enter the process name',
    'faq.faqFrom.description': 'Description',
    'faq.faqFrom.description.placeholder': 'Dialogue process description',

    'flow.optional.selectName': 'Option name:',
    'flow.optional.selectName.placeholder': 'Please enter an option name',
    'flow.optional.description': 'Description',
    'flow.optional.description.placeholder': 'User option description',

    'sample.textRule': 'Required',
    'sample.textRule.placeholder': 'Please fill in',
    'sample.examples': 'User Question',
    'sample.examples.placeholder': 'User Question',
    'sample.labels.placeholder': 'Please enter the question type',
    'sample.labels': 'Question Category',
    'sample.labels.add': 'Add category',
    'conversation.drawer.leave.message':
      'If you leave the page, we will not save your modified data',
    'component.debug.robot.run.modal.title': 'Debug Run',
    'component.debug.robot.run.modal.form.componentIds': 'Available Module',
    'component.debug.robot.run.modal.form.unabled': 'UnAvailable Module',
    'component.debug.robot.run.modal.form.unabled.help':
      'The following agents are incomplete. You may know that debugging failed or did not execute as expected',

    'webhookView.card.delete': 'Delete',
    'webhookView.card.delete.tooltip':
      'Are you sure you want to delete the webhook:',
    'webhookView.card.copy': 'Copy',
    'webhookView.card.detail': 'Detail',

    'messageView.table.ip': 'IP Address',
    'messageView.table.source': 'Source',
    'messageView.table.ratings': 'Ratings',
    'messageView.table.sessionTime': 'Session Time',

    'conversation.intentForm.examples.generate': 'Generate',
    'conversation.intentForm.examples.auto.generate': 'Auto Generate',
    'conversation.intentForm.examples.auto.generate.tip':
      'When there are less than 10 example sentences, it will be automatically generated',
    needAnswer: 'with Answer',
    'project.form.type': 'Type',
    'sample.name': 'Name',
    'project.type.rasa': 'RASA',
    'project.type.llm': 'LLM',
    nameRule: 'Please enter letters, numbers, underscores, or hyphens, without spaces or other symbols.',
    'rule.required': 'Required',
  },
  'zh-CN': {
    'project.import': '暂无数据,您可以尝试导入',
    'project.yaml.import': '导入Yaml',
    'project.zip.import': '导入Mica',
    'sample.textRule': '必填项',
    'sample.textRule.placeholder': '请填写',
    'sample.examples': '用户提问',
    'sample.examples.placeholder': '用户输入',
    'sample.labels.placeholder': '请输入问题类型',
    'sample.labels': '问题分类',
    'sample.labels.add': '添加分类',

    'flow.optional.selectName': '选项名称',
    'flow.optional.selectName.placeholder': '请输入选项名称',
    'flow.optional.description': '描述',
    'flow.optional.description.placeholder': '用户选项描述',

    'faq.table.intent': '意图',
    'faq.table.response': '回答',
    'faq.table.moreIntent': '更多训练例句',
    'faq.faqFrom.flowName': '流程名称',
    'faq.faqFrom.flowName.placeholder': '请输入流程名称',
    'faq.faqFrom.description': '描述',
    'faq.faqFrom.description.placeholder': '对话流程描述',

    'trainLoader.newest': '已加载最新模型！',
    'trainLoader.trainError': '当前模型训练失败，对话可能不准确！',
    'trainLoader.trainSuccess': '训练成功',
    'trainLoader.cancel.error': '取消训练失败',
    'trainLoader.notModal': '没有训练完成的模型，请训练模型',
    'trainLoader.trainCompentNot': '当前模型未训练完成，对话可能不准确！',

    'webHook.from.url.select': '请选择',
    'webHook.from.Header': '请求标头',
    'webHook.from.url.notHeader': '无标头',
    'webHook.from.url.customHeader': '自定义标头',
    'webHook.from.header.pairs.key': '标头名称',
    'webHook.from.header.pairs.val': '标头取值',
    'webHook.from.empty': '请输入',
    'webHook.from.request_body_type': '数据格式',
    'webHook.from.request_body_type.none': '无数据格式',
    'webHook.from.request_body_type.from': '表单数据格式',
    'webHook.from.request_body_type.json': 'JSON数据格式',
    'webHook.from.request_body_type.text': '文本数据格式',
    'webHook.from.request_body_type.json.placeholder': 'JSON数据',
    'webHook.from.request_body_type.text.placeholder': '文本数据',
    'webHook.from.response_body.placeholder': '根据',
    'webHook.from.response_body.url': 'JsonPath表达式',
    'webHook.from.response_body.content.placeholder': '解析请求响应内容',
    'webHook.from.response_body.pairs.val': 'Json 数据值',
    'webHook.from.response_body.pairs.key': 'Rasa 变量',

    'slot.title': '变量列表(Slot)',
    'slot.table.name': '变量名',
    'slot.table.refresh': '刷新',
    'slot.table.option': '操作',
    'slot.table.delete': '删除',
    'slot.table.delete.title': '删除变量',
    'slot.table.delete.content': '确定删除该变量吗 ?',
    'slot.table.delete.success': '删除成功',

    'bots.table.text': '回复',
    'bots.table.from': '来自',
    'bots.table.flow': '流程',
    'bots.table.type': '类型',
    'bots.table.type.text': '文本',
    'bots.table.type.button': '用户选项',
    'bots.table.type.webhook': 'Webhook',
    'bots.table.type.image': '图片',
    'bots.table.type.images.close': '关闭',
    'bots.table.type.images.preview': '预览',
    'bots.table.type.images.previewEmpty': '暂无预览图',
    'bots.table.refresh': '刷新',

    'intents.table.text': '名称',
    'intents.table.used': '被引用于',
    'intents.table.used.number': '次',
    'intents.table.use': '流程引用了',
    'intents.table.length': '训练语句数量',
    'intents.table.delete': '删除',
    'intents.table.refresh': '刷新',
    'intents.table.option': '操作',
    'intents.detele.modelTitle': '删除意图模版',
    'intents.detele.modelDescription': '确定删除该意图模版吗？',
    'intents.detele.success': '删除成功',
    'intents.modal.create': '新增',
    'intents.modal.edit': '编辑',
    'intents.modal.createTitle': '新增意图模版',
    'intents.modal.editTitle': '编辑意图模版',
    'intents.form.name': '模版名',
    'intents.form.name.placeholder': '请输入模版名',
    'intents.form.mainExample': '期待用户输入',
    'intents.form.mask.placeholder': '用户输入',
    'intents.form.slotVisible.true': '需要提取变量',
    'intents.form.slotVisible.false': '不需要提取变量',
    'intents.form.examples': '更多训练例句',
    'intents.form.examples.empty': '暂无更多训练例句',
    'intents.form.examples.add': '新增',
    'intents.form.description': '描述',
    'intents.form.description.placeholder': '用户输入描述',

    link: '进入',
    variable: '变量',
    goto: '跳转到',
    informationGathering: '信息收集',
    doAsk: '缺失询问',
    'doAsk.placeholder': '请输入缺失询问语句',
    'doAsk.description': '缺失询问描述',
    text: '文本',
    image: '图片',
    'welcome.text': '您好,我是您的智能问答助手,我可以帮你解答你的疑问！',
    millisecond: '毫秒',
    'delayed.reply': '延时回复',
    baseResponseRulesMessage: '至少需要一个回复',
    'text.warning': '请填写',
    'delayed.warning': '延时应该在0～5000毫秒之间',
    hello: '你好',
    'picture.description': '图片描述',
    mainExample: '期待用户输入',
    examples: '更多训练例句',
    unknown: '未知',
    'form.out': '信息收集中断',
    'user.options': '用户选项',
    notExamples: '暂无更多训练例句',
    description: '描述',
    'description.placeholder': '用户输入描述',
    'examples.add': '新增',
    'examples.placeholder': '例句',
    'Smart.reply': '机器回复',
    'User.input': '用户输入',
    'Dialogue.process': '对话流程',
    save: '保存',
    'save.success': '保存成功',
    close: '关闭',
    refresh: '刷新',
    bot: '机器人',
    knowledgeGraph: '知识图谱',
    knowledgeBase: '知识库',
    'faq.component.delete': '确定删除用户问答(FAQ)',
    'flow.component.delete': '确定删除对话流图',
    'update.time': '更新时间',
    'project.form.Unknown': '未知问题回复',
    'project.form.Unknown.placeholder': '抱歉，我还在学习中，请换个问题试试。',
    'project.delete.warning': '确定删除项目',
    'project.form.welcome': '欢迎语',
    'project.from.welcome.placeholder':
      '您好，我是您的智能助手，请问有什么需要我做的？',
    'project.create': '创建项目',
    'project.image.tooltip': '点击进入项目',
    'project.state.deployment': '发布中',
    'project.state.Published': '已发布',
    'project.state.Unpublished': '未发布',
    'project.create.title': '创建项目',
    'project.update.title': '更新项目',
    'project.form.name': '名称',
    'project.form.locale': '语言',
    'project.form.description': '描述',
    'project.form.image': '显示图片',
    'faqs.title': '用户问答(FAQ)',
    'faqs.create': '创建用户回答(FAQ)',
    'faqs.update': '更新用户回答(FAQ)',
    'faqs.form.name': '名称',
    'faqs.form.description': '描述',
    'conversation.title': '对话流图',
    'conversation.create': '创建对话流图',
    'conversation.update': '更新会话',
    'conversation.form.name': '名称',
    'conversation.form.description': '描述',
    'entities.title': '实体',
    'entity.title': '实体',
    'entities.create': ' Entity',
    'entities.update': '更新 Entity',
    'entities.info': 'Entity 详细',
    'entities.form.name': '名称',
    'entities.form.description': '描述',
    'entities.form.name.error': '已存在相同名称',
    'entities.form.submit': '提交',
    'entities.form.update': '更新',
    'intents.title': '意图列表',
    'intents.create': ' Intent',
    'intents.Update': '更新',
    'intents.info': 'Intent 详细',
    'intents.form.Keyword': '关键字',
    'intents.form.Keyword.error': '已存在相同名称',
    'intents.form.examples.help': '至少需要两个值',
    '"detele"': '删除',
    'intents.form.examples.reset': '重置',
    'intents.form.Keyword.help': '您可以使用关键字搜索',
    'intents.form.submit': '提交',
    'intents.form.update': '更新',
    'bots.title': '回答列表',
    'bots.item.title': '回答',
    'webhooks.title': 'Webhook',
    'webhooks.create': ' 新增',
    'webhooks.refresh': '刷新',
    'webhooks.info': 'Webhook 详细',
    'webhooks.form.name': '名称',
    'webhooks.form.description': '描述',
    'webhooks.form.url': 'URL',
    'webhooks.form.header': '请求头',
    'webhooks.form.params': '请求参数',
    'webhooks.form.request-type': '请求方法',
    'webhooks.form.request-type.option.get': 'Get',
    'webhooks.form.request-type.option.get-with-url-encode': 'Get 和 Url 参数',
    'webhooks.form.request-type.option.post-form': 'Post 表单',
    'webhooks.form.request-type.option.post-json': 'Post Json',
    'webhooks.form.request-header-type': '请求标头',
    'webhooks.form.request.settings': '请求设置',
    'webhooks.form.response.settings': '响应设置',
    'webhooks.form.response-type': '响应处理',
    'webhooks.form.response-type.option.not': '忽略响应',
    'webhooks.form.response-type.option.direct': '原始响应',
    'webhooks.form.response-type.option.sample': '忽略状态码',
    'webhooks.form.response-type.option.custom': '自定义处理',
    'webhooks.form.response-type.custom': '自定义处理',
    'webhooks.form.response-handle.parse': '解析响应',
    'webhooks.form.response-handle.parse.help':
      '根据JsonPath表达式解析请求响应内容',
    'webhooks.form.response-handle.success': '请求响应成功时',
    'webhooks.form.response-handle.success.help':
      '自定义返回内容, 可以通过上面定义的"{key}"来引用到JsonPath获取到值',
    'webhooks.form.response-handle.error': '请求响应失败时',
    'webhooks.form.response-handle.error.help': '自定义返回内容',
    'webhooks.form.submit': '提交',
    'webhooks.form.cancel': '取消',
    'webhooks.form.update': '更新',
    'stores.title': '故事',
    'messages.title': '消息历史',
    'messages.Tips': '没有消息记录',
    'messages.SelectorPlaceholder': '选择项目',
    'train.error': '训练失败',
    'train.success': '训练结束',
    'train.cancel': '取消训练成功,当前会话可能不准确！',
    'forms.title': '表单',
    'forms.create': '创建表单',
    'forms.update': '更新表单',
    'forms.form.name': '名称',
    'forms.form.description': '描述',
    'conversation.drawer.leave.message':
      '离开改页面，我们将不在保存您修改的数据',
    'component.debug.robot.run.modal.title': '调试运行',
    'component.debug.robot.run.modal.form.componentIds': '可用代理',
    'component.debug.robot.run.modal.form.unabled': '不可用代理',
    'component.debug.robot.run.modal.form.unabled.help':
      '以下代理不完整，可能会调试失败或不按预期执行',

    'webhookView.card.delete': '删除',
    'webhookView.card.delete.tooltip': '确认删除Webhook:',
    'webhookView.card.copy': '复制',
    'webhookView.card.detail': '详情',

    'messageView.table.ip': 'IP地址',
    'messageView.table.source': '来源',
    'messageView.table.ratings': '评价数量',
    'messageView.table.sessionTime': '会话时间',

    'conversation.intentForm.examples.generate': '生成',
    'conversation.intentForm.examples.auto.generate': '自动生成',
    'conversation.intentForm.examples.auto.generate.tip':
      '例句不足10条时,自动生成',
    needAnswer: '附带答案',
    'project.form.type': '类型',
    'sample.name': '名称',
    'project.type.rasa': 'RASA',
    'project.type.llm': '大语言模型',
    nameRule: '请输入字母、数字、下划线或连字符，且不得包含空格或其他符号。',
    'rule.required': '必填项',
  },
};

export default i18n;
