import java.util.*;

public class App {

    static int[] bfsList(int start, List<List<Integer>> adjList) {
        int n = adjList.size();
        int[] dist = new int[n];
        Arrays.fill(dist, -1);

        Queue<Integer> q = new LinkedList<>();
        q.add(start);
        dist[start] = 0;

        while (!q.isEmpty()) {
            int v = q.poll();
            for (int u : adjList.get(v)) {
                if (dist[u] == -1) {
                    dist[u] = dist[v] + 1;
                    q.add(u);
                }
            }
        }

        int far = start;
        for (int i = 0; i < n; i++) {
            if (dist[i] > dist[far]) {
                far = i;
            }
        }

        return new int[]{far, dist[far]};
    }

    // Диаметр дерева
    static int diameterList(List<List<Integer>> adjList) {
        int[] first = bfsList(0, adjList);
        int[] second = bfsList(first[0], adjList);
        return second[1];
    }

    // Пример использования
    public static void main(String[] args) {
        int n = 5;
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) adjList.add(new ArrayList<>());

        // Пример дерева:
        // 0-1, 1-2, 1-3, 3-4
        adjList.get(0).add(1);
        adjList.get(1).add(0);
        adjList.get(1).add(2);
        adjList.get(2).add(1);
        adjList.get(1).add(3);
        adjList.get(3).add(1);
        adjList.get(3).add(4);
        adjList.get(4).add(3);

        System.out.println("Диаметр дерева = " + diameterList(adjList));
    }
}
