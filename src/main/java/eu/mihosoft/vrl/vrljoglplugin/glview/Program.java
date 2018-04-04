package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.opengl.GL3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple wrapper around the GL shader program API.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Program implements GLObject {

    private final GL3 gl;
    private int programHandle;

    private final List<Shader> shaders = new ArrayList<>();

    /**
     * Creates a program object.
     * @param gl gl API object
     */
    public Program(GL3 gl) {
        this.gl = gl;

        this.programHandle = gl.glCreateProgram();

        if (this.programHandle == 0) {
            throw new RuntimeException("Cannot create GL program");
        }
    }

    /**
     * Attaches the specified shaders to this program.
     * @param shaders shaders to attach
     */
    public void attachShaders(List<Shader> shaders) {

        if(programHandle == 0) {
            throw new RuntimeException("Program not created!");
        }

        if (shaders.isEmpty()) {
            throw new RuntimeException("No shaders provided. Cannot create program without shaders");
        }

        this.shaders.addAll(shaders);

        for (Shader s : shaders) {
            gl.glAttachShader(this.programHandle, s.getGLHandle());
        }
    }

    /**
     * Detaches and deletes all shaders currently attached to this program.
     */
    public void deleteShaders() {
        // detach all the shaders
        for (Iterator<Shader> it = shaders.iterator(); it.hasNext();) {
            Shader s = it.next();
            gl.glDetachShader(this.programHandle, s.getGLHandle());
            gl.glDeleteShader(s.getGLHandle());
            it.remove();
        }
    }

    /**
     * Links this program.
     */
    public void link() {

        if(programHandle == 0) {
            throw new RuntimeException("Program not created!");
        }

        gl.glLinkProgram(this.programHandle);

        String linkageInfo = getProgramInfoLog(gl, this.programHandle);

        //throw exception if linking failed
        int[] linkageStatus = new int[1];
        gl.glGetProgramiv(this.programHandle, gl.GL_LINK_STATUS, linkageStatus, 0);

        if (linkageStatus[0] == gl.GL_FALSE) {
            throw new RuntimeException(linkageInfo);
        }

        gl.glValidateProgram(this.programHandle);

        String validationInfo = getProgramInfoLog(gl, this.programHandle);

        //throw exception if validation failed
        int[] validationStatus = new int[1];
        gl.glGetProgramiv(this.programHandle, gl.GL_LINK_STATUS, validationStatus, 0);

        if (validationStatus[0] == gl.GL_FALSE) {
            throw new RuntimeException(validationInfo);
        }
    }

    /**
     * Indicates whether this program is valid.
     *
     * @return {@code true} if this program is valid; {@code false} otherwise
     */
    public boolean isValid() {
        return programHandle != 0;
    }

    /**
     * Deletes this program.
     */
    public void delete() {
        if (isValid()) {
            gl.glDeleteProgram(getGLHandle());
        }
    }

    /**
     * Starts using this program.
     */
    public void startUsing() {
        gl.glUseProgram(getGLHandle());
    }

    /**
     * Indicates whether this program is currently in use.
     * @return {@code true} if this program is currently in use; {@code false} otherwise
     */
    public boolean isInUse() {
        int[] currentProgram = new int[1];
        gl.glGetIntegerv(gl.GL_CURRENT_PROGRAM, currentProgram, 0);
        return currentProgram[0] == getGLHandle();
    }

    /**
     * Stops using this program.
     */
    public void stopUsing() {
        gl.glUseProgram(0);
    }

    @Override
    public int getGLHandle() {
        return programHandle;
    }

    /**
     * Returns the location of the specified attribute.
     * @param attribName name of the attribute
     * @return GL handle to the specified attribute
     */
    public int getAttributeLocation(final String attribName) {
        if (attribName == null || attribName.isEmpty())
            throw new RuntimeException("attribName was empty or null");

        int attrib = gl.glGetAttribLocation(getGLHandle(), attribName);

        if (attrib == -1) {
            throw new RuntimeException("Program attribute not found: " + attribName);
        }

        // System.out.println("Attrib '" + attribName + "' is " + attrib);

        return attrib;
    }

    /**
     * Returns the location of the specified uniform.
     * @param uniformName name of the uniform
     * @return GL handle to the specified attribute
     */
    int getUniformLocation(final String uniformName) {
        if (uniformName == null || uniformName.isEmpty())
            throw new RuntimeException("uniformName was empty or null");

        int uniform = gl.glGetUniformLocation(getGLHandle(), uniformName);
        if (uniform == -1)
            throw new RuntimeException("Program getUniformLocation not found: " + uniformName);

        return uniform;
    }


    private static String getProgramInfoLog(final GL3 gl, final int programObj) {

        final int[] infoLogLength = new int[1];
        gl.glGetProgramiv(programObj, gl.GL_INFO_LOG_LENGTH, infoLogLength, 0);

        if (infoLogLength[0] == 0) {
            return "valid";
        }

        final int[] charsWritten = new int[1];
        final byte[] infoLogBytes = new byte[infoLogLength[0]];
        gl.glGetProgramInfoLog(programObj, infoLogLength[0], charsWritten, 0, infoLogBytes, 0);

        return new String(infoLogBytes, 0, charsWritten[0]);
    }
}
