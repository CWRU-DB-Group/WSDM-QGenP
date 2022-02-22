package MovieLensGraph;

import java.io.IOException;
import java.util.*;

import Infra.*;

import org.jgrapht.Graph;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.GraphMapping;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;

import java.util.Comparator;

public class QGen {


    Graph<DataNode, RelationshipEdge> dataGraph;
    VF2AbstractIsomorphismInspector<DataNode, RelationshipEdge> inspector;
    Comparator<DataNode> myNodeComparator;
    Comparator<RelationshipEdge> myEdgeComparator;
    HashMap<Integer, DataNode> nodeMap;
    Graph<DataNode, RelationshipEdge> mergedGraph;
    HashSet<String> places;
    ArrayList<Graph<DataNode, RelationshipEdge>> res;
    double sumP;
    HashSet<DataNode> candidates;

    public QGen(MovieDataGraph dataGraph) {

        this.dataGraph = dataGraph.getDataGraph();
        this.nodeMap = dataGraph.getNodeMap();
        mergedGraph = new DefaultDirectedGraph<>(RelationshipEdge.class);
        places = new HashSet<>();
        res = new ArrayList<>();
        candidates = new HashSet<>();

        myEdgeComparator = new Comparator<RelationshipEdge>() {
            @Override
            public int compare(RelationshipEdge e1, RelationshipEdge e2) {

                if (e1.getLabel().equals(e2.getLabel())) {
                    return 0;
                } else {
                    return 1;
                }

            }

        };

        myNodeComparator = new Comparator<DataNode>() {
            @Override
            public int compare(DataNode n1, DataNode n2) {
                if (hashIntersection(n1.types, n2.types)) {
                    for (Predicate p : n2.preds) {
                        if (!staisfyP(p, n1)) {
                            return 1;
                        }
                    }
                    return 0;
                } else {
                    return 1;
                }
            }

        };

    }

    public void getInducedGraph(Graph<DataNode, RelationshipEdge> pattern, String uoType) {



        DataNode curNode = null;
        for (DataNode node : pattern.vertexSet()) {
            if (node.isRoot) {
                curNode = node;
                break;
            }
        }
        // post the predefined search condition, e.g. Nation of the file is india.
        //


        HashSet<String> types = curNode.types;
        for (DataNode rootNode : dataGraph.vertexSet()) {
            if (!rootNode.attributes.containsKey("Nation")) {
                continue;
            }
            if (!rootNode.attributes.get("Nation").contains("India")){
                continue;
            }


            if (hashIntersection(rootNode.types, types)) {
                res.add(generateSubgraph(rootNode, 2));
                candidates.add(rootNode);
            }
        }

        System.out.println(res.size() + "glsize");

    }

    private Graph<DataNode, RelationshipEdge> generateSubgraph(DataNode rootNode, int d) {
        Graph<DataNode, RelationshipEdge> curGraph = new DefaultDirectedGraph<>(RelationshipEdge.class);
        Queue<DataNode> queue = new LinkedList<>();
        queue.add(rootNode);
        int level = 0;
        while (level <= d) {

            int size = queue.size();
            for (int i = 0; i < size; i++) {
                DataNode cur = queue.poll();
                if (!curGraph.containsVertex(cur)) {
                    curGraph.addVertex(cur);
                }

                for (RelationshipEdge edge : dataGraph.outgoingEdgesOf(cur)) {
                    queue.add(dataGraph.getEdgeTarget(edge));
                    if (!curGraph.containsVertex(dataGraph.getEdgeTarget(edge))) {
                        curGraph.addVertex(dataGraph.getEdgeTarget(edge));

                    }
                    curGraph.addEdge(cur, dataGraph.getEdgeTarget(edge), new RelationshipEdge(edge.getLabel()));
                }
            }
            level++;
        }

        return curGraph;
    }

    private boolean staisfyP(Predicate p, DataNode n1) {

        if (!n1.attributes.containsKey(p.attr)) {
            return false;
        }
        if (p.attrType.equals("double")) {
            double variable = 0.0;
            String valueStr = (String) n1.attributes.get(p.attr).toArray()[0];
            if (p.attr.equals("Year")) {
                variable = Double.parseDouble(valueStr.substring(0, 4));
            } else if (p.attr.equals("Rating") ) {
                variable = Double.parseDouble(valueStr);
            } else {
                variable = Double.parseDouble((String) n1.attributes.get(p.attr).toArray()[0]);
            }
            double value = Double.parseDouble(p.value);
            if (p.op.equals("l")) {
                return (variable > value);
            } else if (p.op.equals("le")) {
                return (variable > value || variable == value);
            } else if (p.op.equals("s")) {
                return (variable < value);
            } else if (p.op.equals("se")) {
                return (variable < value || variable == value);
            } else {
//                System.out.println(variable + " " + value);
//                System.out.println(variable==value);
                return (variable == value);

            }
        } else {
            return (n1.attributes.get(p.attr).contains(p.value));
        }
    }

    private boolean hashIntersection(HashSet<String> types, HashSet<String> types1) {
        for (String type : types) {
            if (types1.contains(type)) {
                return true;
            }

        }
        return false;
    }

    public HashSet<DataNode> executeInit(Graph<DataNode, RelationshipEdge> pattern) {



        long startTime = System.nanoTime();
        inspector = new VF2SubgraphIsomorphismInspector<>(this.mergedGraph, pattern,
                myNodeComparator, myEdgeComparator, false);

        HashSet<DataNode> currentMatch = new HashSet<>();
        if (inspector.isomorphismExists()) {

            Iterator<GraphMapping<DataNode, RelationshipEdge>> iterator = inspector.getMappings();
            ArrayList<DataNode> patternTypes = new ArrayList<>();
            DataNode rootNode = null;
            DataNode personNode = null;

            for (DataNode node : pattern.vertexSet()) {
                patternTypes.add(node);
            }

            for (DataNode node : pattern.vertexSet()) {

                if (node.isRoot = true) {
                    rootNode = node;
//                    System.out.println(node.types);
//                    System.out.println(node.preds.get(0).value);
                    break;
                }
            }

            while (iterator.hasNext()) {
                org.jgrapht.GraphMapping<DataNode, RelationshipEdge> mappings = iterator.next();
                DataNode currentMatchedNode = mappings.getVertexCorrespondence(rootNode, false);
                currentMatch.add(currentMatchedNode);
            }
        }


        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        System.out.println("Match Size:" + currentMatch.size());
        return currentMatch;

    }

    public HashSet<DataNode> execute(Graph<DataNode, RelationshipEdge> pattern, XVariable X) {


        for (Predicate p : X.predicates) {
            for (DataNode pnode : pattern.vertexSet()) {
                if (pnode.getNodeName().equals(p.nodeName)) {
                    pnode.preds.add(p);
                }
            }
        }

        HashSet<DataNode> currentMatch = new HashSet<>();
        long startTime = System.nanoTime();
        for (Graph<DataNode, RelationshipEdge> cur : res) {

            inspector = new VF2SubgraphIsomorphismInspector<>(cur, pattern,
                    myNodeComparator, myEdgeComparator, false);


            if (inspector.isomorphismExists()) {

                Iterator<GraphMapping<DataNode, RelationshipEdge>> iterator = inspector.getMappings();
                ArrayList<DataNode> patternTypes = new ArrayList<>();
                DataNode rootNode = null;

                for (DataNode node : pattern.vertexSet()) {
                    patternTypes.add(node);
                }

                for (DataNode node : pattern.vertexSet()) {

                    if (node.isRoot = true) {
                        rootNode = node;
//                        System.out.println(node.types);
//                        System.out.println(node.preds.get(0).value);
                        break;
                    }
                }


                while (iterator.hasNext()) {
                    org.jgrapht.GraphMapping<DataNode, RelationshipEdge> mappings = iterator.next();
                    DataNode currentMatchedNode = mappings.getVertexCorrespondence(rootNode, false);
                    currentMatch.add(currentMatchedNode);

                }


            }
        }


        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        System.out.println("Match Size:" + currentMatch.size());

        return currentMatch;

    }

    public double getGroup(String label, ArrayList<String> group) {


        int count = 0;
        HashSet<DataNode> dup = new HashSet<>();

        for (int i = 0; i < group.size(); i++) {

            for (DataNode node : candidates) {
                if (!node.types.contains(label)) {
                    continue;
                }
                String type = group.get(i);
                if (!node.attributes.containsKey("Category")) {
                    continue;
                }
                if (node.attributes.get("Category").contains(type)) {
                    count++;
                }
            }
        }

        return count;
    }



    public void runKGen(Graph<DataNode, RelationshipEdge> pattern, int k) {


        ArrayList<String> group = new ArrayList<>();
        group.add("Comedy");
        group.add("Romance");
        //ArrayList<ArrayList<String>> result = cardinalityResult(currentMatch, group);

        ArrayList<Integer> cc = new ArrayList<>();
        cc.add(50);
        cc.add(50);


        ArrayList<XVariable> xList = enumerateAll();
        ArrayList<HashSet<DataNode>>  groupsM = new ArrayList<>();
        HashSet<DataNode> duplicate = new HashSet<>();



        for (int i = 0; i < group.size(); i++) {
            String type = group.get(i);
            HashSet<DataNode> curG = new HashSet<>();
            for (DataNode node : candidates) {
                if (!node.attributes.containsKey("Category")) {
                    continue;
                }
                if (node.attributes.get("Category").contains(type) && !duplicate.contains(node)) {
                    duplicate.add(node);
                    curG.add(node);
                }
            }
            groupsM.add(curG);
            System.out.println(curG.size() + "Add");
        }

        ArrayList<HashSet<DataNode>> cacheSet = new ArrayList<>();

        ArrayList<Pair> totalSets = new ArrayList<>();
        Graph<DataNode, RelationshipEdge> pattern1 = generatePattern();
        Graph<DataNode, RelationshipEdge> pattern2 = generatePattern1();
        for (XVariable x: xList) {

            totalSets.add(new Pair(x,pattern1,null));
            totalSets.add(new Pair(x,pattern2,null));
        }

        int count = 0;

        for (int i = 0;  i<totalSets.size(); i++) {


            if (count < k) {
                HashSet<DataNode> curmatch = execute(totalSets.get(i).pattern,totalSets.get(i).X);
                cacheSet.add(curmatch);
                for (HashSet<DataNode> set : groupsM) {
                    set.removeAll(curmatch);
                }
                count++;
                continue;

            } else  {

                HashSet<DataNode> newQM = execute(pattern, xList.get(i));
                double min= Double.MAX_VALUE;
                for (HashSet<DataNode> set : cacheSet) {
                    double curDive = getDiv(set);
                    if (min > curDive) {
                        min = curDive;
                    }
                }
                double newQMDive = getDiv(newQM);
                if (UnionOfSets(groupsM).size() == 0) {
                    System.out.println(getDelta(UnionOfSets(cacheSet), group, cc) / sumP  + "early" + UnionOfSets(groupsM).size());
                    return;
                }
                if (NoOfIntersection(UnionOfSets(cacheSet),newQM) == newQM.size()) {
                    continue;
                }
                ArrayList<HashSet<DataNode>> cacheSetCopy = new ArrayList<>(cacheSet);
                HashSet<DataNode> curActualMatch = execute(pattern,xList.get(i));
                double curF = getDiv(UnionOfSets(cacheSet));
                double curDelta = getDelta(UnionOfSets(cacheSet),group,cc);
                double dmaxF = Double.MIN_VALUE;
                double dmaxD = Double.MIN_VALUE;
                HashSet<DataNode> toRemove = null;
                for (HashSet<DataNode> set : cacheSet) {
                    cacheSetCopy.remove(set);
                    cacheSetCopy.add(curActualMatch);
                    double curFGain = getDiv(UnionOfSets(cacheSetCopy)) - curF;
                    double curDGain = curDelta -  getDelta(UnionOfSets(cacheSetCopy),group,cc);

                    if (curDGain > 0) {
                        if (curDGain > dmaxD) {
                            dmaxD = curDGain;
                            toRemove = set;
                        }
                    }

                    if (curFGain > 0) {
                       if (curFGain > dmaxF) {
                           dmaxF = curFGain;
                           toRemove = set;
                       }
                    }



                    cacheSetCopy.remove(curActualMatch);
                    cacheSetCopy.add(set);
                }

                if (toRemove != null) {
                    System.out.println("Switch");
                    cacheSet.remove(toRemove);
                    cacheSet.add(curActualMatch);
                } else {
                    break;
                }


            }



        }

        System.out.println(sumP);
        System.out.println(getDelta(UnionOfSets(cacheSet), group, cc) / sumP);
        System.out.println("TopK");
        return;
    }

    public void runEnum(Graph<DataNode, RelationshipEdge> pattern,ArrayList<String> group,ArrayList<Integer> cc) {

        ArrayList<HashSet<DataNode>> sets = generateSet(pattern);
        ArrayList<HashSet<DataNode>> setsCopy = new ArrayList<>(sets);
        for (HashSet<DataNode> s: sets) {
            System.out.println(s.size());
        }
        System.out.println(setsCopy.size());
        Collections.reverse(setsCopy);

        ArrayList<HashSet<DataNode>> curSet = new ArrayList<>();
        for (int i = 0; i < setsCopy.size(); i++) {
            curSet.add(setsCopy.get(i));
            double curD = getDelta(UnionOfSets(curSet),group,cc);
            if (curD > 0) {
                System.out.println(curD/sumP + " " + i);
                System.out.println(getDiv(UnionOfSets(curSet)));
                System.out.println("---------------");

            }
        }


//        ArrayList<ArrayList<HashSet<DataNode>>> subsets = subsets(setsCopy,60,group,cc);


    }


    private boolean isValid(HashSet<DataNode> set, ArrayList<String> group) {

        for (int i = 0; i < group.size(); i++) {
            String g = group.get(i);
            for (DataNode node : set) {
                if (node.attributes.containsKey("Category")) {
                   if ( node.attributes.get("Category").contains(g)) {
                       return true;
                   }

                }

            }

        }
        return false;
    }

    private ArrayList<ArrayList<HashSet<DataNode>>> subsets(ArrayList<HashSet<DataNode>> sets,
                                                            int k,ArrayList<String> group,ArrayList<Integer> cc) {
        ArrayList<ArrayList<HashSet<DataNode>>> list = new ArrayList<>();
        subsetsHelper(list, new ArrayList<>(), sets, 0, k,group,cc);
        return list;

    }

    private void subsetsHelper(ArrayList<ArrayList<HashSet<DataNode>>> list, ArrayList<HashSet<DataNode>> resultList,
                               ArrayList<HashSet<DataNode>> sets, int start, int k, ArrayList<String> group,ArrayList<Integer> cc) {
        if (k <= 0) {
            list.add(new ArrayList<>(resultList));
            System.out.println(getDiv(UnionOfSets(resultList)) + "div");
            System.out.println(resultList.size() + "size");
            System.out.println((getDelta(UnionOfSets(resultList), group,cc)/sumP) + " Delta");
            return;
        }

        for(int i = start; i < sets.size(); i++){
            // add element
            resultList.add(sets.get(i));
            // Explore
            subsetsHelper(list, resultList, sets, i + 1,k-1, group,cc);
            // remove
            resultList.remove(resultList.size() - 1);
        }

    }
//    public void runMaxSumGenOPT1(Graph<DataNode, RelationshipEdge> pattern) {
//
//        ArrayList<String> group = new ArrayList<>();
//        group.add("Comedy");
//        group.add("Romance");
//        //ArrayList<ArrayList<String>> result = cardinalityResult(currentMatch, group);
//
//        ArrayList<Integer> cc = new ArrayList<>();
//        cc.add(50);
//        cc.add(50);
//
//
//        HashSet<DataNode> solution = maxSumGen(group, cc, candidates);
//
//        System.out.println("Size Sp: " + solution.size());
//
//        HashSet<DataNode> blue = solution;
//
//        HashSet<DataNode> red = new HashSet<>();
//
//        for (DataNode node : candidates) {
//            if (!blue.contains(node)) {
//                red.add(node);
//            }
//        }
//        System.out.println("Size Blue: " + blue.size());
//        System.out.println("Size Red: " + red.size());
//
////        ArrayList<Range> rList = new ArrayList<>();
////        rList.add(new Range(1946, 2016, "r"));
//        HashMap<Integer,ArrayList<Pair>> map = new HashMap<>();
//        ArrayList<XVariable> xList = enumerateAll();
//        ArrayList<Pair> totalSets = new ArrayList<>();
//        int max = 0;
//        HashMap<Pair,HashSet<DataNode>> matchMap = new HashMap<>();
//        Graph<DataNode, RelationshipEdge> pattern1 = generatePattern();
//        Graph<DataNode, RelationshipEdge> pattern2 = generatePattern1();
//        for (XVariable x: xList) {
//            HashSet<DataNode> set = overEstimate(x);
//            if (max < set.size()) {
//                max = set.size();
//            }
//            totalSets.add(new Pair(x,pattern1,set));
//            totalSets.add(new Pair(x,pattern2,set));
//        }
//
//        System.out.println(2*xList.size() + " yigong");
//
//        for (int i = 1; i <= max; i++) {
//            for (Pair p : totalSets) {
//                if (NoOfIntersection(p.set,red) == i) {
//                    if (map.containsKey(i)) {
//                        map.get(i).add(p);
//                    } else {
//                        map.put(i, new ArrayList<>());
//                        map.get(i).add(p);
//                    }
//                }
//            }
//        }
//
//
//
//
//        long startTime = System.nanoTime();
//
//        int totalV = 0;
//        for (int i = 1; i <= max; i++) {
//
//            if (!map.containsKey(i)) {
//                continue;
//            }
//            if (map.get(i).size() == 0) {
//                continue;
//            }
//
//            HashSet<DataNode> curTotal = new HashSet<>();
//            for (int j = 0; j < map.get(i).size(); j++) {
//                curTotal.addAll(map.get(i).get(j).set);
//            }
//
//            if (NoOfIntersection(curTotal,blue) < blue.size()) {
//                continue;
//            }
//
//            ArrayList<HashSet<DataNode>> sets = new ArrayList<>();
//
//            for (Pair p : map.get(i)) {
//                if (matchMap.containsKey(p)) {
//                    sets.add(matchMap.get(p));
//
//                } else {
//                    totalV++;
//                    HashSet<DataNode> set = execute(p.pattern,p.X);
//                    matchMap.put(p,set);
//                    sets.add(set);
//                }
//            }
//
//
//
//            ArrayList<HashSet<DataNode>> curRB = RBCover(sets, red, blue, i);
//
//
//            if (curRB == null) {
//                continue;
//            }
//
//            if (curRB.size() != 0) {
//                System.out.println(i + "stop");
//                System.out.println("RB Size" + curRB.size());
//                ArrayList<HashSet<DataNode>> curRBCopy = new ArrayList<>(curRB);
//                for (HashSet<DataNode> set : curRB) {
//                    if (set == null) {
//                        curRBCopy.remove(set);
//                        continue;
//                    }
//                    System.out.println(set.size() + "...");
//                }
//                System.out.println(getDelta(UnionOfSets(curRBCopy), group, cc) / sumP);
//                break;
//            }
//
//        }
//        long endTime = System.nanoTime();
//        long duration = (endTime - startTime);
//        System.out.println(duration + "RBCoverTime");
//        System.out.println(totalV + "totalV");
//
//    }

    private HashSet<DataNode> overEstimate(XVariable x) {

        DataNode node = new DataNode("1");
        node.types.add("Film");
        node.isRoot = true;


        DataNode node1 = new DataNode("2");
        node1.types.add("Person");

        DataNode node2 = new DataNode("3");
        node2.types.add("Person");


        DataNode node3 = new DataNode("4");
        node3.types.add("Person");

//        DataNode node4 = new DataNode("5");
//        node4.types.add("Person");
        Graph<DataNode, RelationshipEdge> pattern1 = new DefaultDirectedGraph<>(RelationshipEdge.class);
//        Graph<DataNode, RelationshipEdge> pattern2 = new DefaultDirectedGraph<>(RelationshipEdge.class);
//        Graph<DataNode, RelationshipEdge> pattern3 = new DefaultDirectedGraph<>(RelationshipEdge.class);

        pattern1.addVertex(node);
//        pattern2.addVertex(node);
//        pattern3.addVertex(node);
//        pattern1.addVertex(node1);
//        pattern2.addVertex(node2);
//        pattern3.addVertex(node3);
//
//        pattern1.addEdge(node, node1, new RelationshipEdge("starring"));
//        pattern2.addEdge(node, node2, new RelationshipEdge("director"));
//        pattern3.addEdge(node, node3, new RelationshipEdge("writer"));
        HashSet<DataNode> m1 = execute(pattern1, x);
//        HashSet<DataNode> m2 = execute(pattern2, x);
//        HashSet<DataNode> m3 = execute(pattern3, x);
//        HashSet<DataNode> inters = getIntersection(m1,m2);
//        inters = getIntersection(inters,m3);


        return m1;

    }



    public void runMaxSumGen(String groupAttr, ArrayList<String> group, ArrayList<Integer> cc,
                             Graph<DataNode, RelationshipEdge> pattern, ArrayList<RangePair> rangePairs, ArrayList<Range> ranges) {



        HashSet<DataNode> solution = maxSumGen(group, cc, candidates,groupAttr);

        System.out.println("Size Sp: " + solution.size());

        HashSet<DataNode> blue = solution;

        HashSet<DataNode> red = new HashSet<>();

        for (DataNode node : candidates) {
            if (!blue.contains(node)) {
                red.add(node);
            }
        }

        System.out.println("Size Blue: " + blue.size());
        System.out.println("Size Red: " + red.size());

        ArrayList<HashSet<DataNode>> sets = generateSet(pattern, rangePairs,ranges);




        long startTime = System.nanoTime();
        for (int i = 1; i <= red.size(); i++) {

            ArrayList<HashSet<DataNode>> curRB = RBCover(sets, red, blue, i);

            if (curRB == null) {
                continue;
            }

            if (curRB.size() != 0) {
                System.out.println(i + "stop");
                System.out.println("RB Size" + curRB.size());
                ArrayList<HashSet<DataNode>> curRBCopy = new ArrayList<>(curRB);
                for (HashSet<DataNode> set : curRB) {
                    if (set == null || set.size() == 0) {
                        curRBCopy.remove(set);
                        continue;
                    }
                    //System.out.println(set.size() + "...");
                }
                System.out.println(sumP + " SumP");
                System.out.println(getDiv(UnionOfSets(curRBCopy)));
                System.out.println(getDelta(UnionOfSets(curRBCopy), group, cc)/sumP);
                System.out.println(sets.size() + " result size");
                break;
            }

        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration + "RBCoverTime");

    }
//
//    private Graph<DataNode, RelationshipEdge> generatePattern2() {
//        Graph<DataNode, RelationshipEdge> pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
//        DataNode node = new DataNode("1");
//        node.types.add("Film");
//        node.isRoot = true;
//
//        DataNode node1 = new DataNode("2");
//        node1.types.add("Person");
//
//        DataNode node2 = new DataNode("3");
//        node2.types.add("Person");
//
//
//        DataNode node3 = new DataNode("4");
//        node3.types.add("Person");
//
//        DataNode node4 = new DataNode("5");
//        node4.types.add("Person");
//
//
//        pattern.addVertex(node);
//        pattern.addVertex(node1);
//        pattern.addVertex(node2);
////        pattern.addVertex(node3);
////        pattern.addVertex(node4);
//        pattern.addEdge(node, node1, new RelationshipEdge("starring"));
//        pattern.addEdge(node, node2, new RelationshipEdge("director"));
////        pattern.addEdge(node, node3, new RelationshipEdge("writer"));
////        pattern.addEdge(node, node4, new RelationshipEdge("producer"));
//        return pattern;
//
//    }

    private Graph<DataNode, RelationshipEdge> generatePattern1() {
        Graph<DataNode, RelationshipEdge> pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
        DataNode node = new DataNode("1");
        node.types.add("Film");
        node.isRoot = true;

        DataNode node1 = new DataNode("2");
        node1.types.add("Person");

        DataNode node2 = new DataNode("3");
        node2.types.add("Person");


        DataNode node3 = new DataNode("4");
        node3.types.add("Person");

        DataNode node4 = new DataNode("5");
        node4.types.add("Person");


        pattern.addVertex(node);
        pattern.addVertex(node1);
        pattern.addVertex(node2);
//        pattern.addVertex(node3);
//        pattern.addVertex(node4);
        pattern.addEdge(node, node1, new RelationshipEdge("starring"));
        pattern.addEdge(node, node2, new RelationshipEdge("director"));
//        pattern.addEdge(node, node3, new RelationshipEdge("writer"));
//        pattern.addEdge(node, node4, new RelationshipEdge("producer"));
        return pattern;

    }

    public double getDelta(HashSet<DataNode> set, ArrayList<String> group, ArrayList<Integer> cc) {


        ArrayList<Integer> cMatch = new ArrayList<>();
        HashSet<DataNode> duplicate = new HashSet<>();


        for (int i = 0; i < group.size(); i++) {
            int count = 0;
            String type = group.get(i);
            for (DataNode node : set) {
                if (!node.attributes.containsKey("Category")) {
                    continue;
                }
                if (node.attributes.get("Category").contains(type) && !duplicate.contains(node)) {
                    duplicate.add(node);
                    count++;
                }
            }

            cMatch.add(count);
        }
        double dist = 0;
        for (int i = 0; i < cc.size(); i++) {
            dist += (cMatch.get(i) - cc.get(i));
        }

        System.out.println(dist + " dist");
        return dist;
    }

    public ArrayList<HashSet<DataNode>> RBCover
            (ArrayList<HashSet<DataNode>> curSets, HashSet<DataNode> red, HashSet<DataNode> blue, int bound) {

        ArrayList<HashSet<DataNode>> sets = new ArrayList<>(curSets);
        int n = sets.size();
        for (HashSet<DataNode> set : curSets) {
            if (NoOfIntersection(set, red) > bound) {
                sets.remove(set);
            }
        }

        if (NoOfIntersection(UnionOfSets(sets), blue) != blue.size()) {
            //System.out.println("Skip " + NoOfIntersection(UnionOfSets(sets), blue) + " " + blue.size());
            return null;
        }
        double Y = Math.sqrt((n / Math.log(blue.size())));

        HashSet<DataNode> RL = new HashSet<DataNode>();
        HashSet<DataNode> RH = new HashSet<DataNode>();

        for (DataNode rNode : red) {
            int count = 0;
            for (HashSet<DataNode> set : sets) {
                if (set.contains(rNode)) {
                    count++;
                }
            }
            if ((double) count > Y) {
                RH.add(rNode);
            } else {
                RL.add(rNode);
            }
        }
        HashMap<HashSet, HashSet> setMap = new HashMap<>();
        ArrayList<HashSet<DataNode>> SX = new ArrayList<>();
        System.out.println("RH" + RH.size());
        System.out.println("RL" + RL.size());

        for (HashSet<DataNode> s : sets) {
            HashSet<DataNode> intersect = getIntersection(s, RH);
            HashSet<DataNode> curSx = new HashSet<>(s);
            curSx.remove(intersect);
            setMap.put(curSx, s);
            SX.add(curSx);
        }

        ArrayList<HashSet<DataNode>> greedySet = greedySelection(SX);
        ArrayList<HashSet<DataNode>> recovered = new ArrayList<>();
        for (HashSet<DataNode> s : greedySet) {
            recovered.add(setMap.get(s));
        }
        return recovered;
    }


    public HashSet<DataNode> estimate(Graph<DataNode, RelationshipEdge> pattern) {

        DataNode curNode = null;
        for (DataNode node : pattern.vertexSet()) {
            if (node.isRoot) {
                curNode = node;
                break;
            }
        }

        Stack<DataNode> stack = new Stack<DataNode>();
        stack.add(curNode);


        return null;
    }

//    private void generatePaths(XVariable x) {
//
//
//        DataNode node = new DataNode("1");
//        node.types.add("Film");
//        node.isRoot = true;
//
//
//        DataNode node1 = new DataNode("2");
//        node1.types.add("Person");
//
//        DataNode node2 = new DataNode("3");
//        node2.types.add("Person");
//
//
//        DataNode node3 = new DataNode("4");
//        node3.types.add("Person");
//
////        DataNode node4 = new DataNode("5");
////        node4.types.add("Person");
//        Graph<DataNode, RelationshipEdge> pattern1 = new DefaultDirectedGraph<>(RelationshipEdge.class);
//        Graph<DataNode, RelationshipEdge> pattern2 = new DefaultDirectedGraph<>(RelationshipEdge.class);
//        Graph<DataNode, RelationshipEdge> pattern3 = new DefaultDirectedGraph<>(RelationshipEdge.class);
//
//        pattern1.addVertex(node);
//        pattern2.addVertex(node);
//        pattern3.addVertex(node);
//        pattern1.addVertex(node1);
//        pattern2.addVertex(node2);
//        pattern3.addVertex(node3);
//
//        pattern1.addEdge(node, node1, new RelationshipEdge("starring"));
//        pattern2.addEdge(node, node2, new RelationshipEdge("director"));
//        pattern3.addEdge(node, node3, new RelationshipEdge("writer"));
//        long startTime = System.nanoTime();
//        HashSet<DataNode> m1 = execute(pattern1, x);
//        HashSet<DataNode> m2 = execute(pattern2, x);
//        HashSet<DataNode> m3 = execute(pattern3, x);
//        HashSet<DataNode> inters = getIntersection(m1,m2);
//        inters = getIntersection(inters,m3);
//        long endTime = System.nanoTime();
//        long duration = (endTime - startTime);
//        System.out.println(duration);
//        System.out.println("InterSize:" + inters.size());
//
//
//    }


    public double getDist(ArrayList<Integer> matchCardinality, ArrayList<Integer> cardnality) {
        double dist = 0.0;
        for (int i = 0; i < matchCardinality.size() - 2; i++) {
            int curMC = matchCardinality.get(i);
            int curCC = cardnality.get(i);
            dist += Math.abs(curMC - curCC);
        }
        return dist;
    }



    private double JaccardDist(String str1, String str2) {
        Set<Character> s1 = new HashSet<>();
        Set<Character> s2 = new HashSet<>();

        for (int i = 0; i < str1.length(); i++) {
            s1.add(str1.charAt(i));
        }
        for (int j = 0; j < str2.length(); j++) {
            s2.add(str2.charAt(j));
        }

        double mergeNum = 0;//Number of union elements
        double commonNum = 0;//Number of same elements (intersection)

        for (Character ch1 : s1) {
            for (Character ch2 : s2) {
                if (ch1.equals(ch2)) {
                    commonNum++;
                }
            }
        }

        mergeNum = s1.size() + s2.size() - commonNum;
        double jaccard = commonNum / mergeNum;
        return jaccard;
    }


    public Graph<DataNode, RelationshipEdge> generatePattern() {

        // Initializing query template regarding JGraph Pattern format

        Graph<DataNode, RelationshipEdge> pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
        DataNode node = new DataNode("1");
        node.types.add("Film");
        node.isRoot = true; // uo pattern node

        DataNode node1 = new DataNode("2");
        node1.types.add("Person");

        DataNode node2 = new DataNode("3");
        node2.types.add("Person");


//        DataNode node3 = new DataNode("4");
//        node3.types.add("Person");
//
//        DataNode node4 = new DataNode("5");
//        node4.types.add("Person");


        pattern.addVertex(node);
        pattern.addVertex(node1);
        pattern.addVertex(node2);
//        pattern.addVertex(node3);
//        pattern.addVertex(node4);
        pattern.addEdge(node, node1, new RelationshipEdge("starring"));
        pattern.addEdge(node, node2, new RelationshipEdge("director"));
//        pattern.addEdge(node, node3, new RelationshipEdge("writer"));
//        pattern.addEdge(node, node4, new RelationshipEdge("producer"));
        return pattern;

    }



    public ArrayList<HashSet<DataNode>> generateSet(Graph<DataNode, RelationshipEdge> pattern, ArrayList<RangePair> rangePairs, ArrayList<Range> ranges) {

        ArrayList<HashSet<DataNode>> sets = new ArrayList<>();
        ArrayList<XVariable> xList = enumerateAll(rangePairs,ranges);
        System.out.println(xList.size());
        for (int i = 0; i < xList.size(); i++) {
            HashSet<DataNode> match = execute(pattern, xList.get(i));
            if (match != null || match.size() != 0) {
                sets.add(match);
            }
        }

        return sets;
    }

    public ArrayList<HashSet<DataNode>> generateSet(Graph<DataNode, RelationshipEdge> pattern) {

        ArrayList<HashSet<DataNode>> sets = new ArrayList<>();
        ArrayList<XVariable> xList = enumerateAll();
        System.out.println(xList.size());
        for (int i = 0; i < xList.size(); i++) {
            HashSet<DataNode> match = execute(pattern, xList.get(i));
            if (match != null || match.size() != 0) {
                sets.add(match);
            }
        }

        return sets;
    }

    private ArrayList<XVariable> enumerateAll(ArrayList<RangePair> rangePairs, ArrayList<Range> ranges) {

        ArrayList<XVariable> Xlist = new ArrayList<>();
//        for (int i = 0; i < ranges.size(); i++) {
//            if (Xlist.size() == 0) {
//
//                for (double start = ranges.get(i).low; start < ranges.get(i).high; start +=1.0) {
//                    XVariable X = new XVariable();
//                    X.predicates.add(new Predicate(rangePairs.get(i).attr,rangePairs.get(i).operator,String.valueOf(start),
//                            "double","1"));
//                }
//            } else {
//
//                    while (Xlist.size() != 0) {
//                        XVariable curX = Xlist.get(0);
//                        for (double start = ranges.get(i).low; start < ranges.get(i).high; start +=1.0) {
//                            XVariable newX = new XVariable();
//                            newX.predicates.addAll(curX.predicates);
//                            newX.predicates.add(new Predicate(rangePairs.get(i).attr,rangePairs.get(i).operator,String.valueOf(start),
//                                    "double","1"));
//                        }
//                        Xlist.remove(0);
//                    }
//            }
//
//        }

        double value = 1950;
        while (value <= 2000) {
            double rValue = 1;
            while (rValue <= 8) {
                XVariable X = new XVariable();
                X.predicates.add(new Predicate("Year", "l", String.valueOf(value), "double", "1"));
                X.predicates.add(new Predicate("Rating", "l", String.valueOf(rValue), "double", "1"));
                Xlist.add(X);
                rValue++;
            }
            value+=5;
        }

        return Xlist;
    }

    private ArrayList<XVariable> enumerateAll() {

        ArrayList<XVariable> Xlist = new ArrayList<>();


        double value = 1916;
        while (value <= 2019) {
            double rValue = 1;
            while (rValue <= 5) {
                XVariable X = new XVariable();
                X.predicates.add(new Predicate("Year", "l", String.valueOf(value), "double", "1"));
                X.predicates.add(new Predicate("Rating", "l", String.valueOf(rValue), "double", "1"));
                Xlist.add(X);
                rValue++;
            }
            value++;
        }

        return Xlist;
    }

    public ArrayList<HashSet<DataNode>> enumerate3(Graph<DataNode, RelationshipEdge> pattern, Range range, String attr, String pnodeName) {

        DataNode curNode = null;
        for (DataNode node : pattern.vertexSet()) {
            if (node.isRoot) {
                curNode = node;
                System.out.println(node.preds.get(0).attr + node.preds.get(0).value);
                break;
            }
        }
        ArrayList<HashSet<DataNode>> sets = new ArrayList<>();
        double value = range.low;
        while (value < range.high) {

            curNode.preds.remove(0);


//            sets.add(execute(pattern, X));
            value = value + 1.0;

        }

        return sets;
    }

    public ArrayList<Graph<DataNode, RelationshipEdge>> enumerate2(Graph<DataNode,
            RelationshipEdge> pattern, Range range, String attr, String pnodeName) {

        DataNode curNode = null;
        ArrayList<Graph<DataNode, RelationshipEdge>> patterns = new ArrayList<>();
        double value = range.low;
        while (value < range.high) {

            Graph<DataNode, RelationshipEdge> curPattern =
                    new DefaultDirectedGraph<DataNode, RelationshipEdge>(RelationshipEdge.class);
            for (DataNode node : pattern.vertexSet()) {
                curPattern.addVertex(node);
            }

            for (RelationshipEdge edge : pattern.edgeSet()) {
                curPattern.addEdge(dataGraph.getEdgeSource(edge),
                        dataGraph.getEdgeTarget(edge), new RelationshipEdge(edge.getLabel()));
            }

            for (DataNode node : curPattern.vertexSet()) {
                if (node.getNodeName().equals(pnodeName)) {
                    System.out.println(node.getNodeName());
                    curNode = node;
                    break;
                }
            }
//            curNode.preds.get(0).value = String.valueOf(value);

            curNode.preds.remove(0);

            EdgeReversedGraph<DataNode, RelationshipEdge> revGraph = new EdgeReversedGraph<DataNode, RelationshipEdge>(curPattern);
            EdgeReversedGraph<DataNode, RelationshipEdge> graphCopy = new EdgeReversedGraph<DataNode, RelationshipEdge>(revGraph);

            patterns.add(graphCopy);
            value = value + 1.0;

        }

        for (Graph<DataNode, RelationshipEdge> p : patterns) {
            for (DataNode node : p.vertexSet()) {
                if (node.isRoot) {
                    System.out.println(node.preds.get(0).attr + node.preds.get(0).value);
                }
            }
        }
        System.out.println(patterns.size());

        return patterns;
    }

    public HashSet<DataNode> maxSumGen(ArrayList<String> group, ArrayList<Integer> cardinality, HashSet<DataNode> candidates, String groupAttr) {

        System.out.println("Candidates Size:" + candidates.size());
        HashSet<DataNode> solution = new HashSet<>();
        HashSet<DataNode> dup = new HashSet<>();
        int bigcupP = 0;
        for (int i = 0; i < group.size(); i++) {
            String p = group.get(i);
            for (DataNode node : candidates) {
                if (!node.attributes.containsKey(groupAttr)) {
                    continue;
                }
                if (node.attributes.get(groupAttr).contains(p)) {
                    bigcupP++;
                }
            }

        }
        for (int i = 0; i < group.size(); i++) {

            String p = group.get(i);
            int c = cardinality.get(i);
            HashSet<DataNode> curGroup = new HashSet<>();
            for (DataNode node : candidates) {
                if (!node.attributes.containsKey(groupAttr)) {
                    continue;
                }
                if (node.attributes.get(groupAttr).contains(p) &&
                        !dup.contains(node)) {
                    dup.add(node);
                    curGroup.add(node);
                }
            }

            System.out.println("Curr Group Size:" + curGroup.size());
            HashSet<DataNode> ri = new HashSet<>();

            for (int j = 0; j < c; j++) {
                DataNode temp = (DataNode) curGroup.toArray()[j];
                ri.add(temp);
            }

            for (DataNode node : ri) {
                curGroup.remove(node);
            }
            System.out.println("Group Size:" + curGroup.size());
            System.out.println("Select Size:" + ri.size());
            HashSet<DataNode> tempRi;
            tempRi = new HashSet<>(ri);
            for (DataNode n : ri) {

                double curDiv = getDiv(tempRi);
                HashSet<DataNode> tempCur = new HashSet<>(curGroup);
                for (DataNode m : tempCur) {

                    if (!tempCur.contains(m) || !tempRi.contains(n)) {
                        continue;
                    }

                    if (((1 + (1 / bigcupP)) * curDiv) < getDiv(curDiv,tempRi,m,n)) {
                        tempRi.remove(n);
                        tempRi.add(m);
                        curGroup.remove(m);
                    }

                }

            }


            System.out.println(tempRi.size() + "........................");
            solution.addAll(tempRi);

        }

//        double max = Double.MIN_VALUE;
//        double min = Double.MAX_VALUE;
//
//        for (DataNode node : solution) {
//
//            if (!node.attributes.containsKey("Year")) {
//                continue;
//            }
//
//            double variable = Double.parseDouble(((String) node.attributes.get("Year").toArray()[0]).substring(0, 4));
//            System.out.println(variable);
//            if (variable < min) {
//                min = variable;
//
//            }
//
//            if (variable > max) {
//                max = variable;
//            }
//
//        }
//
//        System.out.println(min + " to " + max);

        return solution;

    }
    public double getDiv(HashSet<DataNode> currentMatch) {
        double div = 0.0;
        for (DataNode n : currentMatch) {
            for (DataNode m : currentMatch) {
                if (m.equals(n)) {
                    continue;
                }
                int i = 0;
                double curDiv = 0.0;
                for (String key : n.attributes.keySet()) {
                    if (m.attributes.containsKey(key)) {
                        i++;
                        curDiv += (1 - JaccardDist((String) n.attributes.get(key).toArray()[0],
                                (String) m.attributes.get(key).toArray()[0]));
                    }

                }
                curDiv = curDiv / i;
                div += curDiv;
            }
        }
        System.out.println((2 * div)/(candidates.size()-1));
        return (2 * div)/(candidates.size()-1);
    }

    private double getDiv(double curDiv, HashSet<DataNode> tempRi, DataNode add, DataNode re) {

        for (DataNode node : tempRi) {
            if (node.equals(re)) {
                continue;
            }
            int i = 0;
            double curSim = 0.0;
            for (String key : re.attributes.keySet()) {
                if (key.equals("Category")) {
                    continue;
                }
                if (node.attributes.containsKey(key)) {
                    i++;
                    curSim +=  (1 - JaccardDist((String) node.attributes.get(key).toArray()[0],
                            (String) re.attributes.get(key).toArray()[0]));
                }

            }

            curDiv -= (curSim/i);
        }

        for (DataNode node : tempRi) {
            if (node.equals(re)) {
                continue;
            }
            int i = 0;
            double curSim = 0.0;
            for (String key : add.attributes.keySet()) {
                if (node.attributes.containsKey(key)) {
                    i++;
                    curSim +=  (1 - JaccardDist((String) node.attributes.get(key).toArray()[0],
                            (String) add.attributes.get(key).toArray()[0]));
                }

            }

            curDiv += (curSim/i);
        }
        return (2 * curDiv)/(candidates.size()-1);

    }


    private ArrayList<HashSet<DataNode>> greedySelection(ArrayList<HashSet<DataNode>> curSets) {

        int total = UnionOfSets(curSets).size();

        ArrayList<HashSet<DataNode>> res = new ArrayList<>();


        while (UnionOfSets(res).size() < total) {


            HashSet<DataNode> curBest = new HashSet<>();
            HashSet<DataNode> union = UnionOfSets(res);

            for (HashSet<DataNode> s : curSets) {
                if (s.size() == 0) {
                    continue;
                }
                double min = Double.MAX_VALUE;
                HashSet<DataNode> temp = new HashSet<>(union);
                int originalSize = temp.size();
                temp.addAll(s);
                if (temp.size() == originalSize) {
                    continue;
                }
                if (s.size() / (temp.size() - originalSize) < min) {
                    min = s.size() / (temp.size() - originalSize);
                    curBest = s;
                }

                temp.removeAll(s);

            }
            res.add(curBest);
            curSets.remove(curBest);
//            System.out.println("add");
        }

        System.out.println(res.size() + "greedy select Size");
        return res;
    }

//    private ArrayList<HashSet<DataNode>> greedySelection(ArrayList<HashSet<DataNode>> curSets) {
//
//             int total = UnionOfSets(curSets).size();
//
//             ArrayList<HashSet<DataNode>> res = new ArrayList<>();
//
//             while (UnionOfSets(res).size() < total) {
//
//                 double min = Double.MAX_VALUE;
//                 HashSet<DataNode> curBest = new HashSet<>();
//
//                 ArrayList<HashSet<DataNode>> curRes = new ArrayList<>(res);
//                 HashSet<DataNode> union = UnionOfSets(curRes);
//
//                 for (HashSet<DataNode> s : curSets) {
//
//                     HashSet<DataNode> temp = new HashSet<>(union);
//                     int originalSize = temp.size();
//                     temp.addAll(s);
//                     if (total/(temp.size() - originalSize) < min) {
//                         min = total/(temp.size() - originalSize);
//                         curBest = s;
//                     }
//                     temp.removeAll(s);
//
//                 }
//
//                 res.add(curBest);
//
//             }
//
//
//        return res;
//    }

    private HashSet<DataNode> getIntersection(HashSet<DataNode> s, HashSet<DataNode> rh) {

        HashSet<DataNode> intersect = new HashSet<>();
        for (DataNode node : s) {
            if (rh.contains(node)) {
                intersect.add(node);
            }
        }
        return intersect;
    }

    private HashSet<DataNode> UnionOfSets(ArrayList<HashSet<DataNode>> sets) {
        HashSet<DataNode> union = new HashSet<>();
        for (HashSet<DataNode> set : sets) {
            if (set == null) {
                continue;
            }
            union.addAll(set);
        }
        return union;
    }

    private int NoOfIntersection(HashSet<DataNode> set, HashSet<DataNode> red) {

        int count = 0;

        for (DataNode node : set) {
            if (red.contains(node)) {
                count++;
            }
        }

        return count;
    }


    public static void main(String args[]) throws IOException {
        MovieDataGraph graph = new MovieDataGraph("C:\\Users\\Nick\\Downloads\\fairness\\newType.ttl",
                "C:\\Users\\Nick\\Downloads\\Film_dataset\\Film_dataset\\processed_dataset\\mix.dbpedia.graph");
        graph.enhencedIMDB("C:\\Users\\Nick\\Downloads\\Film_dataset\\Film_dataset\\processed_dataset\\film.imdb.json");
        //Path for graph file and type file.

        QGen qGen = new QGen(graph);
        Graph<DataNode, RelationshipEdge> pattern = qGen.generatePattern();


        String uoType = "Film";
        String groupAttr = "Category";
        ArrayList<String> group = new ArrayList<>();
        group.add("Comedy");
        group.add("Romance");
        ArrayList<Integer> cc = new ArrayList<>();
        cc.add(50);
        cc.add(50);
        //Assign groups and constraints.
       ArrayList<RangePair> rangePairs = new ArrayList<>();
       rangePairs.add(new RangePair("Year","l"));
       rangePairs.add(new RangePair("Rating","l"));
       ArrayList<Range> ranges = new ArrayList<>();
       ranges.add(new Range(1950,1970));
       ranges.add(new Range(4,5));

       qGen.getInducedGraph(pattern,uoType);
       qGen.sumP = qGen.getGroup(uoType, group);

        long startTime = System.nanoTime();
        qGen.runMaxSumGen(groupAttr,group,cc,pattern,rangePairs,ranges);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration + "total");

    }


}