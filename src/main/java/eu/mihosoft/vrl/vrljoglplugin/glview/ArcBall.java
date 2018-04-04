package eu.mihosoft.vrl.vrljoglplugin.glview;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * A UI independent arcball implementation.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public final class ArcBall {

    // tolerance
    private static final float EPSILON = 1.0e-5f;

    private Vector3f startPoint;            // start point (first click) vector in ball coords
    private Vector3f endPoint;              // end point (during drag) vector in ball coords

    private float scaleW;                   // scales mouse coords to [-1,1]
    private float scaleH;                   // scales mouse coords to [-1,1]

    // current rotation (drag start to current drag location)
    private Quaternionf currentRot = new Quaternionf().identity();

    // indicates whether we are currently dragging
    private boolean dragging;

    // contains the complete rotation (including current delta if dragging)
    private final Matrix4f rotMat = new Matrix4f();

    // contains the complete rotation (including current delta if dragging)
    private Quaternionf rotQuat = new Quaternionf();

    /**
     * Creates a new arc ball.
     * @param width window getWidth
     * @param height window getHeight
     */
    public ArcBall(float width, float height) {
        startPoint = new Vector3f();
        endPoint = new Vector3f();
        setBounds(width, height);
    }

    /**
     * Resets this arcball.
     */
    public void reset() {
        rotMat.identity();
        rotQuat = new Quaternionf();
        currentRot = new Quaternionf();
        startPoint = new Vector3f();
        endPoint = new Vector3f();
        dragging = false;
        setBounds(1.f,1.f);

    }

    /**
     * Maps the specified point (in mouse/window coordinates) to the virtual ball.
     *
     * @param x x coordinate (cursor location)
     * @param y y coordinate (cursor location)
     * @param vector vector that stores the mapped location (instance is modified)
     */
    private void mapToSphere(int x, int y, Vector3f vector) {

        Vector2f scaledP = new Vector2f(x, y);

        // adjusts the point coords and scale down to range of [-1 ... 1]
        scaledP.x = (x * this.scaleW) - 1.0f;
        scaledP.y = (y * this.scaleH) - 1.0f;

        // computes the square of the length of the vector to the point from the center
        float lengthSq = (scaledP.x * scaledP.x) + (scaledP.y * scaledP.y);

        // if the point is mapped outside of the sphere... (length > radius squared)
        if (lengthSq > 1.0f) {
            // Compute a normalizing factor (radius / sqrt(length))
            float norm = (float) (1.0 / java.lang.Math.sqrt(lengthSq));
            //
            // return the "normalized" vector, a point on the sphere
            vector.x = scaledP.x * norm;
            vector.y = scaledP.y * norm;
            vector.z = 0.0f;
        } else {
            // the point is on the inside of the sphere
            //
            // return a vector to a point mapped inside the sphere
            // sqrt(radius squared - length)
            vector.x = scaledP.x;
            vector.y = scaledP.y;
            vector.z = (float) java.lang.Math.sqrt(1.0f - lengthSq);
        }
    }

    /**
     * Sets the window/surface bounds of this arcball.
     * @param width width (>= 1.0f)
     * @param height height (>= 1.0f)
     */
    public void setBounds(float width, float height) {
        if ((width < 1.0f) || (height < 1.0f)) {
            throw new RuntimeException("Windows width and height cannot be smaller than 1.0f");
        }

        // set scale factor for getWidth/getHeight (for mapping window coords to [-1,1] )
        scaleW = 1.0f / ((width - 1.0f) * 0.5f);
        scaleH = 1.0f / ((height - 1.0f) * 0.5f);
    }

    /**
     * Starts a dragging gesture (mouse move with left button down)
     * @param x x coordinate (cursor location)
     * @param y y coordinate (cursor location)
     */
    public void beginDrag(int x, int y) {

        dragging = true;

        // reset the current rotation
        currentRot = new Quaternionf(rotQuat);

        mapToSphere(x,y, this.startPoint);
    }

    /**
     * Ends the current drag gesture.
     */
    public void endDrag() {
        dragging = false;

        // update the rotation matrix
        rotQuat.get(rotMat);
    }


    /**
     * Performs a drag movement on the virtual ball (mouse move with left button down)
     * @param x x coordinate (cursor location)
     * @param y y coordinate (cursor location)
     */
    public void drag(int x, int y) {

        // if we didn't start dragging we have nothing to do
        if (!dragging) {
            return;
        }

        // the new rotation (current delta)
        Quaternionf newRot = new Quaternionf();

        // Map the point to the sphere
        this.mapToSphere(x, y, endPoint);

        // Now we compute the vector perpendicular to the startPoint and endPoint vectors
        Vector3f perp = new Vector3f(startPoint).cross(endPoint);

        if (perp.length() > EPSILON) {
            // vector length > zero. We return the perpendicular vector as the transform

            newRot.x = perp.x;
            newRot.y = perp.y;
            newRot.z = perp.z;

            // In the quaternion values, w is cosine (theta / 2),
            // where theta is rotation angle
            newRot.w = new Vector3f(startPoint).dot(endPoint);

        } else {
            // the startPoint and endPoint vectors coincide.
            // that's why there's no rotation.
            newRot.identity();
        }

        // newRot contains the rotation from startPoint to endPoint
        // we apply the current rotation to our newRot and set the
        // final rotation quaternion
        rotQuat = newRot.mul(currentRot);
    }


    /**
     * Returns the rotation state.
     *
     * @return rotation state as transformation matrix (4x4)
     */
    public Matrix4f getRotation() {
        if (dragging) {
            // if we are currently dragging we set the matrix to the
            // rotation quaternion which contains the delta rotation
            // otherwise it contains just the previous rotation
            rotQuat.get(rotMat);
        }

        return rotMat;
    }

}