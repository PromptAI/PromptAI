import React, { useCallback } from 'react';
import MonacoEditor, { MonacoEditorProps } from 'react-monaco-editor';
import GitHubDark from 'monaco-themes/themes/GitHub Dark.json';
import GitHubLight from 'monaco-themes/themes/GitHub Light.json';
import { useGlobalContext } from '@/context';

const CodeEditor = (props: MonacoEditorProps) => {
  const { theme } = useGlobalContext();
  const editorWillMount = useCallback(
    (monaco) => {
      monaco.editor.defineTheme(
        'monokai',
        theme == 'dark' ? (GitHubDark as any) : (GitHubLight as any)
      );
      props.editorWillMount?.(monaco);
    },
    [props, theme]
  );
  return (
    <MonacoEditor
      {...props}
      theme="monokai"
      editorWillMount={editorWillMount}
      options={{ selectOnLineNumbers: true, fontSize: 16, ...props.options }}
    />
  );
};

export default CodeEditor;
