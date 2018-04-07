package eu.mihosoft.vrl.vrljoglplugin.glview;

import eu.mihosoft.vvecmath.Vector3d;

/**
 * A simple glMesh class.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public final class Mesh {

    float[] vertices;
    int[] indices;

    private Mesh(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
    }

    public static Mesh newInstance(float[] vertices, int[] indices) {
        return new Mesh(vertices,indices);
    }

    float xmin() {
        return min(0);
    }

    float ymin() {
        return min(1);
    }

    float zmin() {
        return min(2);
    }

    float xmax() {
        return max(0);
    }

    float ymax() {
        return max(1);
    }

    float zmax() {
        return max(2);
    }

    float min(int start) {
        if (start >= vertices.length) {
            return -1;
        }
        float v = vertices[start];
        for (int i = start; i < vertices.length; i += 3) {
            v = Math.min(v, vertices[i]);
        }
        return v;
    }

    float max(int start) {
        if (start >= vertices.length) {
            return 1;
        }
        float v = vertices[start];
        for (int i = start; i < vertices.length; i += 3) {
            v = Math.max(v, vertices[i]);
        }
        return v;
    }

    boolean isEmpty() {
        return vertices.length == 0;
    }

    public Bounds getBounds() {
        return new Bounds(Vector3d.xyz(xmin(),ymin(),zmin()),Vector3d.xyz(xmax(),ymax(),zmax()));
    }

    public static final class Bounds {
        private final Vector3d min;
        private final Vector3d max;

        private Bounds(Vector3d min, Vector3d max) {
            this.min = min;
            this.max = max;
        }

        public Vector3d getMin() {
            return min;
        }

        public Vector3d getMax() {
            return max;
        }
    }
}