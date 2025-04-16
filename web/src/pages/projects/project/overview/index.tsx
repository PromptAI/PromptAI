import * as React from 'react';
import useUrlParams from '../hooks/useUrlParams';
import { NodeDefined } from '@/core-next/types';
import { Spin } from '@arco-design/web-react';
import GraphCore from '@/core-next';
import { useGraphStore } from './store';
import { debounce } from 'lodash';
import { Tool, useTools } from '@/components/Layout/tools-context';
import { DebugRobot, Tool as DebugRunTool } from '../components/debug';
import DownloadRasaFile from '../components/DownloadRasaFile';
import {
  BranchNode,
  ComplexNode,
  FallbackNode,
  ProjectNode,
  SampleNode,
} from './views';
import { useProjectType } from '@/layout/project-layout/context';

const nodesConfig: Record<string, NodeDefined> = {
  project: { component: ProjectNode },
  sample: { component: SampleNode },
  fallback: { component: FallbackNode },
  complex: { component: ComplexNode },
  conversation: { component: BranchNode },
};

// eslint-disable-next-line @typescript-eslint/no-empty-interface
interface OverviewProps {}
const Overview: React.FC<OverviewProps> = () => {
  const containerRef = React.useRef<HTMLDivElement>();
  const { projectId } = useUrlParams();
  const { loading, nodes, setSelection, initial, refreshNodes } = useGraphStore(
    ({ loading, nodes, setSelection, initial, refreshNodes }) => ({
      loading,
      nodes,
      setSelection,
      initial,
      refreshNodes,
    })
  );
  React.useEffect(() => initial({ projectId }), [projectId, initial]);
  React.useEffect(() => {
    const resize = new ResizeObserver(debounce(() => refreshNodes(), 200));
    resize.observe(containerRef.current);
    return () => resize.disconnect();
  }, [refreshNodes]);
  const type = useProjectType();

  const tools = React.useMemo<Tool[]>(
    () => [
      { key: 'run', component: <DebugRunTool current="" disabledCurrent /> },
      type === 'rasa' && {
        key: 'download-rasa',
        component: <DownloadRasaFile />,
      },
    ],
    [type]
  );
  useTools(tools);
  const componentIds = React.useMemo(() => nodes.map((n) => n.id), [nodes]);

  return (
    <div ref={containerRef} className="select-none">
      <Spin loading={loading} className="w-full">
        <GraphCore
          width="100%"
          value={nodes}
          nodes={nodesConfig}
          name="overview-graph"
          onSelect={setSelection}
          height="calc(100vh - 100px)"
          onDoubleSelect={setSelection}
          onCanvasClick={() => setSelection(null)}
          canvasClassName="flex justify-center items-center h-full pr-16"
        />
      </Spin>
      <DebugRobot projectId={projectId} componentIds={componentIds} />
    </div>
  );
};

export default Overview;
