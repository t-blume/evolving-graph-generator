package main.java;

import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class RandomGenerator {

    private RandomEuclideanGenerator gen;
    double threshold = 0.05;
    private Graph graph;
    private String[] types;
    private String[] properties;
    private String[] sources;

    private int[] minMaxTypes;


    private HashMap<String, String> usedSources = new HashMap<>();


    public RandomGenerator(int numberOfTypes, int numberOfProperties, int numberOfSources,
                           int minTypes, int maxTypes) {
        HashSet<String> types = new HashSet<>();
        while (types.size() < numberOfTypes)
            types.add(randomType(numberOfTypes, RandomString.lower));
        this.types = new String[numberOfTypes];
        types.toArray(this.types);

        HashSet<String> properties = new HashSet<>();
        while (properties.size() < numberOfProperties)
            properties.add(randomProperty(numberOfTypes, RandomString.lower));
        this.properties = new String[numberOfProperties];
        properties.toArray(this.properties);

        HashSet<String> sources = new HashSet<>();
        while (sources.size() < numberOfSources)
            sources.add(randomSource(numberOfSources, RandomString.lower));
        this.sources = new String[numberOfSources];
        sources.toArray(this.sources);

        minMaxTypes = new int[2];
        minMaxTypes[0] = minTypes;
        minMaxTypes[1] = maxTypes;
        gen = new RandomEuclideanGenerator(2, true, false, "types", "property");

        graph = new SingleGraph("random euclidean");
        gen.addSink(graph);

    }

    private Set<String> numericToTypes(float number) {
        Set<String> types = new HashSet<>();

        Random random = new Random();
        double stdNormal = random.nextGaussian();
        double normalValue = 1.0 * stdNormal + 1.0;


        int numberOfTypes = Math.round((int) Math.max(minMaxTypes[0], Math.min(minMaxTypes[1], normalValue)));
        for (int i = 0; i < numberOfTypes; i++) {
            double rnd = Math.abs(random.nextGaussian());
            rnd = Math.max(0, Math.min(1, rnd));
            types.add(this.types[(int) Math.round(rnd * (this.types.length - 1))]);
        }
        return types;
    }

    private String numericToProperty(float number) {
        // 0 <= number <= 1
        // 0 -> 0
        // 1 -> types.length-1
        return properties[Math.round(number * (properties.length - 1))];
    }


    private String numericToSource(float number) {
        // 0 <= number <= 1
        // 0 -> 0
        // 1 -> types.length-1
        return sources[Math.round(number * (sources.length - 1))];
    }


    private String randomProperty(int numberOfProperties, String alphabet) {
        int length = (int) Math.max(1, (long) Math.ceil(Math.log10(numberOfProperties) / Math.log10(alphabet.length())));
        RandomString randomLabel = new RandomString(length + 1, new SecureRandom(), alphabet);
        return randomLabel.nextString();
    }

    private String randomType(int numberOfTypes, String alphabet) {
        int length = (int) Math.max(1, (long) Math.ceil(Math.log10(numberOfTypes) / Math.log10(alphabet.length())));
        RandomString randomLabel = new RandomString(length + 1, new SecureRandom(), alphabet);
        return randomLabel.nextString();
    }

    private String randomSource(int numberOfSources, String alphabet) {
        int length = (int) Math.max(1, (long) Math.ceil(Math.log10(numberOfSources) / Math.log10(alphabet.length())));
        RandomString randomLabel = new RandomString(length + 1, new SecureRandom(), alphabet);
        return randomLabel.nextString();
    }

    public void addNodes(int numberOfNodes) {

        //gen.setThreshold(0.01); //only for scale test (avoid too high connectivity for large graphs, usually 0.05
        int nodesInGraph = graph.getNodeCount();
        if(nodesInGraph > 0) {
            if(graph.getEdgeCount() > 10 * nodesInGraph) {
                threshold = threshold - (((double) numberOfNodes / (double) nodesInGraph) * threshold);
                System.out.println("Adjusting threshold: " + threshold);
                gen.setThreshold(threshold);
            }
            else if(graph.getEdgeCount() < 3 * nodesInGraph) {
                threshold = threshold + (((double) numberOfNodes / (double) nodesInGraph) * threshold);
                System.out.println("Adjusting threshold: " + threshold);
                gen.setThreshold(threshold);
            }
        }



        gen.begin();
        for (int i = 0; i < numberOfNodes-1; i++) {
            gen.nextEvents();
        }
        gen.end();

    }

    public void transform() {
        graph.getEachNode().forEach(X -> {
            if (X.getAttribute("types") instanceof Double) {
                X.setAttribute("types", numericToTypes(Float.valueOf(X.getAttribute("types").toString())));
            }
            if (!X.getAttributeKeySet().contains("source")) {
                String source = numericToSource((float) Math.random());
                X.setAttribute("source", source);
                usedSources.put(X.getId(), source);
            }
        });

        graph.getEachEdge().forEach(X -> {
            if (X.getAttribute("property") instanceof Double) {
                X.setAttribute("property", numericToProperty(Float.valueOf(X.getAttribute("property").toString())));
            }
            if (!X.getAttributeKeySet().contains("source")) {
                double dist = Math.random();
                String source = usedSources.get(X.getSourceNode().getId());

                //small chance that a property is from another source (Note: random source could be again same source)
                if (dist < 0.05) {
                    source = numericToSource((float) dist);
                }
                X.setAttribute("source", source);
            }
        });
    }


    public void evolve(double addRatio, double deleteRatio) {
        Random randomNumber = new Random();
        int count = graph.getNodeCount();
        int deleteInstances = (int) Math.round(count * deleteRatio);
        System.out.println("Deleting " + deleteInstances + "/" + count);
        for (int i = 0; i < deleteInstances; i++) {
            int index = randomNumber.nextInt(usedSources.size());
            String id = (String) usedSources.keySet().toArray()[index];
            usedSources.remove(id);
            graph.removeNode(id);
        }
        int addInstances = (int) Math.round(count * addRatio);
        System.out.println("Adding " + addInstances + "/" + count);

        if(addInstances > 0) {
            addNodes(addInstances);
        }
    }


    public void export(String filename) throws IOException {
        System.out.println("Exporting " + graph.getNodeCount() + " nodes and " + graph.getEdgeCount() + " edges.");
        File file = new File(filename);
        if (!file.exists())
            file.getParentFile().mkdirs();
        GZIPOutputStream gis = new GZIPOutputStream(new FileOutputStream(file));

        BufferedWriter writer =     new BufferedWriter(
                new OutputStreamWriter(gis, "UTF-8"));
        Set<String> exportedNodes = new HashSet<>();
        graph.getEachEdge().forEach(X -> {
            if (!exportedNodes.contains(X.getSourceNode().getId())) {
                Node node = graph.getNode(X.getSourceNode().getId());
                Set<String> types = node.getAttribute("types");
                String source = node.getAttribute("source");
                types.forEach(T -> {
                    try {
                        writer.write("<" + node.getId() + "> <type> <" + T + "> <" + source + "> .\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            try {
                writer.write("<" + X.getSourceNode().getId() + "> <" + X.getAttribute("property") + "> <"
                        + X.getTargetNode().getId() + "> <" + X.getAttribute("source") + "> .\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
        gis.close();
    }

    public Graph getGraph() {
        return graph;
    }

    public static void main(String[] args) {
        int numberOfNodes = 10000;
        int numberOfTypes = 7;
        int numberOfProperties = 7;
        int numberOfSources = 50;

        int iterations = 50;
        double decay = 1.05;

        int minTypes = 0;
        int maxTypes = 4;


        RandomGenerator generator = new RandomGenerator(numberOfTypes, numberOfProperties, numberOfSources, minTypes, maxTypes);
        generator.addNodes(numberOfNodes);
        generator.transform();

        double addRatio = 0.2;
        double delRatio = 0.1;

        String baseFolder = "out/";
        baseFolder = "test-graphs/";
        String stringDecay = String.valueOf(decay);
        String folder = baseFolder + "euclidean-graph_" + numberOfNodes + "n-" + numberOfTypes + "t-" + numberOfProperties + "p-" + numberOfSources + "s-"
                + String.valueOf(addRatio).replace("0.", "")+ "a-" + String.valueOf(delRatio).replace("0.", "")
                + "d-" + (stringDecay.startsWith("0.") ? stringDecay.replace("0.", "") : stringDecay.replace("1.", "1")) + "dc";
        try {
            generator.export(folder + "/iteration-0.nq.gz");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 1; i < iterations; i++) {
            System.out.println("Starting iteration " + i + "...");
            generator.evolve(addRatio, delRatio);
            generator.transform();
            try {
                System.out.println(".. finished iteration " + i);
                generator.export(folder + "/iteration-" + i + ".nq.gz");
            } catch (IOException e) {
                e.printStackTrace();
            }
            addRatio *= decay;
            delRatio *= decay;
        }
    }
}
