package bearmaps.utils.graph;
import bearmaps.utils.pq.MinHeapPQ;
import jdk.jshell.spi.ExecutionControl;

import java.util.*;

public class AStarSolver<Vertex> implements ShortestPathsSolver<Vertex> {

    AStarGraph<Vertex> graph;
    Vertex source;
    Vertex goal;
    double startTime;
    double endTime;
    double currentTime;
    MinHeapPQ<Vertex> PQ = new MinHeapPQ<>();
    HashMap<Vertex, Double> distTo = new HashMap<>();
    HashMap<Vertex, Vertex> prev = new HashMap<>();
    LinkedList<Vertex> solution = new LinkedList<>();
    SolverOutcome outcome;
    int numStatesExplored = 1;

    public AStarSolver(AStarGraph<Vertex> input, Vertex start, Vertex end, double timeout) {
        source = start; goal = end; graph = input;
        startTime = System.currentTimeMillis();
        endTime = startTime + timeout * 100.;
        PQ.insert(start, h(start, end));
        distTo.put(start, 0.);

        Vertex p;
        while (!PQ.peek().equals(end)) {
            if (timeOut()) {
                outcome = SolverOutcome.TIMEOUT;
                return;
            }
            else {
                p = PQ.poll();
                numStatesExplored += 1;
                for (var e: graph.neighbors(p)) {
                    relax(e);
                }
            }
            if (PQ.size() <= 0) {
                outcome = SolverOutcome.UNSOLVABLE;
                return;
            }
        }
        buildSolution();
    }

    private void buildSolution() {
        Vertex curr = goal;
        while (curr != null) {
            solution.addFirst(curr);
            curr = prev.get(curr);
        }
        outcome = SolverOutcome.SOLVED;
    }

    private boolean timeOut() {
        currentTime = System.currentTimeMillis() - startTime;
        return System.currentTimeMillis() > endTime;
    }

    private double h(Vertex v, Vertex goal) {
        return graph.estimatedDistanceToGoal(v, goal);
    }

    private void relax(WeightedEdge<Vertex> e) {
        Vertex p = e.from();
        Vertex q = e.to();
        double w = e.weight();
        if (distTo.get(q) == null || distTo.get(p) + w < distTo.get(q)) {
            distTo.put(q, distTo.get(p) + w);
            prev.put(q, p);
            if (PQ.contains(q)) {
                PQ.changePriority(q, distTo.get(q) + h(q, goal));
            }
            else {
                PQ.insert(q, distTo.get(q) + h(q, goal));
            }
        }
    }

    public SolverOutcome outcome() {
        return outcome;
    }
    public List<Vertex> solution() {
        return solution;
    }
    public double solutionWeight() {
        Double w = distTo.get(goal);
        return (w == null) ? 0 : w;
    }
    public int numStatesExplored() {
        return numStatesExplored;
    }
    public double explorationTime() {
        return currentTime;
    }
}

