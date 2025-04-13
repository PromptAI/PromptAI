package com.zervice.kbase.database;
import java.util.ArrayList;
import java.util.List;

/*
 * To store the data of digraph
 */
public class Digraph
{
    private final int _verticesCount;
    private int _edgeCount;
    private List<Integer>[] _edges;

    @SuppressWarnings("unchecked")
    public Digraph(int verticesCount)
    {
        this._verticesCount = verticesCount;
        _edges = (List<Integer>[]) new ArrayList[verticesCount];
        for(int i = 0; i < verticesCount; i++)
            _edges[i] = new ArrayList<Integer>();
    }

    public void addEdge(int v, int w)
    {
        _edges[v].add(w);
        _edgeCount++;
    }

    public Digraph reverse()
    {
        Digraph reversed = new Digraph(_verticesCount);
        for(int v = 0; v < _verticesCount; v++)
        {
            // for each v -> w
            for(int w : _edges[v])
            {
                reversed.addEdge(w, v);
            }
        }

        return reversed;
    }

    public List<Integer> getEdges(int v)
    {
        return _edges[v];
    }

    public int getVerticesCount()
    {
        return _verticesCount;
    }

    public int getEdgeCount()
    {
        return _edgeCount;
    }

    public List<Integer>[] getEdges()
    {
        return _edges;
    }
}
