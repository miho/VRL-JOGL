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

    private Mesh mesh;


    /**
     * Constructor. Creates a GLMesh object.
     *
     * @param gl   gl object
     * @param mesh triangle glMesh to render
     */
    GLMesh(GL3 gl, Mesh mesh) {
        this.gl = gl;
        this.mesh = mesh;

        vertices = new GLVertexBuffer(gl);
        indices = new GLIndexBuffer(gl);

        vertices.create();
        vertices.bind();

        if (mesh.colors != null && mesh.colors.length > 0) {

            float[] posAndColor = new float[mesh.vertices.length + mesh.colors.length];

            for (int i = 0; i < mesh.vertices.length/3; i++) {

                int idx = i * 7;

                // vertex position
                posAndColor[idx + 0] = mesh.vertices[i * 3 + 0];
                posAndColor[idx + 1] = mesh.vertices[i * 3 + 1];
                posAndColor[idx + 2] = mesh.vertices[i * 3 + 2];

                // vertex color
                posAndColor[idx + 3] = mesh.colors[i*4 + 0];
                posAndColor[idx + 4] = mesh.colors[i*4 + 1];
                posAndColor[idx + 5] = mesh.colors[i*4 + 2];
                posAndColor[idx + 6] = mesh.colors[i*4 + 3];
            }

            //System.arraycopy(mesh.vertices, 0, posAndColor,0, mesh.vertices.length);
            //System.arraycopy(mesh.colors, 0, posAndColor,mesh.vertices.length, mesh.colors.length);


            vertices.allocate(posAndColor);

        } else {

            float[] posAndColor = new float[mesh.vertices.length + mesh.vertices.length/3*4];

            int counter = 0;

            for (int i = 0; i < mesh.vertices.length / 3; i++) {

                int idx = counter + i * 3;

                // vertex position
                posAndColor[idx + 0] = mesh.vertices[i * 3 + 0];
                posAndColor[idx + 1] = mesh.vertices[i * 3 + 1];
                posAndColor[idx + 2] = mesh.vertices[i * 3 + 2];

                // vertex color
                posAndColor[idx + 3] = mesh.globalColor[0];
                posAndColor[idx + 4] = mesh.globalColor[1];
                posAndColor[idx + 5] = mesh.globalColor[2];
                posAndColor[idx + 6] = mesh.globalColor[3];

                counter += 4; // r,g,b,a
            }

            //System.arraycopy(mesh.vertices, 0, posAndColor,0, mesh.vertices.length);
            //System.arraycopy(mesh.colors, 0, posAndColor,mesh.vertices.length, mesh.colors.length);

            vertices.allocate(posAndColor);
        }

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

    /**
     * Draws this glMesh.
     *
     * @param vertexPosition vertex position handle
     */
    void draw(int vertexPosition, int colorPosition) {
        vertices.bind();
        indices.bind();


        // first we store the vertex coordinates
        gl.glVertexAttribPointer(vertexPosition, 3/*(x,y,z)*/, GL_FLOAT, false,
                7 * GLBuffers.SIZEOF_FLOAT, 0);

        // colors are added after the vertex coordinates
        gl.glVertexAttribPointer(colorPosition, 4/*(r,g,b,a)*/, GL_FLOAT, false,
                7 * GLBuffers.SIZEOF_FLOAT, 3 * GLBuffers.SIZEOF_FLOAT);


        gl.glDrawElements(GL_TRIANGLES, indices.numElements(),
                GL_UNSIGNED_INT, 0);

        vertices.release();
        indices.release();
    }

    public boolean isColorDataPresent() {
        return mesh.colors != null && mesh.colors.length > 0;
    }
}