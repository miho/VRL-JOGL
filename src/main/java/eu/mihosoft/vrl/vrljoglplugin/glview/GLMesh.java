package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import static com.jogamp.opengl.GL.*;

/**
 * A class for rendering triangle meshes.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GLMesh {

    private GL3 gl;

    private GLVertexBuffer vertices;
    private GLIndexBuffer indices;


    /**
     * Constructor. Creates a GLMesh object.
     * @param gl gl object
     * @param mesh triangle glMesh to render
     */
    GLMesh(GL3 gl, Mesh mesh) {
        this.gl = gl;

        vertices = new GLVertexBuffer(gl);
        indices = new GLIndexBuffer(gl);

        vertices.create();
        vertices.bind();
        vertices.allocate(mesh.vertices);
        vertices.release();

        indices.create();
        indices.bind();
        indices.allocate(mesh.indices);
        indices.release();
    }

    /**
     * Draws this glMesh.
     *
     * @param vertexPosition vertex position handle
     */
    void draw(int vertexPosition) {
        vertices.bind();
        indices.bind();

        gl.glVertexAttribPointer(vertexPosition, 3, GL_FLOAT, false,
                3 * GLBuffers.SIZEOF_FLOAT, 0);

        gl.glDrawElements(GL_TRIANGLES, indices.numElements(),
                GL_UNSIGNED_INT, 0);

        vertices.release();
        indices.release();
    }
}