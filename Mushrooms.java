import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Mushrooms {
    private static final int inf = (int) 1e9;
    private static final char[] dirs = { 'L', 'U', 'R', 'D' };
    private static final int[] dr = { 0, -1, 0, 1 };
    private static final int[] dc = { -1, 0, 1, 0 };
    private static final char Empty = '.';
    private static final char Tree = '#';
    private static final char Mushroom = '@';
    private static final char Pers = '*';
    private static final char Hut = '^';
    private static final int killValue = 1;
    private int N;
    private int H;
    private int P;
    private float RD;
    private float CD;
    private char[][] grid;
    private float[][] values;
    private Point[] people;
    private int[][][][] distance;
    private ArrayList<Point>[] solution;
    private float score;
    private ArrayList<Point>[] bestSolution;
    private float bestScore;
    private Point[] huts;
    static Watch watch = new Watch();
    static PCG_XSH_RR rng = new PCG_XSH_RR(114514L ^ System.nanoTime());
    private SAState sa = new SAState();
    private int maxTurn;
    private float[] scores;
    private float[] powCD;
    private float[] powRD;

    public static void main(String[] args) {
        new Mushrooms().run();
    }

    private void run() {
        read();
        solve();
        write();
    }

    private void solve() {
        solution = new ArrayList[P];
        for (int p = 0; p < P; p++) {
            solution[p] = new ArrayList<>();
        }
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] == Mushroom) {
                    int p = rng.nextInt(P);
                    solution[p].add(new Point(r, c));
                    {
                        int h = rng.nextInt(H);
                        solution[p].add(huts[h]);
                    }
                }
            }
        }
        scores = new float[P];
        score = calculateScore();
        bestSolution = new ArrayList[P];
        for (int i = 0; i < P; i++) {
            bestSolution[i] = new ArrayList<>();
        }
        bestScore = 0;
        saveBest();
        multiSA();
        restoreBest();
    }

    private void saveBest() {
        if (score > bestScore) {
            bestScore = score;
            for (int p = 0; p < P; p++) {
                bestSolution[p].clear();
                for (int i = 0; i < solution[p].size(); i++) {
                    bestSolution[p].add(solution[p].get(i));
                }
            }
        }
    }

    private void restoreBest() {
        score = bestScore;
        for (int p = 0; p < P; p++) {
            solution[p].clear();
            for (int i = 0; i < bestSolution[p].size(); i++) {
                solution[p].add(bestSolution[p].get(i));
            }
        }
    }

    private float calculateScore() {
        float score = 0;
        for (int p = 0; p < P; p++) {
            scores[p] += calculateScore(p);
            score += scores[p];
        }
        return score;
    }

    private float calculateScore(int p) {
        float score = 0;
        int r = people[p].r;
        int c = people[p].c;
        float carry = 0;
        int turns = 0;
        for (int i = 0; i < solution[p].size(); i++) {
            Point point = solution[p].get(i);
            if (grid[point.r][point.c] == Mushroom) {
                int dist = distance[r][c][point.r][point.c];
                turns += dist;
                carry *= powCD[dist];
                float v = powRD[turns] * values[point.r][point.c];
                if (v >= killValue) {
                    carry += v;
                }
                carry *= CD;
                turns += 1;
            } else {
                int dist = distance[r][c][point.r][point.c];
                if (dist > 0) {
                    turns += dist;
                    carry *= powCD[dist - 1];
                    score += carry;
                    carry = 0;
                    if (turns > maxTurn) {
                        break;
                    }
                }
            }
            r = point.r;
            c = point.c;
        }
        return score;
    }

    private float calculateScore(int p1, int i1, int p2, int i2) {
        float score = 0;
        int r = people[p1].r;
        int c = people[p1].c;
        float carry = 0;
        int turns = 0;
        for (int i = 0; i < i1; i++) {
            Point point = solution[p1].get(i);
            if (grid[point.r][point.c] == Mushroom) {
                int dist = distance[r][c][point.r][point.c];
                turns += dist;
                carry *= powCD[dist];
                float v = powRD[turns] * values[point.r][point.c];
                if (v >= killValue) {
                    carry += v;
                }
                carry *= CD;
                turns += 1;
            } else {
                int dist = distance[r][c][point.r][point.c];
                if (dist > 0) {
                    turns += dist;
                    carry *= powCD[dist - 1];
                    score += carry;
                    carry = 0;
                    if (turns > maxTurn) {
                        break;
                    }
                }
            }
            r = point.r;
            c = point.c;
        }
        for (int i = i2; i < solution[p2].size(); i++) {
            Point point = solution[p2].get(i);
            if (grid[point.r][point.c] == Mushroom) {
                int dist = distance[r][c][point.r][point.c];
                turns += dist;
                carry *= powCD[dist];
                float v = powRD[turns] * values[point.r][point.c];
                if (v >= killValue) {
                    carry += v;
                }
                carry *= CD;
                turns += 1;
            } else {
                int dist = distance[r][c][point.r][point.c];
                if (dist > 0) {
                    turns += dist;
                    carry *= powCD[dist - 1];
                    score += carry;
                    carry = 0;
                    if (turns > maxTurn) {
                        break;
                    }
                }
            }
            r = point.r;
            c = point.c;
        }
        return score;
    }

    private void multiSA() {
        int numRestart = N < 20 ? 3 : 1;
        double startTime = watch.getSecond();
        double endTime = 9.5;
        double remainTime = endTime - startTime;
        double startStartTemperature = 50;
        double endStartTemperature = 1e-9;
        for (double restart = 0; restart < numRestart; restart++) {
            sa.startTime = startTime + remainTime * restart / numRestart;
            sa.endTime = startTime + remainTime * (restart + 1) / numRestart;
            sa.startTemperature = endStartTemperature + (startStartTemperature - endStartTemperature) * ((numRestart - restart) / numRestart);
            sa.endTemperature = 1e-9;
            if (N < 20) {
                SA();
            } else {
                SA2();
            }
        }
    }

    private void SA() {
        sa.init();
        for (;; ++sa.numIterations) {
            if ((sa.numIterations & ((1 << 10) - 1)) == 0) {
                sa.update();
                if (sa.isTLE()) {
                    break;
                }
            }
            mutate();
        }
    }

    private void mutate() {
        double random = 8.001 * rng.nextDouble();
        if (random < 1) {
            swap();
        } else if (random < 2) {
            insert();
        } else if (random < 3) {
            reverse();
        } else if (random < 4) {
            swapHut();
        } else if (random < 8) {
            swapTail();
        } else if (random < 8.001) {
            bestInsert();
        }
    }

    private void SA2() {
        sa.init();
        for (;; ++sa.numIterations) {
            if ((sa.numIterations & ((1 << 10) - 1)) == 0) {
                sa.update();
                if (sa.isTLE()) {
                    break;
                }
            }
            mutate2();
        }
    }

    private void mutate2() {
        double random = 11.001 * rng.nextDouble();
        if (random < 1) {
            swap();
        } else if (random < 5) {
            insert();
        } else if (random < 6) {
            reverse();
        } else if (random < 7) {
            swapHut();
        } else if (random < 11) {
            swapTail();
        } else if (random < 11.001) {
            bestInsert();
        }
    }

    private void swapHut() {
        if (H == 1) {
            return;
        }
        int p1 = rng.nextInt(P);
        if (solution[p1].size() == 0) {
            return;
        }
        int i1 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p1].size()));
        Point p = solution[p1].get(i1);
        if (grid[p.r][p.c] != Hut) {
            return;
        }
        Point q = huts[(int) (rng.nextDouble() * huts.length)];
        while (q.r == p.r && q.c == p.c) {
            q = huts[(int) (rng.nextDouble() * huts.length)];
        }
        solution[p1].set(i1, q);
        float score_p1 = calculateScore(p1);
        float deltaScore = score_p1 - scores[p1];
        if (sa.accept(deltaScore)) {
            score += deltaScore;
            scores[p1] = score_p1;
            saveBest();
        } else {
            solution[p1].set(i1, p);
        }
    }

    private ArrayList<Point> tail1 = new ArrayList<>();
    private ArrayList<Point> tail2 = new ArrayList<>();

    private void swapTail() {
        if (P == 1) {
            return;
        }
        int p1 = rng.nextInt(P);
        int p2 = rng.nextInt(P - 1);
        if (p2 >= p1) {
            ++p2;
        }
        if (solution[p1].size() == 0) {
            return;
        }
        if (solution[p2].size() == 0) {
            return;
        }
        int i1 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p1].size()));
        int i2 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p2].size()));
        if (i1 == 0 && i2 == 0) {
            return;
        }
        float score_p1 = calculateScore(p1, i1, p2, i2);
        float score_p2 = calculateScore(p2, i2, p1, i1);
        float deltaScore = score_p1 - scores[p1] + score_p2 - scores[p2];
        if (sa.accept(deltaScore)) {
            score += deltaScore;
            scores[p1] = score_p1;
            scores[p2] = score_p2;
            for (int i = solution[p1].size() - 1; i >= i1; i--) {
                tail1.add(solution[p1].remove(i));
            }
            for (int i = solution[p2].size() - 1; i >= i2; i--) {
                tail2.add(solution[p2].remove(i));
            }
            for (int i = tail2.size() - 1; i >= 0; i--) {
                solution[p1].add(tail2.remove(i));
            }
            for (int i = tail1.size() - 1; i >= 0; i--) {
                solution[p2].add(tail1.remove(i));
            }
            saveBest();
        }
    }

    private void swap() {
        int p1 = rng.nextInt(P);
        int p2 = rng.nextInt(P);
        if (solution[p1].size() == 0) {
            return;
        }
        if (solution[p2].size() == 0) {
            return;
        }
        int i1 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p1].size()));
        int i2 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p2].size()));
        if (p1 == p2) {
            if (i1 == i2) {
                return;
            }
        }
        {
            Point swap = solution[p1].get(i1);
            solution[p1].set(i1, solution[p2].get(i2));
            solution[p2].set(i2, swap);
        }
        float score_p1 = calculateScore(p1);
        float deltaScore = score_p1 - scores[p1];
        float score_p2 = 0;
        if (p1 != p2) {
            score_p2 = calculateScore(p2);
            deltaScore += score_p2 - scores[p2];
        }
        if (sa.accept(deltaScore)) {
            score += deltaScore;
            scores[p1] = score_p1;
            if (p1 != p2) {
                scores[p2] = score_p2;
            }
            saveBest();
        } else {
            Point swap = solution[p1].get(i1);
            solution[p1].set(i1, solution[p2].get(i2));
            solution[p2].set(i2, swap);
        }
    }

    private void insert() {
        int p1 = rng.nextInt(P);
        int p2 = rng.nextInt(P);
        if (solution[p1].size() == 0) {
            return;
        }
        int i1 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p1].size()));
        int i2 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p2].size()));
        if (p1 == p2) {
            if (i1 == i2) {
                return;
            }
        }
        Point remove = solution[p1].remove(i1);
        solution[p2].add(i2, remove);
        float score_p1 = calculateScore(p1);
        float deltaScore = score_p1 - scores[p1];
        float score_p2 = 0;
        if (p1 != p2) {
            score_p2 = calculateScore(p2);
            deltaScore += score_p2 - scores[p2];
        }
        if (sa.accept(deltaScore)) {
            score += deltaScore;
            scores[p1] = score_p1;
            if (p1 != p2) {
                scores[p2] = score_p2;
            }
            saveBest();
        } else {
            solution[p2].remove(i2);
            solution[p1].add(i1, remove);
        }
    }

    private void bestInsert() {
        int p1 = rng.nextInt(P);
        int p2 = rng.nextInt(P);
        if (solution[p1].size() == 0) {
            return;
        }
        int i1 = rng.nextInt(solution[p1].size());
        Point remove = solution[p1].remove(i1);
        int bestI2 = 0;
        float best = -inf;
        for (int i2 = 0; i2 <= solution[p2].size(); i2++) {
            if (p1 == p2) {
                if (i1 == i2) {
                    continue;
                }
            }
            solution[p2].add(i2, remove);
            float score_p1 = calculateScore(p1);
            float deltaScore = score_p1 - scores[p1];
            float score_p2 = 0;
            if (p1 != p2) {
                score_p2 = calculateScore(p2);
                deltaScore += score_p2 - scores[p2];
            }
            if (deltaScore > best) {
                best = deltaScore;
                bestI2 = i2;
            }
            solution[p2].remove(i2);
        }
        int i2 = bestI2;
        solution[p2].add(i2, remove);
        float score_p1 = calculateScore(p1);
        float deltaScore = score_p1 - scores[p1];
        float score_p2 = 0;
        if (p1 != p2) {
            score_p2 = calculateScore(p2);
            deltaScore += score_p2 - scores[p2];
        }
        if (sa.accept(deltaScore)) {
            score += deltaScore;
            scores[p1] = score_p1;
            if (p1 != p2) {
                scores[p2] = score_p2;
            }
            saveBest();
        } else {
            solution[p2].remove(i2);
            solution[p1].add(i1, remove);
        }
    }

    private void reverse() {
        int p1 = rng.nextInt(P);
        if (solution[p1].size() == 0) {
            return;
        }
        int i1 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p1].size()));
        int i2 = (int) (rng.nextDouble() * rng.nextDouble() * (solution[p1].size()));
        if (i1 == i2) {
            return;
        }
        if (i1 > i2) {
            int swap = i1;
            i1 = i2;
            i2 = swap;
        }
        for (int l = i1, r = i2; l < r; l++, r--) {
            Point swap = solution[p1].get(l);
            solution[p1].set(l, solution[p1].get(r));
            solution[p1].set(r, swap);
        }
        float score_p1 = calculateScore(p1);
        float deltaScore = score_p1 - scores[p1];
        if (sa.accept(deltaScore)) {
            score += deltaScore;
            scores[p1] = score_p1;
            saveBest();
        } else {
            for (int l = i1, r = i2; l < r; l++, r--) {
                Point swap = solution[p1].get(l);
                solution[p1].set(l, solution[p1].get(r));
                solution[p1].set(r, swap);
            }
        }
    }

    private void read() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            N = Integer.parseInt(in.readLine());
            H = Integer.parseInt(in.readLine());
            P = Integer.parseInt(in.readLine());
            RD = (float) Double.parseDouble(in.readLine());
            CD = (float) Double.parseDouble(in.readLine());
            grid = new char[N][N];
            huts = new Point[H];
            for (int r = 0, hi = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    grid[r][c] = in.readLine().charAt(0);
                    if (grid[r][c] == Hut) {
                        huts[hi++] = new Point(r, c);
                    }
                }
            }
            values = new float[N][N];
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    values[r][c] = (float) Double.parseDouble(in.readLine());
                }
            }
            people = new Point[P];
            for (int i = 0; i < P; i++) {
                String[] temp = in.readLine().split(" ");
                int r = Integer.parseInt(temp[0]);
                int c = Integer.parseInt(temp[1]);
                people[i] = new Point(r, c);
            }
            calculateDistance();
            {
                maxTurn = N * N * N;
                float value = 1000.0f;
                float d = Math.max(RD, CD);
                for (int turn = 0; turn < N * N * N; turn++) {
                    if (value < 1.0) {
                        maxTurn = turn;
                        break;
                    }
                    value *= d;
                }
            }
            powCD = new float[N * N * N];
            powCD[0] = 1;
            for (int i = 1; i < powCD.length; i++) {
                powCD[i] = CD * powCD[i - 1];
            }
            powRD = new float[N * N * N];
            powRD[0] = 1;
            for (int i = 1; i < powRD.length; i++) {
                powRD[i] = RD * powRD[i - 1];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculateDistance() {
        distance = new int[N][N][N][N];
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                int[][] dist = distance[r][c];
                calculateDistance2(r, c, dist);
            }
        }
    }

    private void calculateDistance2(int r0, int c0, int[][] distance) {
        for (int r = 0; r < N; r++) {
            Arrays.fill(distance[r], inf);
        }
        LinkedList<Integer> queue = new LinkedList<>();
        if (grid[r0][c0] != Tree) {
            distance[r0][c0] = 0;
            queue.addLast(r0);
            queue.addLast(c0);
            queue.addLast(0);
        }
        while (!queue.isEmpty()) {
            int r = queue.pollFirst().intValue();
            int c = queue.pollFirst().intValue();
            int dist = queue.pollFirst().intValue();
            for (int d = 0; d < dr.length; d++) {
                int nr = r + dr[d];
                int nc = c + dc[d];
                if (!Utils.isValid(nr, 0, N) || !Utils.isValid(nc, 0, N)) {
                    continue;
                }
                if (grid[nr][nc] == Tree) {
                    continue;
                }
                int ndist = dist + 1;
                if (ndist >= distance[nr][nc]) {
                    continue;
                }
                distance[nr][nc] = ndist;
                queue.addLast(nr);
                queue.addLast(nc);
                queue.addLast(ndist);
            }
        }
    }

    private void write() {
        ArrayList<Character>[] moves = new ArrayList[P];
        for (int p = 0; p < P; p++) {
            moves[p] = new ArrayList<>();
            int r = people[p].r;
            int c = people[p].c;
            for (int i = 0; i < solution[p].size(); i++) {
                Point point = solution[p].get(i);
                while (!(r == point.r && c == point.c)) {
                    boolean find = false;
                    int ndist = distance[point.r][point.c][r][c] - 1;
                    for (int d = 0; d < dr.length; d++) {
                        int nr = r + dr[d];
                        int nc = c + dc[d];
                        if (!Utils.isValid(nr, 0, N) || !Utils.isValid(nc, 0, N)) {
                            continue;
                        }
                        if (grid[nr][nc] == Tree) {
                            continue;
                        }
                        if (distance[point.r][point.c][nr][nc] == ndist) {
                            r = nr;
                            c = nc;
                            moves[p].add(dirs[d]);
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
//                        Utils.debug("ndist", ndist);
                        throw new AssertionError();
                    }
                }
                if (grid[point.r][point.c] == Mushroom) {
                    moves[p].add('C');
                } else if (grid[point.r][point.c] == Hut) {
                }
                if (moves[p].size() > maxTurn) {
                    break;
                }
            }
        }
        int numMoves = 0;
        for (int p = 0; p < P; p++) {
            numMoves = Math.max(numMoves, Math.min(maxTurn, moves[p].size()));
        }
        for (int p = 0; p < P; p++) {
            while (moves[p].size() < numMoves) {
                moves[p].add('S');
            }
        }
        StringBuilder res = new StringBuilder();
        res.append(numMoves).append('\n');
        for (int n = 0; n < numMoves; n++) {
            for (int p = 0; p < P; p++) {
                res.append(moves[p].get(n)).append('\n');
            }
        }
//        Utils.debug("write", "time", watch.getSecondString());
        System.out.print(res.toString());
        System.out.flush();
    }

    static class Point {
        int r;
        int c;

        public Point(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }
}

class Utils {
    private Utils() {
    }

    public static final void debug(Object... o) {
        System.err.println(toString(o));
        System.err.flush();
    }

    public static final String toString(Object... o) {
        return Arrays.deepToString(o);
    }

    public static boolean isValid(int v, int min, int minUpper) {
        return v >= min && v < minUpper;
    }
}

class Watch {
    private long start;

    public Watch() {
        init();
    }

    public double getSecond() {
        return (System.nanoTime() - start) * 1e-9;
    }

    public void init() {
        init(System.nanoTime());
    }

    private void init(long start) {
        this.start = start;
    }

    public String getSecondString() {
        return toString(getSecond());
    }

    public static final String toString(double second) {
        if (second < 60) {
            return String.format("%5.2fs", second);
        } else if (second < 60 * 60) {
            int minute = (int) (second / 60);
            return String.format("%2dm%2ds", minute, (int) (second % 60));
        } else {
            int hour = (int) (second / (60 * 60));
            int minute = (int) (second / 60);
            return String.format("%2dh%2dm%2ds", hour, minute % (60), (int) (second % 60));
        }
    }
}

final class PCG_XSH_RR {
    private long state = 5342;

    public PCG_XSH_RR(final long state) {
        this.state = state;
    }

    public int nextInt() {
        final long oldstate = state;
        state = oldstate * 6364136223846793005L + 521L;
        final int xorshift = (int) (((oldstate >>> 18) ^ oldstate) >>> 27);
        final int rotation = (int) (oldstate >>> 59);
        return (xorshift >>> rotation) | (xorshift << (-rotation & 31));
    }

    public int nextInt(int n) {
        return (int) (n * nextDouble());
    }

    public double nextDouble() {
        return (nextInt() >>> 1) * 4.6566128730773926E-10;
    }
}

class SAState {
    public static final boolean useTime = true;
    public double startTime;
    public double endTime;
    public double time;
    public double startTemperature;
    public double endTemperature;
    public double inverseTemperature;
    public double lastAcceptTemperature;
    public double startRange;
    public double endRange;
    public double range;
    public int numIterations;
    public int validIterations;
    public int acceptIterations;
    private double[] log = new double[32768];

    public SAState() {
        for (int i = 0; i < log.length; i++) {
            log[i] = Math.log((i + 0.5) / log.length);
        }
    }

    public void init() {
        numIterations = 0;
        validIterations = 0;
        acceptIterations = 0;
        startTime = useTime ? Mushrooms.watch.getSecond() : numIterations;
        update();
        lastAcceptTemperature = inverseTemperature;
    }

    public void update() {
        updateTime();
        updateTemperature();
    }

    public boolean useExp = !true;

    public void updateTemperature() {
        if (useExp) {
            double time0to1 = elapsedPercentage(startTime, endTime, time);
            double startY = startTemperature;
            double endY = endTemperature;
            double startX = Math.log(startY);
            double endX = Math.log(endY);
            double xStartToEnd = interpolate(startX, endX, time0to1);
            double temperature = Math.exp(xStartToEnd);
            inverseTemperature = 1.0 / temperature;
        } else {
            double time0to1 = elapsedPercentage(startTime, endTime, time);
            double startY = startTemperature;
            double endY = endTemperature;
            double temperature = interpolate(startY, endY, time0to1);
            inverseTemperature = 1.0 / temperature;
        }
    }

    private double elapsedPercentage(double min, double max, double v) {
        return (v - min) / (max - min);
    }

    private double interpolate(double v0, double v1, double d0to1) {
        return v0 + (v1 - v0) * d0to1;
    }

    public void updateRange() {
        range = endRange + (startRange - endRange) * Math.pow((endTime - time) / (endTime - startTime), 1.0);
    }

    public void updateTime() {
        time = useTime ? Mushrooms.watch.getSecond() : numIterations;
    }

    public boolean isTLE() {
        return time >= endTime;
    }

    public boolean accept(double deltaScore) {
        return acceptB(deltaScore);
    }

    public boolean acceptB(double deltaScore) {
        validIterations++;
        if (deltaScore > -1e-9) {
            acceptIterations++;
            return true;
        }
        double d = deltaScore * inverseTemperature;
        if (d < -10) {
            return false;
        }
        if (log[Mushrooms.rng.nextInt() & 32767] < d) {
            acceptIterations++;
            lastAcceptTemperature = inverseTemperature;
            return true;
        }
        return false;
    }

    public boolean acceptS(double deltaScore) {
        validIterations++;
        if (deltaScore < 1e-9) {
            acceptIterations++;
            return true;
        }
        double d = -deltaScore * inverseTemperature;
        if (d < -10) {
            return false;
        }
        if (log[Mushrooms.rng.nextInt() & 32767] < d) {
            acceptIterations++;
            lastAcceptTemperature = inverseTemperature;
            return true;
        }
        return false;
    }
}
