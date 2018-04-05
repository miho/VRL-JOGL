package edu.gcsc.vrl.vrljoglplugin.tmp;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import com.jogamp.opengl.GLEventListener;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import eu.mihosoft.vrl.vrljoglplugin.JOGLCanvas3D;
import eu.mihosoft.vrl.vrljoglplugin.Visualization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jogamp.opengl.GL.GL_FRONT_AND_BACK;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2.*; // GL2 constants

import javax.vecmath.Point3f;


/**
 * JOGL 2.0 Example 2: Rotating 3D Shapes (GLCanvas)
 */
public class STLVisualization implements Visualization, GLEventListener {
    private static final long serialVersionUID=1L;

    private static final int FPS = 20; // animator's target frames per second

    private final List<Point3f> vertices = new ArrayList<>();
    private transient FPSAnimator animator;

    // Setup OpenGL Graphics Renderer
    private transient GLU glu;  // for the GL Utility
    private float angleRot = 0;    // rotational angle in degree
    private final float rotSpeed = 0.5f; // rotational speed


    private void readSTL(File file) throws IOException {
        STLLoader loader = new STLLoader();

        vertices.clear();
        vertices.addAll(loader.parse(file));

        System.out.println("file: " + file + ", #verts: " + vertices.size());
    }

    // ------ Implement methods declared in GLEventListener ------
    /**
     * Called back immediately after the OpenGL context is initialized. Can be
     * used to perform one-time initialization. Run only once.
     *
     * @param drawable
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();      // get the OpenGL graphics context
        glu = new GLU();                         // get GL Utilities
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
        gl.glClearDepth(1.0f);      // set clear depth value to farthest
        gl.glEnable(GL_DEPTH_TEST); // enables depth testing
        gl.glDepthFunc(GL_LEQUAL);  // the type of depth test to do
        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // best perspective correction
        gl.glShadeModel(GL_SMOOTH); // blends colors nicely, and smoothes out lighting
    }

    /**
     * Call-back handler for window re-size event. Also called when the drawable
     * is first set to visible.
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();  // get the OpenGL 2 graphics context

        if (height == 0) {
            height = 1;   // prevent divide by zero
        }
        float aspect = (float) width / height;

        // Set the view port (display area) to cover the entire window
        gl.glViewport(0, 0, width, height);

        // Setup perspective projection, with aspect ratio matches viewport
        gl.glMatrixMode(GL_PROJECTION);  // choose projection matrix
        gl.glLoadIdentity();             // reset projection matrix
        glu.gluPerspective(45.0, aspect, 0.1, 100.0); // fovy, aspect, zNear, zFar

        // Enable the model-view transform
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity(); // reset
    }

    /**
     * Called back by the animator to perform rendering.
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();  // get the OpenGL 2 graphics context
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear color and depth buffers

        // ----- Render the Object -----
        gl.glLoadIdentity();                 // reset the model-view matrix
        gl.glTranslatef(0.0f, 0.0f, -6.0f); // translate left and into the screen
        gl.glRotatef(angleRot, -0.2f, 1.0f, 0.0f); // rotate about the y-axis

//        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
//        gl.glColor4f(1.0f, 1.0f, 1.0f, 0.3f);
//
//        gl.glEnable(GL_POLYGON_OFFSET_FILL );
//        gl.glPolygonOffset( 1f, 1f );
//
//        gl.glBegin(GL_TRIANGLES);
//
//        for (Point3f point3f : vertices) {
//            gl.glVertex3f(point3f.x, point3f.y, point3f.z);
//        }
//
//        // draw triangles
//        gl.glEnd(); // of the object


        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        gl.glColor3f(1.0f, 1.0f, 1.0f);

        gl.glBegin(GL_TRIANGLES);

        for (Point3f point3f : vertices) {
            gl.glVertex3f(point3f.x, point3f.y, point3f.z);
        }

        // draw triangles
        gl.glEnd(); // of the object

        // Update the rotational angle after each refresh.
        angleRot += rotSpeed;
    }

    /**
     * Called back before the OpenGL context is d
     */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        if(animator!=null) {
            animator.stop();
        }

        drawable.removeGLEventListener(this);
    }

    @Override
    public void init(JOGLCanvas3D canvas) {
        canvas.addGLEventListener(this);
        animator = new FPSAnimator(canvas, FPS, true);
        animator.start(); // start the animation loop
    }

    @Override
    public void dispose(JOGLCanvas3D canvas) {
        if(animator!=null) {
            animator.stop();
            try {
                animator.remove(canvas);
            } catch(Exception ex) {
                // if it fails, we already removed it
            }
        }

        canvas.removeGLEventListener(this);
    }

    public void setSTLFile(File stlFile) {
        try {
            readSTL(stlFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
