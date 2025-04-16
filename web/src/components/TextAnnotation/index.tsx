import React, { useMemo, useState } from 'react';
import {
  Editable,
  withReact,
  Slate,
  RenderElementProps,
  RenderLeafProps,
} from 'slate-react';
import { Transforms, Editor, Range, createEditor, Descendant } from 'slate';
import { withHistory } from 'slate-history';
import { ButtonElement, CombinedElement } from './type';
import { useCallback } from 'react';

const Element: React.FC<RenderElementProps> = (props) => {
  const { attributes, children, element } = props;
  switch (element.type) {
    case 'button':
      return <b style={{ color: 'red' }} {...props} />;
    default:
      return (
        <p style={{ margin: 0 }} {...attributes}>
          {children}
        </p>
      );
  }
};

const Text: React.FC<RenderLeafProps> = (props) => {
  const { attributes, children, leaf } = props;
  return (
    <span
      style={leaf.text === '' ? { paddingLeft: '0.1px' } : null}
      {...attributes}
    >
      {children}
    </span>
  );
};

const containerStyle = {
  width: '100%',
  margin: ' 0 auto',
  padding: '4px 0px 4px 12px',
  backgroundColor: 'var(--color-fill-2)',
};
const withInline = (editor: Editor) => {
  const { isInline } = editor;

  editor.isInline = (element: CombinedElement) =>
    ['button'].includes(element.type) || isInline(element);

  return editor;
};

function unnormalize({ text, marks }: TextAnnotationValue): Descendant[] {
  let textSplit = 0;
  let children: Descendant[] = [];
  if (marks && marks.length > 0) {
    children = marks
      .sort((o, op) => o.start - op.start)
      .map(({ start, end }) => {
        const target: Descendant[] = [
          { text: text.slice(textSplit, start) },
          { type: 'button', children: [{ text: text.slice(start, end) }] },
        ];
        textSplit = end;
        return target;
      })
      .reduce((p, c) => [...p, ...c], []);
  }
  const last = text.slice(textSplit);
  children.push({ text: last });
  return [
    {
      type: 'paragraph',
      children,
    },
  ];
}

function normalize(value: Descendant[]): TextAnnotationValue {
  const tree = JSON.parse(JSON.stringify(value));
  const intent: TextAnnotationValue = {
    text: '',
    marks: [],
  };
  let node;
  while ((node = tree.shift())) {
    if (node.type === 'paragraph') {
      tree.unshift(...node.children);
    } else if (node.type === 'button') {
      const start: number = intent.text.length;
      intent.text += node.children[0].text;
      intent.marks.push({
        start,
        end: start + node.children[0].text.length,
      });
    } else {
      intent.text += node.text;
    }
  }
  intent.text = intent.text.trim();
  return intent;
}

interface Mark {
  start: number;
  end: number;
}

interface TextAnnotationValue {
  text: string;
  marks: Mark[];
}

interface TextAnnotationProps {
  disabled?: boolean;
  value?: TextAnnotationValue;
  onChange?: (val: TextAnnotationValue) => void;
  placeholder?: string;
}

const defaultText: TextAnnotationValue = { text: '', marks: [] };
const defaultOnChange = () => null;

const TextAnnotation = ({
  disabled = false,
  value = defaultText,
  onChange = defaultOnChange,
  placeholder,
}: TextAnnotationProps) => {
  const [editorValue, setValue] = useState<Descendant[]>(() =>
    unnormalize(value)
  );
  const editor = useMemo(
    () => withInline(withHistory(withReact(createEditor()))),
    []
  );
  const onEditChange = useCallback(
    (val) => {
      if (disabled) return;
      setValue(val);
      onChange(normalize(val));
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [disabled]
  );
  return (
    <div style={containerStyle}>
      <Slate editor={editor} value={editorValue} onChange={onEditChange}>
        <Editable
          readOnly={disabled}
          renderElement={(props) => <Element {...props} />}
          renderLeaf={(props) => <Text {...props} />}
          placeholder={placeholder}
          autoFocus
          onSelectCapture={() => {
            const { selection } = editor;
            const isCollapsed = selection && Range.isCollapsed(selection);
            if (isCollapsed) return;
            const button: ButtonElement = {
              type: 'button',
              children: [],
            };
            const windSelection = window.getSelection();
            const text = windSelection?.toString() as any;
            const endLength = text.length - text.trimEnd().length;
            const startLength = text.length - text.trimStart().length;

            if (text && selection && editor && text.trim().length) {
              if (selection.anchor.offset < selection.focus.offset) {
                if (startLength) {
                  Transforms.move(editor, {
                    distance: startLength,
                    unit: 'offset',
                    reverse: false,
                    edge: 'anchor',
                  });
                }
                if (endLength) {
                  Transforms.move(editor, {
                    distance: endLength,
                    unit: 'offset',
                    reverse: true,
                    edge: 'focus',
                  });
                }
              }
              if (selection.anchor.offset > selection.focus.offset) {
                if (startLength) {
                  Transforms.move(editor, {
                    distance: startLength,
                    unit: 'offset',
                    reverse: false,
                    edge: 'focus',
                  });
                }
                if (endLength) {
                  Transforms.move(editor, {
                    distance: endLength,
                    unit: 'offset',
                    reverse: true,
                    edge: 'anchor',
                  });
                }
              }
              Transforms.unwrapNodes(editor, {
                at: [],
                match: (node) =>
                  !Editor.isEditor(node) &&
                  'type' in node &&
                  node.type === 'button',
                mode: 'all',
              });

              Transforms.wrapNodes(editor, button, { split: true });
              Transforms.collapse(editor, { edge: 'end' });
            }
          }}
        />
      </Slate>
    </div>
  );
};

export default TextAnnotation;
