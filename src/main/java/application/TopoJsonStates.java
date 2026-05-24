package application;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a us-atlas TopoJSON file into a single SVG path d-string, with every
 * state's outline projected through {@link AlbersUsa}.
 *
 * TopoJSON represents shared boundaries as "arcs" (delta-encoded coordinate
 * sequences) and each polygon ring as a list of signed arc indices. A
 * negative index ~i means "use arc |i+1| reversed." This decodes that.
 */
public final class TopoJsonStates {

    /** us-atlas TopoJSON uses FIPS state codes as feature IDs; map them to 2-letter abbreviations. */
    private static final Map<String, String> FIPS_TO_ABBR = Map.ofEntries(
            Map.entry("01", "AL"), Map.entry("02", "AK"), Map.entry("04", "AZ"),
            Map.entry("05", "AR"), Map.entry("06", "CA"), Map.entry("08", "CO"),
            Map.entry("09", "CT"), Map.entry("10", "DE"), Map.entry("11", "DC"),
            Map.entry("12", "FL"), Map.entry("13", "GA"), Map.entry("15", "HI"),
            Map.entry("16", "ID"), Map.entry("17", "IL"), Map.entry("18", "IN"),
            Map.entry("19", "IA"), Map.entry("20", "KS"), Map.entry("21", "KY"),
            Map.entry("22", "LA"), Map.entry("23", "ME"), Map.entry("24", "MD"),
            Map.entry("25", "MA"), Map.entry("26", "MI"), Map.entry("27", "MN"),
            Map.entry("28", "MS"), Map.entry("29", "MO"), Map.entry("30", "MT"),
            Map.entry("31", "NE"), Map.entry("32", "NV"), Map.entry("33", "NH"),
            Map.entry("34", "NJ"), Map.entry("35", "NM"), Map.entry("36", "NY"),
            Map.entry("37", "NC"), Map.entry("38", "ND"), Map.entry("39", "OH"),
            Map.entry("40", "OK"), Map.entry("41", "OR"), Map.entry("42", "PA"),
            Map.entry("44", "RI"), Map.entry("45", "SC"), Map.entry("46", "SD"),
            Map.entry("47", "TN"), Map.entry("48", "TX"), Map.entry("49", "UT"),
            Map.entry("50", "VT"), Map.entry("51", "VA"), Map.entry("53", "WA"),
            Map.entry("54", "WV"), Map.entry("55", "WI"), Map.entry("56", "WY"));

    private TopoJsonStates() {}

    /** Returns one SVG path d-string per state, keyed by 2-letter abbreviation. */
    public static Map<String, String> toStatePaths(InputStream topoJsonStream, AlbersUsa projection) {
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

        Map<String, String> result = new LinkedHashMap<>();
        for (JsonElement g : geometries) {
            JsonObject geom = g.getAsJsonObject();
            String fips = geom.has("id") ? geom.get("id").getAsString() : null;
            String abbr = fips == null ? null : FIPS_TO_ABBR.get(fips);
            if (abbr == null) continue;

            StringBuilder d = new StringBuilder();
            String type = geom.get("type").getAsString();
            JsonArray geomArcs = geom.getAsJsonArray("arcs");
            if ("Polygon".equals(type)) {
                writePolygon(d, geomArcs, absArcs, projection);
            } else if ("MultiPolygon".equals(type)) {
                for (JsonElement polyArcs : geomArcs) {
                    writePolygon(d, polyArcs.getAsJsonArray(), absArcs, projection);
                }
            }
            if (d.length() > 0) result.put(abbr, d.toString());
        }
        return result;
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
