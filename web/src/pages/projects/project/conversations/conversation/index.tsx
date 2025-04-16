import * as React from 'react';
import { NodeDefined } from '@/core-next/types';
import GraphNext from '@/graph-next';
import { Spin } from '@arco-design/web-react';
import useUrlParams from '../../hooks/useUrlParams';
import CopyAndTrash from './CopyAndTrash';
import ErrorPanel from './components/ErrorPanel';
import { DebugRobot, Tool as DebugTool } from '../../components/debug';
import { useGraphStore } from './store/graph';
import {
  BotNode,
  FlowNode,
  UserNode,
  ConditionNode,
  GotoNode,
} from './views';
import { debounce } from 'lodash';
import { Tool, useTools } from '@/components/Layout/tools-context';
import ru, { Redo, RedoUndoManager, Undo } from './features/ru';
import { DragContextProvider } from './dnd';
import { FullScreen } from './features/fullsreen';
import { Zoom } from './features/zoom';
import DownloadRasaFile from '../../components/DownloadRasaFile';
import { useProjectType } from '@/layout/project-layout/context';
import { GptNode } from './views/gpt/gpt';

export const nodeConfig: Record<string, NodeDefined> = {
  conversation: { component: FlowNode },
  user: { component: UserNode },
  bot: { component: BotNode },
  // form: { component: FormNode },
  // confirm: { component: ConfirmNode },
  condition: { component: ConditionNode },
  goto: { component: GotoNode },
  // 'form-gpt': { component: GptFormNode },
  // 'abort-gpt': { component: GptAbortNode },
  // 'complete-gpt': { component: GptCompletedNode },
  gpt: { component: GptNode },
};

const Conversation = () => {
  const { flowId, projectId } = useUrlParams();
  const containerRef = React.useRef<HTMLDivElement>();
  const graphRef = React.useRef<HTMLDivElement>();
  const { loading, nodes, originNodes, setSelection, refreshNodes, initial } =
    useGraphStore(
      ({
        loading,
        nodes,
        originNodes,
        initial,
        setSelection,
        refreshNodes,
      }) => ({
        loading,
        nodes,
        originNodes,
        initial,
        setSelection,
        refreshNodes,
      })
    );

  // 标记第一个节点
  const newNodes = nodes.map((node, index) => index === 0 ? { ...node, first: true } : node);
  React.useEffect(
    () => initial({ flowId, projectId }),
    [flowId, initial, projectId]
  );
  React.useEffect(() => {
    const resize = new ResizeObserver(debounce(() => refreshNodes(), 200));
    resize.observe(containerRef.current);
    return () => resize.disconnect();
  }, [refreshNodes]);
  React.useEffect(() => {
    ru.setManager(new RedoUndoManager(flowId, 20));
    return () => ru.destoryManager();
  }, [flowId]);
  const type = useProjectType();

  const tools = React.useMemo<Tool[]>(
    () => [
      { key: 'redo', component: <Redo /> },
      { key: 'undo', component: <Undo /> },
      {
        key: 'fullscreen',
        component: <FullScreen containerRef={graphRef} />,
      },
      { key: 'zoom', component: <Zoom /> },
      { key: 'debug', component: <DebugTool current={flowId} /> },
      {
        key: 'download',
        component: <DownloadRasaFile flowId={flowId} />,
      },
    ],
    [flowId, type]
  );
  useTools(tools);
  return (
    <div ref={containerRef} className="select-none">
      <DragContextProvider>
        <CopyAndTrash flowId={flowId}>
          <Spin loading={loading} className="w-full h-full">
            <GraphNext
              name="flow-graph"
              ref={graphRef}
              width="100%"
              height="calc(100vh - 100px)"
              // value={nodes}
              value={newNodes}
              onSelect={setSelection}
              onDoubleSelect={setSelection}
              onCanvasClick={() => setSelection(null)}
              nodes={nodeConfig}
            >
              <ErrorPanel nodes={originNodes} />
            </GraphNext>
          </Spin>
        </CopyAndTrash>
      </DragContextProvider>
      <DebugRobot current={flowId} projectId={projectId} />
    </div>
  );
  // return (
  //   <div ref={containerRef} className="select-none">
  //     <DragContextProvider>
  //       <CopyAndTrash flowId={flowId}>
  //         <Favorites type="conversation">
  //           <Spin loading={loading} className="w-full h-full">
  //             <GraphNext
  //               name="flow-graph"
  //               ref={graphRef}
  //               width="100%"
  //               height="calc(100vh - 100px)"
  //               value={nodes}
  //               onSelect={setSelection}
  //               onDoubleSelect={setSelection}
  //               onCanvasClick={() => setSelection(null)}
  //               nodes={nodeConfig}
  //             >
  //               <ErrorPanel nodes={originNodes} />
  //             </GraphNext>
  //           </Spin>
  //           {/* {!editSelection && (
  //               <Keyboard
  //                 selection={selection}
  //                 refreshGraph={refresh}
  //                 onChangeEdit={onDoubleSelect}
  //                 onChangeSelected={onSelect}
  //               />
  //             )} */}
  //         </Favorites>
  //       </CopyAndTrash>
  //     </DragContextProvider>
  //     <DebugRobot current={flowId} projectId={projectId} />
  //   </div>
  // );
};

export default Conversation;
