import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class Day10 {

    private static final List<Point> ASTEROIDS = new ArrayList<>();

    public static void main(final String[] args) {
        parseAsteroids(getInput());

        final int maxX = ASTEROIDS.stream().mapToInt(p -> p.x).max().orElse(0);
        final int maxY = ASTEROIDS.stream().mapToInt(p -> p.y).max().orElse(0);

        final Point miningStation = solvePart1(maxX, maxY);
        final Point blasted = solvePart2(miningStation);

        System.out.printf("200th asteroid to be shot: (%d, %d)%n", blasted.x, blasted.y);
    }

    private static Point solvePart1(int maxX, int maxY) {
        int highscore = 0;
        Point candidate = null;
        for (final Point asteroid : ASTEROIDS) {
            final Set<Double> angles = new HashSet<>();
            for (int y = 0; y <= maxY; y++) {
                for (int x = 0; x <= maxX; x++) {
                    if (asteroid.x == x && asteroid.y == y) {
                        continue; // This is our self, skip.
                    }

                    if (!asteroidAt(x, y)) {
                        continue; // No asteroid at this spot, skip.
                    }

                    // Can we _see_ the asteroid at X,Y?
                    // Make X and Y relative to us.
                    final int offsetX = asteroid.x - x;
                    final int offsetY = asteroid.y - y;
                    // What's the angle to X,Y?
                    final double angle = Math.atan2(offsetX, offsetY);

                    // Add that angle to a hash set. Duplicate angles will be filtered out.
                    // Which means asteroids hiding behind another asteroid at the same angle will be filterd out.
                    angles.add(angle);
                }
            }
            System.out.printf("Asteroid at (%d,%d) can see %d asteroids", asteroid.x, asteroid.y, angles.size());
            System.out.println();

            if (angles.size() > highscore) {
                System.out.printf("CANDIDATE ASTEROID FOUND (%d,%d), unique angles: %d", asteroid.x, asteroid.y, angles.size());
                System.out.println();
                highscore = angles.size();
                candidate = asteroid;
            }
        }

        System.out.println("Most asteroids seen: " + highscore);
        return candidate;
    }

    private static Point solvePart2(final Point miningStation) {
        // Set each asteroids relative angle to mining station
        // Sort the list by descending degrees (360 being up top, 359 being first step clockwise)
        final List<RelPoint> relAsteroidsClockwise = ASTEROIDS.stream()
            .map(a -> {
                final double atan2 = Math.atan2(miningStation.x - a.x, miningStation.y - a.y);
                final double relDeg = (atan2 > 0 ? atan2 : (2 * Math.PI + atan2)) * 360 / (2 * Math.PI);
                return new RelPoint(a.x, a.y, relDeg);
            })
            .filter(r -> !(r.x == miningStation.x && r.y == miningStation.y)) // Remove our self
            .sorted(Comparator
                .comparing(a -> ((RelPoint) a).relDeg).reversed() // Sort by degrees descending
                .thenComparingDouble(a -> distanceBetween(miningStation, (Point) a)) // Sort by distance to mining station ascending
            )
            .collect(Collectors.toList());

        final List<RelPoint> blastedAsteroids = new ArrayList<>();
        while (blastedAsteroids.size() < relAsteroidsClockwise.size()) {
            // Each iteration of this loop is one clockwise rotation of laser
            final Set<Double> blastedAngles = new HashSet<>();
            // Try to blast each asteroid.
            for (final RelPoint asteroid : relAsteroidsClockwise) {
                final boolean blastedSuccessfully = blastedAngles.add(asteroid.relDeg);
                // If we haven't shot our lazer at this angle before, "blastedSuccessfully" should be true.
                if (blastedSuccessfully) {
                    blastedAsteroids.add(asteroid);
                }
            }
        }

        return blastedAsteroids.get(199);
    }

    private static boolean asteroidAt(final int x, final int y) {
        return ASTEROIDS.stream().anyMatch(p -> p.x == x && p.y == y);
    }

    private static double distanceBetween(final Point first, final Point second) {
        final double xDelta = second.x - first.x;
        final double yDelta = second.y - first.y;
        return Math.sqrt(xDelta * xDelta + yDelta * yDelta);
    }

    private static void parseAsteroids(final String input) {
        int x = 0;
        int y = 0;
        for (final char c : input.toCharArray()) {
            if (c == '#') {
                ASTEROIDS.add(new Point(x, y));
                x++;
            } else if (c == '\n') {
                x = 0;
                y++;
            } else {
                x++;
            }
        }
    }

    private static class Point {
        final int x;
        final int y;

        private Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class RelPoint extends Point {
        private final double relDeg;

        private RelPoint(final int absX, final int absY, final double relativeDegrees) {
            super(absX, absY);
            this.relDeg = relativeDegrees;
        }
    }

    private static String getTestInputP1() {
        return "#.#...#.#.\n"
            + ".###....#.\n"
            + ".#....#...\n"
            + "##.#.#.#.#\n"
            + "....#.#.#.\n"
            + ".##..###.#\n"
            + "..#...##..\n"
            + "..##....##\n"
            + "......#...\n"
            + ".####.###.";
    }

    private static String getTestInputP1_Big() {
        return ".#..##.###...#######\n"
            + "##.############..##.\n"
            + ".#.######.########.#\n"
            + ".###.#######.####.#.\n"
            + "#####.##.#.##.###.##\n"
            + "..#####..#.#########\n"
            + "####################\n"
            + "#.####....###.#.#.##\n"
            + "##.#################\n"
            + "#####.##.###..####..\n"
            + "..######..##.#######\n"
            + "####.##.####...##..#\n"
            + ".#####..#.######.###\n"
            + "##...#.##########...\n"
            + "#.##########.#######\n"
            + ".####.#.###.###.#.##\n"
            + "....##.##.###..#####\n"
            + ".#.#.###########.###\n"
            + "#.#.#.#####.####.###\n"
            + "###.##.####.##.#..##";
    }

    private static String getTestInputP2() {
        return ".#....#####...#..\n"
            + "##...##.#####..##\n"
            + "##...#...#.#####.\n"
            + "..#.....#...###..\n"
            + "..#.#.....#....##";
    }

    private static String getInput() {
        return ".###.###.###.#####.#\n"
            + "#####.##.###..###..#\n"
            + ".#...####.###.######\n"
            + "######.###.####.####\n"
            + "#####..###..########\n"
            + "#.##.###########.#.#\n"
            + "##.###.######..#.#.#\n"
            + ".#.##.###.#.####.###\n"
            + "##..#.#.##.#########\n"
            + "###.#######.###..##.\n"
            + "###.###.##.##..####.\n"
            + ".##.####.##########.\n"
            + "#######.##.###.#####\n"
            + "#####.##..####.#####\n"
            + "##.#.#####.##.#.#..#\n"
            + "###########.#######.\n"
            + "#.##..#####.#####..#\n"
            + "#####..#####.###.###\n"
            + "####.#.############.\n"
            + "####.#.#.##########.";
    }
}
