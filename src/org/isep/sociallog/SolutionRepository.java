package org.isep.sociallog;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by slefebvr on 07/03/17.
 */
public class SolutionRepository {

    public static Predicate<Solution> createTaskAndUserPred(String task, String user) {
        return solution -> solution.obstacle.taskId.equals(task) && solution.obstacle.user.equals(user);
    }
    public static Predicate<Solution> createObstaclePred(String task, Category category) {
        return solution -> solution.obstacle.taskId.equals(task) && solution.obstacle.category.equals(category);
    }


    final List<Solution> solution = new ArrayList<>();


    /**
     * adds a solution to this repo
     * @param s
     */
    public void insertSolution(Solution s) {
        solution.add(s);
    }


    private Solution solve(Predicate<Solution> p) {
        List<Solution> results = filterSolutions(p);
        if(results.size() > 0)
            return results.get(0);
        else
            return null;

    }

    public List<Solution> getSolutions() {
        return this.solution;

    }

    private List<Solution> filterSolutions(Predicate<Solution> p) {
        return solution.stream()
                .filter(p)
                .collect(Collectors.toList());
    }

    /**
     * Returns a solution matching the obstacle task and user,
     * returns null if not solution is found.
     * @param obs
     * @return
     */
    public Solution solveByUser(Obstacle obs) {
        return solve(createTaskAndUserPred(obs.taskId,obs.user));
    }

    /**
     * Solves by task and obstacle
     * @return
     */
    public Solution solveByObstacle(Obstacle obs) {
        return solve(createObstaclePred(obs.taskId,obs.category));

    }

    public List<Solution> getSolutionsByUserTask(String task, String user) {
        return filterSolutions(createTaskAndUserPred(task,user));
    }



}
