const i18n = {
  'en-US': {
    'llm.save': 'Save',
    'sample.form.blnNlpModel': 'FAQ Use LLM ?',
    'sample.form.blnNlpModel.mutate': 'Reminder',
    'sample.form.blnNlpModel.mutate.tip':
      'Note: Please recompile (Debug Run) if the above setting is changed.',
    'sample.form.blnShowRelatedQuestions':
      'When answering the FAQ, the bot will display other related questions.',
    'sample.form.showKnowledgeBaseSource':
      'When answering the Knowledge Base, the bot will display related links.',
    'sample.form.blnUseNlpAnswer':
      'When answering a question not in the FAQ,  the bot could generate answers from the knowledge bases.',
    'sample.form.blnNlpModel.config': 'LLM',
    'sample.form.useNlpModel': 'LLM',
    'sample.form.LLM.faq': 'FAQ',
    'sample.form.LLM.flow':
      'Flows (Intent classification and entity recognition will be conducted by LLMs, which has higher accuracy)',
    'sample.form.LLM.other.setting': 'FAQ other settings',
    'sample.form.replyKnowledgeBaseStrict':'Only answer with content that is already present in the knowledge base.',
    'llm.account': 'Account',
    'llm.account.user': 'Use your OpenAI API KEY',
    'llm.account.user.api': 'Your OpenAI API KEY',
    'llm.account.user.api.placeholder': 'OpenAI API Key...',
    'llm.settings': 'Enable/Disable LLMs in the following modules',
  },
  'zh-CN': {
    'llm.save': '保存',
    'sample.form.blnNlpModel': 'FAQ是否使用大语言模型',
    'sample.form.blnNlpModel.mutate': '提示',
    'sample.form.blnNlpModel.mutate.tip':
      'Note: Please recompile (Debug Run) if the above setting is changed.',
    'sample.form.blnShowRelatedQuestions': '回答FAQ时，显示其他相关问题',
    'sample.form.showKnowledgeBaseSource': '回答知识库时，显示相关链接',
    'sample.form.blnUseNlpAnswer':
      '回答FAQ时，使用大语言模型生成答案(跟原来的答案可能有差异)',
    'sample.form.blnNlpModel.config': '大语言模型',
    'sample.form.useNlpModel': '大语言模型',
    'sample.form.LLM.faq': '用户问答(FAQ)',
    'sample.form.LLM.flow': 'Flows（意图分类和实体识别由LLM进行，精度更高）',
    'sample.form.LLM.other.setting': '用户问答(FAQ)其它设置',
    'sample.form.replyKnowledgeBaseStrict':'回答知识库时，仅回答知识库已有内容',
    'llm.account': '账号',
    'llm.account.system': '使用我们的账号',
    'llm.account.user': '使用你自己的OpenAI的账号',
    'llm.account.user.api': '你自己的OpenAI API KEY',
    'llm.account.user.api.placeholder': 'Key...',
    'llm.settings': '激活/停用以下模块中的大语言模型',
  },
};
export default i18n;
