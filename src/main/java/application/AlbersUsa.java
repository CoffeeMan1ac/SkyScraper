package application;

/**
 * Albers USA composite equal-area conic projection, scaled to a 763×449 canvas.
 *
 * A port of d3-geo's geoAlbersUsa: three regional Albers projections
 * (contiguous US, Alaska, Hawaii) are composed by trying each in order and
 * returning the result whose projected coords land inside that region's
 * pre-computed clip extent. Alaska and Hawaii are positioned as insets in
 * the lower-left, matching what the original PNG showed.
 */
public final class AlbersUsa {

    private static final double WIDTH = 763.0;
    private static final double HEIGHT = 449.0;
    private static final double K = 820.0;
    private static final double TX = WIDTH / 2.0;
    private static final double TY = HEIGHT / 2.0;
    private static final double EPS = 1e-6;

    private static final ConicEqualArea LOWER48 = new ConicEqualArea(
            29.5, 45.5, -0.6, 38.7, 96.0, K, TX, TY);
    private static final ConicEqualArea ALASKA = new ConicEqualArea(
            55.0, 65.0, -2.0, 58.5, 154.0, K * 0.35,
            TX - 0.307 * K, TY + 0.201 * K);
    private static final ConicEqualArea HAWAII = new ConicEqualArea(
            8.0, 18.0, -3.0, 19.9, 157.0, K,
            TX - 0.205 * K, TY + 0.212 * K);

    // Clip extents in canvas space; first region whose projection lands inside wins.
    private static final double[] LOWER48_CLIP = {
            TX - 0.455 * K, TY - 0.238 * K,
            TX + 0.455 * K, TY + 0.238 * K};
    private static final double[] ALASKA_CLIP = {
            TX - 0.425 * K + EPS, TY + 0.120 * K + EPS,
            TX - 0.214 * K - EPS, TY + 0.234 * K - EPS};
    private static final double[] HAWAII_CLIP = {
            TX - 0.214 * K + EPS, TY + 0.166 * K + EPS,
            TX - 0.115 * K - EPS, TY + 0.234 * K - EPS};

    private AlbersUsa() {}

    /** Returns [x, y] on the 763×449 canvas, or null if the point lies outside every region. */
    public static double[] project(double lngDeg, double latDeg) {
        double[] xy;
        xy = LOWER48.project(lngDeg, latDeg);
        if (inBox(xy, LOWER48_CLIP)) return xy;
        xy = ALASKA.project(lngDeg, latDeg);
        if (inBox(xy, ALASKA_CLIP)) return xy;
        xy = HAWAII.project(lngDeg, latDeg);
        if (inBox(xy, HAWAII_CLIP)) return xy;
        return null;
    }

    private static boolean inBox(double[] p, double[] box) {
        return p[0] >= box[0] && p[0] <= box[2] && p[1] >= box[1] && p[1] <= box[3];
    }

    /** Single regional Albers equal-area conic projection. */
    private static final class ConicEqualArea {
        private final double n;
        private final double c;
        private final double rho0;
        private final double rotateLngRad;
        private final double cx, cy;
        private final double k, tx, ty;

        ConicEqualArea(double phi1Deg, double phi2Deg,
                       double centerLngDeg, double centerLatDeg,
                       double rotateLngDeg,
                       double k, double tx, double ty) {
            double phi1 = Math.toRadians(phi1Deg);
            double phi2 = Math.toRadians(phi2Deg);
            double sinPhi1 = Math.sin(phi1);
            this.n = (sinPhi1 + Math.sin(phi2)) / 2.0;
            this.c = Math.cos(phi1) * Math.cos(phi1) + 2.0 * n * sinPhi1;
            this.rho0 = Math.sqrt(c) / n;
            this.rotateLngRad = Math.toRadians(rotateLngDeg);

            double[] centerProj = rawProject(
                    Math.toRadians(centerLngDeg),
                    Math.toRadians(centerLatDeg));
            this.cx = centerProj[0];
            this.cy = centerProj[1];

            this.k = k;
            this.tx = tx;
            this.ty = ty;
        }

        private double[] rawProject(double lambda, double phi) {
            double theta = n * lambda;
            double rho = Math.sqrt(c - 2.0 * n * Math.sin(phi)) / n;
            return new double[]{rho * Math.sin(theta), rho0 - rho * Math.cos(theta)};
        }

        double[] project(double lngDeg, double latDeg) {
            double lambda = Math.toRadians(lngDeg) + rotateLngRad;
            while (lambda > Math.PI) lambda -= 2.0 * Math.PI;
            while (lambda < -Math.PI) lambda += 2.0 * Math.PI;
            double phi = Math.toRadians(latDeg);

            double[] raw = rawProject(lambda, phi);
            return new double[]{
                    tx + k * (raw[0] - cx),
                    ty - k * (raw[1] - cy)
            };
        }
    }
}
