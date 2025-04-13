package com.zervice.kbase.database;


import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * DependencySorter sorts a set of string by constrains:
 * example:
 * constrains:
 *  {"a", ["d", "f"]}
 *  {"d", []}
 *  {"f", ["c"]}
 *  {"c", []}
 *  "a","d","f","c" is the string we need to sort.
 *  [] is the constrains we need to follow when sorting the string list
 *  {"a", ["d", "f"]} means "a" must be behind "d" and "f"
 *  {"f", ["c"]} means "f" must be behind "c"
 *  So after sorting, the result can be:"c", "f", "d", "a"
 *
 *  And if some constrains are conflict, we need to throw an exception:
 *  example:
 *  constrains:
 *   {"a" : ["b"]}
 *   {"b" : ["a"]}
 */

public class DependencySorter {
    private Digraph _digraph;
    private HashMap<String, Integer> _vertexStringMap;

    public DependencySorter(List<Pair<String, String[]>> constraints) {
        HashMap<String, Integer> vertexStrings = new HashMap<String, Integer>();
        int index = 0;
        for(Pair<String, String[]> constraint: constraints) {
            vertexStrings.put(constraint.getKey(), index);
            index++;
        }

        _digraph = new Digraph(constraints.size());
        for(Pair<String, String[]> constraint: constraints) {
            String key = constraint.getKey();

            String[] afterConstraints = constraint.getValue();
            for(int i = 0; i < afterConstraints.length; i++) {
                Integer indexOfStartVertex = vertexStrings.get(key);
                Integer indexOfEndVertex = vertexStrings.get(afterConstraints[i]);
                if(indexOfEndVertex == null) {
                    throw new IllegalArgumentException("Can't find string " + afterConstraints[i] + " in vertex");
                }
                _digraph.addEdge(indexOfStartVertex, indexOfEndVertex);
            }
        }

        _vertexStringMap = vertexStrings;
    }

    public List<String> sort() {
        KahnTopological kahnTopological = new KahnTopological(_digraph);
        List<Integer> indexResult = kahnTopological.getResult();
        List<String> result = new ArrayList<String>();
        //transform index result to string result
        //And reverse the result
        for(int i = indexResult.size() - 1; i >= 0; i--) {
            for(Map.Entry<String, Integer> entry : _vertexStringMap.entrySet()) {
                if(indexResult.get(i).intValue() == entry.getValue()) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }
}

