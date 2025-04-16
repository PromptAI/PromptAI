import useLocale from '@/utils/useLocale';
import React, { memo } from 'react';
import { useHotkeys } from 'react-hotkeys-hook';
import useUrlParams from '../../../hooks/useUrlParams';
import { useCopy } from '../copy/context';
import { createHandleDelFunc, normalGraphNode } from '../nodes/util';
import { useRedoUndo } from '../ru_context';
import i18n from './locale';

interface KeyboardProps {
  refreshGraph: () => void;
  selection: any;
  onChangeEdit: (node: any) => void;
  onChangeSelected: (node: any) => void;
}
const Keyboard = ({
  refreshGraph,
  selection,
  onChangeEdit,
  onChangeSelected,
}: KeyboardProps) => {
  const t = useLocale(i18n);
  const { projectId, flowId } = useUrlParams();
  const { RU } = useRedoUndo();
  const { submit: submitCopy } = useCopy();
  /// keyboard map
  /// ******
  useHotkeys(['ctrl+z', 'meta+z'], () => RU.undo().then(refreshGraph), []);
  /// ******
  useHotkeys(
    ['ctrl+y', 'meta+shift+z'],
    () => RU.redo().then(refreshGraph),
    []
  );
  /// ******
  useHotkeys(
    ['delete', 'backspace'],
    () => {
      if (selection) {
        const handle = createHandleDelFunc({
          title: t['flow.node.delete'],
          content: t['flow.node.delete.content'],
          projectId,
          flowId,
          node: selection,
          RU,
          refresh: refreshGraph,
          onChangeEditSelection: onChangeEdit,
          onChangeSelection: onChangeSelected,
        });
        handle?.();
      }
    },
    [selection, projectId, flowId, t]
  );
  /// ******
  useHotkeys(
    ['ctrl+c', 'meta+c'],
    () =>
      ['user', 'option', 'bot'].includes(selection.type) &&
      submitCopy({
        key: selection.id,
        type: selection.type,
        data: { breakpoint: normalGraphNode(selection) },
      }),
    [selection]
  );
  return <div />;
};

export default memo(Keyboard);
