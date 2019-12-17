import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Day12 {

    private static final Pattern MOON_PATTERN = Pattern.compile("<x=(-?\\d+), y=(-?\\d+), z=(-?\\d+)>");

    public static void main(final String[] args) {
        final List<Moon> moons = parseMoons(getInput());
        final Set<String> seenHashes = new HashSet<>();

        int steps = 0;

        while (true) {
            // Adjust velocity
            for (final Moon moon : moons) {
                for (final Moon otherMoon : moons) {
                    if (moon.equals(otherMoon)) {
                        continue; // Our own gravity does not effect us
                    }

                    moon.xVel += Integer.compare(otherMoon.x, moon.x);
                    moon.yVel += Integer.compare(otherMoon.y, moon.y);
                    moon.zVel += Integer.compare(otherMoon.z, moon.z);
                }
            }

            // Update position
            for (final Moon moon : moons) {
                moon.x += moon.xVel;
                moon.y += moon.yVel;
                moon.z += moon.zVel;
            }

            steps++;

            if (steps == 1000) {
                final int totalEnergy = moons.stream().mapToInt(m -> {
                    final int potentialEnergy = Math.abs(m.x) + Math.abs(m.y) + Math.abs(m.z);
                    final int kineticEnergy = Math.abs(m.xVel) + Math.abs(m.yVel) + Math.abs(m.zVel);
                    return potentialEnergy * kineticEnergy;
                }).sum();
                System.out.println("Total energy: " + totalEnergy);

                //TODO: Solve part 2...
                break;
            }

            System.out.println(moons);
        }
    }

    private static List<Moon> parseMoons(final String input) {
        final List<Moon> moons = new ArrayList<>();
        for (final String moonData : input.split("\n")) {
            final Matcher matcher = MOON_PATTERN.matcher(moonData);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid input");
            }
            moons.add(new Moon(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
            ));
        }

        return moons;
    }

    private static class Moon {
        private int x;
        private int y;
        private int z;

        private int xVel;
        private int yVel;
        private int zVel;

        public Moon(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format(
                "pos=<x=%d, y=%d, z=%d>, vel=<x=%d, y=%d, z=%d>",
                x, y, z, xVel, yVel, zVel
            );
        }
    }

    private static String hash(final List<Moon> moons) {
        final StringJoiner hashBuilder = new StringJoiner(",");
        for (final Moon moon : moons) {
            hashBuilder.add(moon.toString());
        }
        return hashBuilder.toString();
    }

    private static String getTestInput() {
        return "<x=-1, y=0, z=2>\n"
            + "<x=2, y=-10, z=-7>\n"
            + "<x=4, y=-8, z=8>\n"
            + "<x=3, y=5, z=-1>";
    }

    private static String getInput() {
        return "<x=14, y=9, z=14>\n"
            + "<x=9, y=11, z=6>\n"
            + "<x=-6, y=14, z=-4>\n"
            + "<x=4, y=-4, z=-3>";
    }
}
