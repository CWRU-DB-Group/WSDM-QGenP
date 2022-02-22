package MovieLensGraph;

import Infra.DataGraphBase;
import Infra.DataNode;
import Infra.RelationshipEdge;
import org.apache.jena.rdf.model.*;

import org.json.*;

import java.io.*;
import java.util.*;

public class MovieDataGraph extends DataGraphBase {


    public MovieDataGraph(String nodeTypesFilePath, String dataGraphFilePath) throws IOException {

        super();
        loadNodeMap(nodeTypesFilePath);
        addAllVertex()  ;
        loadGraph(dataGraphFilePath);
        enhencedIMDB("C:\\Users\\Nick\\Downloads\\Film_dataset\\Film_dataset\\processed_dataset\\film.imdb.json");
    }


    public static <String, Integer> void printMap(Map<String, Integer> map) {
        int t = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("Key : " + entry.getKey()
                    + " Value : " + entry.getValue());
            t += (int) entry.getValue();
        }
        System.out.println(t);

    }


    private void loadNodeMap(String nodeTypesFilePath) throws IOException {

        if (nodeTypesFilePath == null || nodeTypesFilePath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        System.out.println("Start Loading DBPedia Node Map...");

        Model model = ModelFactory.createDefaultModel();
        System.out.println("Loading Node Types...");
        model.read(nodeTypesFilePath);
        StmtIterator typeTriples = model.listStatements();
        while (typeTriples.hasNext()) {

            Statement stmt = typeTriples.nextStatement();
            String subject = stmt.getSubject().getURI();

            if (subject.length() > 28) {
                subject = subject.substring(28);
            }

            DataNode dataNode;

            String object = stmt.getObject().asResource().getLocalName();

            int nodeId = subject.hashCode();

            if (!nodeMap.containsKey(nodeId)) {
                dataNode = new DataNode(subject);
                dataNode.types.add(object);
                nodeMap.put(nodeId, dataNode);
            } else {
                nodeMap.get(nodeId).types.add(object);
            }
        }

        System.out.println("Done Loading DBPedia Node Map!!!");
        System.out.println("DBPedia NodesMap Size: " + nodeMap.size());
    }

    private void loadGraph(String dataGraphFilePath) throws IOException {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }
        System.out.println("Loading DBPedia Graph...");

        File file = new File(dataGraphFilePath);
        File file1 = new File(dataGraphFilePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedReader br1 = new BufferedReader(new FileReader(file1));

        String st;
        String st1;
        while ((st = br.readLine()) != null) {
            String[] triples = st.split("\t");
            String subject = triples[0];
            String predicate = triples[1];
            String object = triples[2];

            String subjectString = subject.substring(1, triples[0].length() - 1);
            String objectString = object.substring(1, triples[2].length() - 1);
            if (predicate.equals("<abstract>") || predicate.equals("<substract>")
                    || predicate.equals("<alias>") || predicate.equals("<digitalChannel>")
                    || predicate.equals("<militaryCommand>")) {
                continue;
            }
            if (subjectString.contains("Category:") || objectString.contains("Category:")) {
                continue;
            }

            if (predicate.equals("<country>") || predicate.equals("<genre>") || predicate.equals("<birthPlace>")) {
                if (!nodeMap.containsKey(subjectString.hashCode())) {
                    continue;
                }


                DataNode curr = nodeMap.get(subjectString.hashCode());

                if (!curr.attributes.containsKey(predicate.substring(1, predicate.length() - 1))) {

                    HashSet<String> newSet = new HashSet<>();
                    newSet.add(objectString);
                    curr.attributes.put(predicate.substring(1, predicate.length() - 1), newSet);

                } else {
                    curr.attributes.get(predicate.substring(1, predicate.length() - 1)).add(objectString);
                }
            }


            if (object.charAt(0) != '<') {
                if (object.indexOf('\"') >= object.lastIndexOf('\"')) {
                    continue;
                }
                String attribute = object.substring(object.indexOf('\"') + 1, object.lastIndexOf('\"'));
                if (!nodeMap.containsKey(subjectString.hashCode())) {
                    continue;
                }
                DataNode curr = nodeMap.get(subjectString.hashCode());

                if (!curr.attributes.containsKey(predicate.substring(1, predicate.length() - 1))) {

                    HashSet<String> newSet = new HashSet<>();
                    newSet.add(attribute);
                    curr.attributes.put(predicate.substring(1, predicate.length() - 1), newSet);

                } else {
                    curr.attributes.get(predicate.substring(1, predicate.length() - 1)).add(attribute);
                }
            }

        }
        br.close();

        int count = 0;
        int count4 = 0;


        while ((st1 = br1.readLine()) != null) {

            String[] triples = st1.split("\t");
            String subject = triples[0];
            String predicate = triples[1];
            String object = triples[2];
            String subjectString = triples[0].substring(1, triples[0].length() - 1);
            String predicateString = triples[1].substring(1, triples[1].length() - 1);
            String objectString = triples[2].substring(1, triples[2].length() - 1);

            if (predicate.equals("<abstract>")) {
                continue;
            }
            if (object.charAt(0) == '<') {

                if (!nodeMap.containsKey(subjectString.hashCode())) {

                    count++;

                    continue;
                }

                if (!nodeMap.containsKey(objectString.hashCode())) {
                    count4++;

                    continue;

                }
                DataNode snode = nodeMap.get(subjectString.hashCode());
                DataNode tnode = nodeMap.get(objectString.hashCode());
                dataGraph.addVertex(snode);
                dataGraph.addVertex(tnode);
                dataGraph.addEdge(snode, tnode, new RelationshipEdge(predicateString));

            }

        }
        br1.close();
        System.out.println("MissingS!" + count);
        System.out.println("MissingT!" + count4);
        System.out.println("Number of Edges: " + dataGraph.edgeSet().size());
        System.out.println("Number of Nodes: " + dataGraph.vertexSet().size());
        System.out.println("Done Loading DBPedia Graph!!!");

    }


    private void cleanNodeMap(String nodeTypesFilePath, String filePath) throws IOException {

        if (nodeTypesFilePath == null || nodeTypesFilePath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        System.out.println("Start Loading DBPedia Node Map...");


        File file = new File(filePath);

        BufferedReader br = new BufferedReader(new FileReader(file));
        HashSet<String> set = new HashSet<>();

        String st;

        while ((st = br.readLine()) != null) {

            String[] triples = st.split("\t");

            String sub = triples[0];
            String subject = sub.substring(1, sub.length() - 1);
            set.add(subject);

            String obj = triples[2];
            if (obj.charAt(0) == '<') {
                String object = obj.substring(1, obj.length() - 1);
                set.add(object);

            }
        }
        System.out.println(set.size() + "total size!");

        br.close();


        Model model = ModelFactory.createDefaultModel();
        System.out.println("Loading Node Types...");
        model.read(nodeTypesFilePath);
        StmtIterator typeTriples = model.listStatements();

        File newType = new File("newType.ttl");
        BufferedWriter writer = new BufferedWriter(new FileWriter(newType));


        while (typeTriples.hasNext()) {

            Statement stmt = typeTriples.nextStatement();
            String sub = stmt.getSubject().getURI();

            if (sub.length() > 28) {
                sub = sub.substring(28);
            }
            if (!set.contains(sub)) {
                continue;
            }

            String url = stmt.getObject().asResource().getNameSpace();

            if (url.equals("http://dbpedia.org/ontology/")) {
                writer.write("<" + stmt.getSubject().toString() + ">" + " " + "<" + stmt.getPredicate().toString() + ">" + " " + "<" + stmt.getObject() + ">" + " .");
                writer.write("\n");
            }

        }
        writer.close();
        System.out.println("Done");

    }

    public void enhencedIMDB(String jsonFile) throws IOException {

        File file = new File(jsonFile);
        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        int i = 0;
        int j = 0;
        while ((st = br.readLine()) != null) {

            if (st.trim().length() == 0) {
                continue;
            }


            JSONObject obj = new JSONObject(st);
            if (!obj.has("Title")) {
                continue;
            }
            i++;
            String title = obj.getString("Title");
            String newTitle = title.trim().replaceAll(" ", "_");
//            System.out.println(newTitle);

            if (nodeMap.containsKey(newTitle.hashCode())) {
                j++;

                if (!obj.get("imdbRating").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    set.add(obj.getString("imdbRating"));
                    nodeMap.get(newTitle.hashCode()).attributes.put("Rating", set);
                }

                if (!obj.get("Rated").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    set.add(obj.getString("Rated"));
                    nodeMap.get(newTitle.hashCode()).attributes.put("Rated", set);
                }
                if (!obj.get("Genre").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    String[] genres = obj.getString("Genre").split(",");
                    for (String g : genres) {
                        set.add(g.trim());
                    }
                    nodeMap.get(newTitle.hashCode()).attributes.put("Category", set);
                }
                if (!obj.get("Language").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    String[] lang = obj.getString("Language").split(",");
                    for (String l : lang) {
                        set.add(l.trim());
                    }
                    nodeMap.get(newTitle.hashCode()).attributes.put("Language", set);
                }
                if (!obj.get("Country").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    set.add(obj.getString("Country"));
                    nodeMap.get(newTitle.hashCode()).attributes.put("Nation", set);
                }
                if (!obj.get("Year").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    set.add(obj.getString("Year"));
                    nodeMap.get(newTitle.hashCode()).attributes.put("Year", set);
                }
                if (!obj.get("Awards").equals("N/A")) {
                    HashSet<String> set = new HashSet<>();
                    set.add(obj.getString("Awards"));
                    nodeMap.get(newTitle.hashCode()).attributes.put("Awards", set);
                }



            }


        }

    }


    private double Dist(DataNode n, DataNode m) {

//        if(m == null || n == null) {
//            return 0;
//        }

        int i = 0;
        double result = 0;
        for (String key : n.attributes.keySet()) {
            if (m.attributes.containsKey(key)) {
                i++;
                result += JaccardDist((String) n.attributes.get(key).toArray()[0],
                        (String) m.attributes.get(key).toArray()[0]);
            }


        }

        return result / i;
    }

    private double JaccardDist(String str1, String str2) {
        Set<Character> s1 = new HashSet<>();//set elements cannot be repeated
        Set<Character> s2 = new HashSet<>();

        for (int i = 0; i < str1.length(); i++) {
            s1.add(str1.charAt(i));//Put the elements in string into the set collection by index one by one
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
//        System.out.println(jaccard);
        return jaccard;
    }
    public static void main(String args[]) throws IOException {

        MovieDataGraph dataGraph = new MovieDataGraph("newType.ttl",
                "C:\\Users\\Nick\\Downloads\\Film_dataset\\Film_dataset\\processed_dataset\\mix.dbpedia.graph");

        dataGraph.cleanNodeMap("newType.ttl","C:\\Users\\Nick\\Downloads\\Film_dataset\\Film_dataset\\processed_dataset\\mix.dbpedia.graph");

    }

}


