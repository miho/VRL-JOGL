package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import java.nio.Buffer;


/**
 * A simple wrapper around the GL vertex buffer API.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GLVertexBuffer implements GLObject {
    private final GL3 gl;
    private int size;

    private int[] bufferObj = new int[1];

    public GLVertexBuffer(GL3 gl) {
        this.gl = gl;
    }

    public void create() {
        gl.glGenBuffers(1, bufferObj, 0);
    }

    public int getGLHandle() {
        return bufferObj[0];
    }

    public void bind() {
        gl.glBindBuffer(gl.GL_ARRAY_BUFFER, getGLHandle());
    }

    public void release() {
        // unbind the VBO and VAO
        gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
        // gl.glBindVertexArray(0);
    }

    public void allocate(float[] data) {
        this.size = data.length;
        Buffer buffer = Buffers.newDirectFloatBuffer(data);
        int bufferByteSize = buffer.capacity() * Buffers.SIZEOF_FLOAT;
        gl.glBufferData(gl.GL_ARRAY_BUFFER, bufferByteSize, buffer, gl.GL_STATIC_DRAW);
    }

    /**
     * Returns the buffer size in bytes.
     * @return the buffer size in bytes
     */
    public int size() {
        return this.size*GLBuffers.SIZEOF_FLOAT;
    }

    /**
     * Returns the number of floats in this buffer.
     * @return the number of floats in this buffer
     */
    public int numElements() {
        return this.size;
    }
}
