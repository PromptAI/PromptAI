package com.zervice.kbase.database;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * This is an algorithm to topological sort for digraph
 * To see more: https://blog.csdn.net/dm_vincent/article/details/7714519
 */
public class KahnTopological {
    private List<Integer> _result;   // save result
    private Queue<Integer> _setOfZeroIndegree;  // save the vertices which indegree is 0
    private int[] _indegrees;  // save the indegree of each vertex
    private int _edgeCount;
    private Digraph _di;

    public KahnTopological(Digraph di)
    {
        this._di = di;
        this._edgeCount = di.getEdgeCount();
        this._indegrees = new int[di.getVerticesCount()];
        this._result = new ArrayList<Integer>();
        this._setOfZeroIndegree = new LinkedList<Integer>();

        // initial indegrees and setOfZeroIndegree
        List<Integer>[] edges = di.getEdges();
        for(int i = 0; i < edges.length; i++)
        {
            // for each edge
            for(int w : edges[i])
            {
                _indegrees[w]++;
            }
        }

        for(int i = 0; i < _indegrees.length; i++)
        {
            if(0 == _indegrees[i])
            {
                _setOfZeroIndegree.add(i);
            }
        }
        process();
    }

    private void process()
    {
        while(!_setOfZeroIndegree.isEmpty())
        {
            int v = _setOfZeroIndegree.remove();

            // add v to result
            _result.add(v);

            // transverse each edge of v
            for(int w : _di.getEdges(v))
            {
                // remove v from digraph
                _edgeCount--;
                if(0 == --_indegrees[w])   // if indegrees is 0, add it to setOfZeroIndegree
                {
                    _setOfZeroIndegree.add(w);
                }
            }
        }
        // if there is edge in digraph, it means there is a cycle
        if(0 != _edgeCount)
        {
            throw new IllegalArgumentException("Has cycle in digraph");
        }
    }

    public List<Integer> getResult()
    {
        return _result;
    }
}

