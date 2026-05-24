package application;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a us-atlas TopoJSON file into a single SVG path d-string, with every
 * state's outline projected through {@link AlbersUsa}.
 *
 * TopoJSON represents shared boundaries as "arcs" (delta-encoded coordinate
 * sequences) and each polygon ring as a list of signed arc indices. A
 * negative index ~i means "use arc |i+1| reversed." This decodes that.
 */
public final class TopoJsonStates {

    private TopoJsonStates() {}

    public static String toSvgPath(InputStream topoJsonStream, AlbersUsa projection) {
        JsonObject topo = JsonParser.parseReader(
                new InputStreamReader(topoJsonStream, StandardCharsets.UTF_8)).getAsJsonObject();

        JsonObject transform = topo.getAsJsonObject("transform");
        double[] scale = readPair(transform.getAsJsonArray("scale"));
        double[] translate = readPair(transform.getAsJsonArray("translate"));

        // Decode each delta-encoded arc into absolute [lng, lat] pairs.
        JsonArray arcs = topo.getAsJsonArray("arcs");
        double[][][] absArcs = new double[arcs.size()][][];
        for (int i = 0; i < arcs.size(); i++) {
            JsonArray arc = arcs.get(i).getAsJsonArray();
            double[][] points = new double[arc.size()][2];
            double x = 0, y = 0;
            for (int j = 0; j < arc.size(); j++) {
                JsonArray pt = arc.get(j).getAsJsonArray();
                x += pt.get(0).getAsDouble();
                y += pt.get(1).getAsDouble();
                points[j][0] = x * scale[0] + translate[0];
                points[j][1] = y * scale[1] + translate[1];
            }
            absArcs[i] = points;
        }

        JsonObject states = topo.getAsJsonObject("objects").getAsJsonObject("states");
        JsonArray geometries = states.getAsJsonArray("geometries");

        StringBuilder d = new StringBuilder();
        for (JsonElement g : geometries) {
            JsonObject geom = g.getAsJsonObject();
            String type = geom.get("type").getAsString();
            JsonArray geomArcs = geom.getAsJsonArray("arcs");
            if ("Polygon".equals(type)) {
                writePolygon(d, geomArcs, absArcs, projection);
            } else if ("MultiPolygon".equals(type)) {
                for (JsonElement polyArcs : geomArcs) {
                    writePolygon(d, polyArcs.getAsJsonArray(), absArcs, projection);
                }
            }
        }
        return d.toString();
    }

    private static void writePolygon(StringBuilder d, JsonArray rings, double[][][] absArcs, AlbersUsa projection) {
        for (JsonElement ring : rings) {
            JsonArray ringArcs = ring.getAsJsonArray();
            List<double[]> ringPoints = new ArrayList<>();
            for (JsonElement arcRef : ringArcs) {
                int idx = arcRef.getAsInt();
                double[][] arc;
                boolean reverse = false;
                if (idx >= 0) {
                    arc = absArcs[idx];
                } else {
                    arc = absArcs[~idx];
                    reverse = true;
                }
                if (reverse) {
                    for (int k = arc.length - 1; k >= 0; k--) {
                        appendIfNotDup(ringPoints, arc[k]);
                    }
                } else {
                    for (double[] p : arc) {
                        appendIfNotDup(ringPoints, p);
                    }
                }
            }

            boolean first = true;
            for (double[] pt : ringPoints) {
                double[] xy = projection.project(pt[0], pt[1]);
                if (xy == null) continue;
                if (first) {
                    d.append('M').append(fmt(xy[0])).append(',').append(fmt(xy[1]));
                    first = false;
                } else {
                    d.append('L').append(fmt(xy[0])).append(',').append(fmt(xy[1]));
                }
            }
            if (!first) d.append('Z');
        }
    }

    private static void appendIfNotDup(List<double[]> ring, double[] p) {
        if (ring.isEmpty()) {
            ring.add(p);
            return;
        }
        double[] last = ring.get(ring.size() - 1);
        if (last[0] != p[0] || last[1] != p[1]) ring.add(p);
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static double[] readPair(JsonArray pair) {
        return new double[]{pair.get(0).getAsDouble(), pair.get(1).getAsDouble()};
    }
}
