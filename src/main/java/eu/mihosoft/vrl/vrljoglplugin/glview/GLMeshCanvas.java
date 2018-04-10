package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.FPSAnimator;
import eu.mihosoft.vrl.visual.VSwingUtil;
import eu.mihosoft.vrl.vrljoglplugin.JOGLCanvas3D;
import eu.mihosoft.vrl.vrljoglplugin.Visualization;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple GL canvas.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GLMeshCanvas implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, Visualization {

    // jogl drawable
    private GLAutoDrawable drawable;
    // jogl gl object (access to gl API)
    private GL3 gl;

    // shader program
    private Program program;

    // gl mesh to display
    private GLMesh glMesh;

    // mesh
    private Mesh mesh;

    // scalable surface (for HiDPI aware mouse control/rendering)
    private ScalableSurface scalableSurface;

    private int width;
    private int height;

    private Vector3f center = new Vector3f();

    // object scale (to fit it in unit cube)
    private float scale;

    // zoom factor (controlled via mouse wheel)
    private float zoom;

    // perspective value (usually 0.25 for perspective and 0.0 for orthographic)
    private float perspective;

    // render scale (important for HiDPI displays)
    private float renderScaleX = 1.f;
    // render scale (important for HiDPI displays)
    private float renderScaleY = 1.f;

    // mouse position
    private Point mousePos = new Point();

    // arcball for rotating the geometry
    private ArcBall arcBall = new ArcBall(100, 100);

    // buffer for the transformation matrix (for GPU shaders)
    private FloatBuffer transformMatrixBuffer = Buffers.newDirectFloatBuffer(16);
    // buffer for the view matrix (for GPU shaders)
    private FloatBuffer viewMatrixBuffer = Buffers.newDirectFloatBuffer(16);

    private FPSAnimator animator;

    // indicates whether zoom should be reset to 1.0 on
    // center animation
    private boolean resetZoomOnCenterAnim;

    // perspective animation properties
    private Float fromPerspective;
    private Float toPerspective;
    private float deltaPerspective;

    // scale animation properties
    private Float fromScale;
    private Float toScale;
    private float deltaScale;

    // center animation properties
    private Vector3f fromCenter;
    private Vector3f toCenter;
    private Vector3f deltaCenter;

    // zoom animation properties
    private Float fromZoom;
    private Float toZoom;
    private float deltaZoom;
    private boolean animationEnabled;
    private boolean skipInitAnimation;

    private boolean zoomToInner = true;

    public GLMeshCanvas(Mesh mesh) {
        this.mesh = mesh;
        setAnimationEnabled(true);
    }

    /**
     * Defines whether to skip initial scale animation (might need too many resources if many
     * visualizations are used at the same time).
     *
     * @param state state to set
     */
    public void setSkipInitAnimation(boolean state) {
        this.skipInitAnimation = state;
    }

    /**
     * Indicates whether the initial scale animation shall be skipped.
     *
     * @return {@code true} if skipped; {@code false} otherwise
     */
    public boolean isSkipInitAnimation() {
        return skipInitAnimation;
    }

    /**
     * Defines whether to enable animations.
     *
     * @param state state to set
     */
    public final void setAnimationEnabled(boolean state) {
        this.animationEnabled = state;
    }

    /**
     * Indicates whether animations are enabled.
     *
     * @return {@code true} if enabled; {@code false} otherwise
     */
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    private void perspectiveAnim(float v) {
        if (isAnimationEnabled()) {
            if (!animator.isAnimating()) {
                animator.start();
            }
            fromPerspective = perspective;
            toPerspective = v;
            deltaPerspective = (toPerspective - fromPerspective) / 15f;
        } else {
            perspective = v;
        }
    }

    private void scaleAnim(float initV, float v) {
        if (isAnimationEnabled()) {
            System.out.println("animating: " + isAnimationEnabled());
            if (!animator.isAnimating()) {
                animator.start();
            }
            fromScale = initV;
            scale = initV;
            toScale = v;
            deltaScale = (toScale - fromScale) / 15f;
        } else {
            System.out.println("not animating");
            scale = v;
        }
    }

    private void zoomAnim(float initV, float v) {
        if (isAnimationEnabled()) {
            if (!animator.isAnimating()) {
                animator.start();
            }
            fromZoom = initV;
            zoom = initV;
            toZoom = v;
            deltaZoom = (toZoom - fromZoom) / 20f;
        } else {
            zoom = v;
        }
    }

    private void centerAnim(Vector3f fromCenter, Vector3f toCenter) {
        if (isAnimationEnabled()) {
            if (!animator.isAnimating()) {
                animator.start();
            }
            this.fromCenter = fromCenter;
            this.center = fromCenter;
            this.toCenter = toCenter;

            deltaCenter = new Vector3f(toCenter).sub(fromCenter).div(15f);
        } else {
            this.center = toCenter;
        }
    }

    public void setOrthographicView() {
        perspectiveAnim(0);
    }

    public void setPerspectiveView() {
        perspectiveAnim(0.25f);
    }

    /**
     * Indicates whether zooming to inner is allowed.
     * @return {@code true} if allowed; {@code false} otherwise
     */
    public boolean isZoomToInner() {
        return zoomToInner;
    }

    /**
     * Defines whether zoom to inner is allowed.
     * @param zoomToInner value to set
     */
    public void setZoomToInner(boolean zoomToInner) {
        this.zoomToInner = zoomToInner;
    }

    private void displayMesh(Mesh m) {

        this.mesh = m;

        Vector3f lower = new Vector3f(m.xmin(), m.ymin(), m.zmin());
        Vector3f upper = new Vector3f(m.xmax(), m.ymax(), m.zmax());

        center = new Vector3f(lower).add(upper).mul(0.5f);

        scale = 2.f / (new Vector3f(upper).sub(lower).length());

        // reset other camera/transform/rotate parameters
        zoom = 1f;
        arcBall.reset();

        // rescale glMesh vertices and center them at (0,0,0)
        for (int i = 0; i < m.vertices.length; i++) {
            m.vertices[i] = (m.vertices[i] - center.get(i % 3)) * scale;
        }

        // since we rescaled and centered them, scale will be 1.0 and center is the origin (0,0,0)
        center = new Vector3f();
        scale = 1.0f;

        // create the gl glMesh for rendering
        glMesh = new GLMesh(gl, m);
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        System.out.println("-> [VRL-JOGL]: gl init");

        this.drawable = drawable;
        this.gl = drawable.getGL().getGL3();

        program = new Program(gl);

        List<Shader> shaders = new ArrayList<>();
        shaders.add(Shader.newShaderFromResource(gl,
                "/eu/mihosoft/vrl/vrljoglplugin/glview/mesh-vert.shader", Shader.Type.VERTEX));
        shaders.add(Shader.newShaderFromResource(gl,
                "/eu/mihosoft/vrl/vrljoglplugin/glview/mesh-frag.shader", Shader.Type.FRAGMENT));

        program.attachShaders(shaders);
        program.link();
        program.deleteShaders();

        displayMesh(mesh);

        arcBall.setBounds(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        updateRenderScale();

        animator = new FPSAnimator(drawable, 60);

        perspective = 0.25f;
        if (!isSkipInitAnimation()) {
            scaleAnim(0.01f, 1.0f);
        }

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        System.out.println("-> [VRL-JOGL]: gl dispose");

        program.delete();
        scalableSurface = null;

        try {
            animator.remove(drawable);
        } catch (Exception ex) {
            // animator already removed
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(gl.GL_DEPTH_TEST);

        animateFrame();

        drawMesh(gl);
    }

    private void animateFrame() {
        if (fromPerspective != null) {
            perspective += deltaPerspective;

            if (deltaPerspective > 0 && perspective > toPerspective
                    || deltaPerspective <= 0 && perspective <= toPerspective) {
                perspective = toPerspective;
                fromPerspective = null;
                toPerspective = null;
            }
        }

        if (fromScale != null) {
            scale += deltaScale;
            if (deltaScale > 0 && scale > toScale || deltaScale <= 0 && scale <= toScale) {
                scale = toScale;
                fromScale = null;
                toScale = null;
            }
        }

        if (fromCenter != null) {
            center.add(deltaCenter);
            if (new Vector3f(toCenter).sub(center).length() < 1e-3) {
                center.set(toCenter);
                fromCenter = null;
                toCenter = null;

                if (resetZoomOnCenterAnim) {
                    zoomAnim(zoom, 1.f);
                }
            }
        }

        if (fromZoom != null) {
            zoom += deltaZoom;
            if (deltaZoom > 0 && zoom > toZoom || deltaZoom <= 0 && zoom <= toZoom) {
                zoom = toZoom;
                fromZoom = null;
                toZoom = null;
            }
        }

        if (animator != null && !isAnimating() && animator.isAnimating()) {
            animator.stop();
        }
    }

    private boolean isAnimating() {
        return fromScale != null
                || fromPerspective != null
                || fromCenter != null
                || fromZoom != null;
    }

    void drawMesh(GL3 gl) {
        program.startUsing();

        // write matrices to buffer
        transformMatrix().get(transformMatrixBuffer);
        viewMatrix().get(viewMatrixBuffer);

        // Load the transform and view matrices into the shader
        gl.glUniformMatrix4fv(
                program.getUniformLocation("transform_matrix"),
                1, false, transformMatrixBuffer);

        gl.glUniformMatrix4fv(
                program.getUniformLocation("view_matrix"),
                1, false, viewMatrixBuffer);

        // Compensate for color changes during zoom (geometry looks flattened)
        gl.glUniform1f(program.getUniformLocation("zoomInverse"), 1.f / zoom);

        // Find and enable the attribute location for vertex position
        int vertexPosition = program.getAttributeLocation("vertex_position");
        gl.glEnableVertexAttribArray(vertexPosition);

        if (glMesh != null) {
            // Then draw the glMesh with that vertex position
            glMesh.draw(vertexPosition);
        }

        // Clean up state machine
        gl.glDisableVertexAttribArray(vertexPosition);
        program.stopUsing();
    }

    Matrix4f transformMatrix() {

        Matrix4f mat = new Matrix4f().identity();

        // move to (0,0,0)
        mat.translate(new Vector3f(center).negate());

        mat.mul(arcBall.getRotation());

        // scale
        mat.scale(-scale, scale, -scale);

        return mat;
    }

    Matrix4f viewMatrix() {

        Matrix4f m = new Matrix4f().identity();

        if (getWidth() > getHeight()) {
            m.scale(-getHeight() / (float) getWidth(), 1, 0.5f);
        } else {
            m.scale(-1, getWidth() / (float) getHeight(), 0.5f);
        }

        m.scale(zoom, zoom, 1f);

        // prevent far distortion
        // restricting to 2.0f prevents near clipping (zooming inside is then not possible)
        float zoomCompensation = isZoomToInner()?zoom: Math.min(2.0f, zoom);

        m._m23(perspective * zoomCompensation);

        return m;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 2) {
            centerAnim(center, new Vector3f());
            arcBall.reset();
            arcBall.setBounds(getWidth(), getHeight());
            resetZoomOnCenterAnim = true;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
            mousePos = e.getPoint();

            // scale mouse coordinates according to render scale
            // (important for retina, index.e., hiDPI)
            mousePos.x *= renderScaleX;
            mousePos.y *= renderScaleY;

            arcBall.beginDrag(mousePos.x, mousePos.y);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
            arcBall.endDrag();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

        Point p = e.getPoint();

        // scale mouse coordinates according to render scale
        // (important for retina, index.e., hiDPI)
        p.x *= renderScaleX;
        p.y *= renderScaleY;

        Point d = new Point(p.x - mousePos.x, p.y - mousePos.y);

        if (SwingUtilities.isLeftMouseButton(e)) {

            arcBall.drag(p.x, p.y);

            updateDisplay();
        } else if (SwingUtilities.isRightMouseButton(e)) {

            Vector3f v = new Vector3f(
                    d.x / (0.5f * getWidth()),
                    d.y / (0.5f * getHeight()),
                    0.f
            );

            center.add(v.mul(1.f / (zoom * 0.8f /* influences drag speed*/)));

            updateDisplay();
        }

        mousePos = p;
    }

    public void updateDisplay() {
        if(drawable instanceof JComponent) {
            VSwingUtil.repaintRequest((JComponent) drawable);
        } else {
            drawable.display();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        // we do nothing if zoom is currently animated
        if (fromZoom != null) {
            return;
        }

        float delta = (float) e.getPreciseWheelRotation();
        // this zoom update works great on Apple trackpads
        // more testing needed on other machines

        zoom -= zoom * delta * 0.005 /*base zoom*/
                + Math.signum(delta) * Math.min(0.007 / (zoom * zoom), 0.01) /*accelerate if far away*/;

        if (zoom < 0.01f) {
            zoom = 0.01f;
        } else if (zoom > 100f) {
            zoom = 100f;
        }

        updateDisplay();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        gl.glViewport(0, 0, width, height);

        this.width = width;
        this.height = height;

        arcBall.setBounds(width, height);

        updateRenderScale();
    }

    private void updateRenderScale() {
        if (scalableSurface != null) {
            float[] renderScale = new float[2];
            scalableSurface.getCurrentSurfaceScale(renderScale);

            if (Float.compare(renderScale[0], renderScaleX) != 0
                    && Float.compare(renderScale[1], renderScaleY) != 0) {
                System.out.println("-> [VRL-JOGL]: render scale changed:");
                System.out.println(" old scale = (" + renderScaleX + "," + renderScaleY + ")");
                System.out.println(" new scale = (" + renderScale[0] + "," + renderScale[1] + ")");

                renderScaleX = renderScale[0];
                renderScaleY = renderScale[1];
            }
        }
    }

    public void setScalableSurface(ScalableSurface scalableSurface) {
        this.scalableSurface = scalableSurface;
        updateRenderScale();

        System.out.println("-> [VRL-JOGL]: render scale:");
        System.out.println(" scale = (" + renderScaleX + "," + renderScaleY + ")");
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setRenderScale(float sx, float sy) {
        this.renderScaleX = sx;
        this.renderScaleY = sy;
    }

    @Override
    public void init(JOGLCanvas3D canvas) {
        System.out.println("-> [VRL-JOGL]: visualization init");
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);

        setScalableSurface(canvas);
    }

    @Override
    public void dispose(JOGLCanvas3D canvas) {
        System.out.println("-> [VRL-JOGL]: visualization dispose");
        canvas.removeGLEventListener(this);
        canvas.removeMouseListener(this);
        canvas.removeMouseMotionListener(this);
        canvas.removeMouseWheelListener(this);

        setScalableSurface(null);

        try {
            animator.remove(drawable);
        } catch (Exception ex) {
            // animator already removed
        }
    }
}



// TODO 09.04.2018
// We would prefer to set the view matrix according to gluPerspective()
//
//    Matrix4f transformMatrix() {
//
//        Matrix4f mat = new Matrix4f().identity();
//
//        //mat._m33(0);
//
//        // move to (0,0,0)
//
//        mat.translate(0,0,1);
//
//        mat.translate(new Vector3f(center).negate().mul(scale * zoom, scale * zoom, scale * zoom));
//
//        // scale
//        mat.scale(scale * zoom, scale * zoom, scale * zoom);
//
//        mat.mul(arcBall.getRotation());
//
//
//        return mat;
//    }
//
//    Matrix4f viewMatrix() {
//
//        Matrix4f m = new Matrix4f().identity();
//
//        // gluPerspective()
//        // f = cotangent(fovy/2), fovy is the view angle in degrees
//        float aspect = getWidth()/getHeight();
//
//        // zFar/zNear not to large because of z buffer resolution
//        float zNear = 0.00001f;
//        float zFar = 10f;
//
//        float f = 1.7305f;
//
//
//        m._m00(-f/aspect);
//        m._m11(f);
//        m._m22(-(zFar+zNear)/(zNear-zFar));
//        m._m32((2*zFar*zNear)/(zNear-zFar));
//        m._m23(1);
//
//
////        if (getWidth() > getHeight()) {
////            m.scale(-getHeight() / (float) getWidth(), 1, 0.5f);
////        } else {
////            m.scale(-1, getWidth() / (float) getHeight(), 0.5f);
////        }
//
//        //m.scale(zoom, zoom, 1f);
//
//        // m._m23(perspective);
//
//        return m;
//    }
