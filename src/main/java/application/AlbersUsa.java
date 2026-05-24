package application;

/**
 * Albers USA composite equal-area conic projection.
 *
 * Port of d3-geo's geoAlbersUsa: three regional Albers projections
 * (contiguous US, Alaska, Hawaii) composed by trying each and returning
 * the result that lands inside its clip extent. Alaska and Hawaii are
 * placed as insets in the lower-left.
 *
 * Instances are scaled to a specific canvas size; the same K-per-pixel
 * ratio is preserved so dot positions scale uniformly with the canvas.
 */
public final class AlbersUsa {

    private static final double BASE_W = 763.0;
    private static final double BASE_H = 449.0;
    private static final double BASE_K = 820.0;
    private static final double EPS = 1e-6;

    private final double k, tx, ty;
    private final ConicEqualArea lower48, alaska, hawaii;
    private final double[] lower48Clip, alaskaClip, hawaiiClip;

    public AlbersUsa(double width, double height) {
        this.k = Math.min(width / BASE_W, height / BASE_H) * BASE_K;
        this.tx = width / 2.0;
        this.ty = height / 2.0;

        this.lower48 = new ConicEqualArea(29.5, 45.5, -0.6, 38.7, 96.0, k, tx, ty);
        this.alaska = new ConicEqualArea(55.0, 65.0, -2.0, 58.5, 154.0, k * 0.35,
                tx - 0.307 * k, ty + 0.201 * k);
        this.hawaii = new ConicEqualArea(8.0, 18.0, -3.0, 19.9, 157.0, k,
                tx - 0.205 * k, ty + 0.212 * k);

        this.lower48Clip = new double[]{
                tx - 0.455 * k, ty - 0.238 * k,
                tx + 0.455 * k, ty + 0.238 * k};
        this.alaskaClip = new double[]{
                tx - 0.425 * k + EPS, ty + 0.120 * k + EPS,
                tx - 0.214 * k - EPS, ty + 0.234 * k - EPS};
        this.hawaiiClip = new double[]{
                tx - 0.214 * k + EPS, ty + 0.166 * k + EPS,
                tx - 0.115 * k - EPS, ty + 0.234 * k - EPS};
    }

    /** Returns [x, y] on the canvas, or null if the point lies outside every region. */
    public double[] project(double lngDeg, double latDeg) {
        double[] xy;
        xy = lower48.project(lngDeg, latDeg);
        if (inBox(xy, lower48Clip)) return xy;
        xy = alaska.project(lngDeg, latDeg);
        if (inBox(xy, alaskaClip)) return xy;
        xy = hawaii.project(lngDeg, latDeg);
        if (inBox(xy, hawaiiClip)) return xy;
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
