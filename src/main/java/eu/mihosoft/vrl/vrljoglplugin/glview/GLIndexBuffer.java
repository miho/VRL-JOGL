package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import java.nio.Buffer;

/**
 * A simple wrapper around the GL element index buffer API.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GLIndexBuffer implements GLObject {
    private final GL3 gl;
    private int size;

    private int[] bufferObj = new int[1];

    public GLIndexBuffer(GL3 gl) {
        this.gl = gl;
    }

    public void create() {
        gl.glGenBuffers(1, bufferObj, 0);
    }

    @Override
    public int getGLHandle() {
        return bufferObj[0];
    }

    public void bind() {
        gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, getGLHandle());
    }

    public void release() {
        // unbind the VBO and VAO
        gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void allocate(int[] data) {
        this.size = data.length;
        Buffer buffer = Buffers.newDirectIntBuffer(data);
        int bufferByteSize = buffer.capacity() * Buffers.SIZEOF_INT;
        gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, bufferByteSize, buffer, gl.GL_STATIC_DRAW);

    }

    /**
     * Returns the buffer size in bytes.
     * @return the buffer size in bytes
     */
    public int size() {
        return this.size*GLBuffers.SIZEOF_INT;
    }

    /**
     * Returns the number of ints in this buffer.
     * @return the number of ints in this buffer
     */
    public int numElements() {
        return this.size;
    }
}
