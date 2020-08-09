package bearmaps.test;

import bearmaps.AugmentedStreetMapGraph;
import org.junit.Before;
import org.junit.Test;
import bearmaps.Router;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bearmaps.utils.Constants.BASE_DIR_PATH;
import static org.junit.Assert.assertEquals;

/** Test of the routing part of the assignment. */
public class TestRouter {
    private static final String PARAMS_FILE = BASE_DIR_PATH + "data/proj3_test_inputs/path_params.txt";
    private static final String RESULTS_FILE = BASE_DIR_PATH + "data/proj3_test_inputs/path_results.txt";
    private static final int NUM_TESTS = 8;
    private static final String OSM_DB_PATH = BASE_DIR_PATH + "data/proj3_xml/berkeley-2020.osm.xml";
    private static AugmentedStreetMapGraph graph;
    private static boolean initialized = false;

    @Before
    public void setUp() throws Exception {
        if (initialized) {
            return;
        }
        graph = new AugmentedStreetMapGraph(OSM_DB_PATH);
        initialized = true;
        String prefix = "76";
        System.out.println("Getting prefixes for " + prefix);
        System.out.println(graph.getLocationsByPrefix(prefix));
    }

    @Test
    public void testShortestPath() throws Exception {
        List<Map<String, Double>> testParams = paramsFromFile();
        List<List<Long>> expectedResults = resultsFromFile();

        for (int i = 0; i < NUM_TESTS; i++) {
            System.out.println(String.format("Running test: %d", i));
            Map<String, Double> params = testParams.get(i);
            List<Long> actual = Router.shortestPath(graph,
                    params.get("start_lon"), params.get("start_lat"),
                    params.get("end_lon"), params.get("end_lat"));
            List<Long> expected = expectedResults.get(i);
            assertEquals("Your results did not match the expected results", expected, actual);
        }
    }

    private List<Map<String, Double>> paramsFromFile() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(PARAMS_FILE), Charset.defaultCharset());
        List<Map<String, Double>> testParams = new ArrayList<>();
        int lineIdx = 2; // ignore comment lines
        for (int i = 0; i < NUM_TESTS; i++) {
            Map<String, Double> params = new HashMap<>();
            params.put("start_lon", Double.parseDouble(lines.get(lineIdx)));
            params.put("start_lat", Double.parseDouble(lines.get(lineIdx + 1)));
            params.put("end_lon", Double.parseDouble(lines.get(lineIdx + 2)));
            params.put("end_lat", Double.parseDouble(lines.get(lineIdx + 3)));
            testParams.add(params);
            lineIdx += 4;
        }
        return testParams;
    }

    private List<List<Long>> resultsFromFile() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(RESULTS_FILE), Charset.defaultCharset());
        List<List<Long>> expected = new ArrayList<>();
        int lineIdx = 2; // ignore comment lines
        for (int i = 0; i < NUM_TESTS; i++) {
            int numVertices = Integer.parseInt(lines.get(lineIdx));
            lineIdx++;
            List<Long> path = new ArrayList<>();
            for (int j = 0; j < numVertices; j++) {
                path.add(Long.parseLong(lines.get(lineIdx)));
                lineIdx++;
            }
            expected.add(path);
        }
        return expected;
    }
}
