package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.opengl.GL3;

import java.util.Scanner;

/**
 * A simple wrapper the GL shader API.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Shader implements GLObject {

    private String code;
    private int shaderHandle;
    private Type type;

    private final GL3 gl;

    /**
     * Creates a new shader object.
     * @param gl gl object
     * @param code shader code
     * @param type shader type
     */
    private Shader(final GL3 gl, final String code, final Type type) {
        this.gl = gl;
        this.shaderHandle = gl.glCreateShader(type.getGlShaderType());
        this.code = code;
        this.type = type;

        gl.glShaderSource(this.shaderHandle, 1, new String[]{this.code}, (int[]) null, 0);
        gl.glCompileShader(this.shaderHandle);

        String info = getShaderInfoLog(gl, this.shaderHandle);

        if (!isShaderValid(gl, this.shaderHandle)) {
            gl.glDeleteShader(this.shaderHandle);
            this.shaderHandle = 0;
            throw new RuntimeException(info);
        }
    }

    /**
     * Returns the GL handle to this shader.
     * @return handle to this shader
     */
    @Override
    public int getGLHandle() {
        return shaderHandle;
    }

    /**
     * Returns the shader code.
     *
     * @return shader code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the shader type.
     * @return shader type
     */
    public Type getType() {
        return type;
    }

    /**
     * Indicates whether this shader is valid.
     * @return {@code true} if this shader is valid; {@code false} otherwise
     */
    public boolean isValid() {
        return shaderHandle != 0;
    }

    /**
     * Creates a new shader from String.
     * @param gl gl object
     * @param code shader code
     * @param type shader type
     * @return shader type
     */
    public static Shader newShaderFromString(GL3 gl, String code, Type type) {
        Shader result = new Shader(gl, code, type);

        return result;
    }

    /**
     * Creates a new shader from resource.
     * @param gl gl object
     * @param resource shader code
     * @param type shader type
     * @return shader type
     */
    public static Shader newShaderFromResource(GL3 gl, String resource, Type type) {
        String code = new Scanner(Shader.class.getResourceAsStream(resource), "UTF-8").
                useDelimiter("\\A").next();
        Shader result = new Shader(gl, code, type);

        return result;
    }

    /**
     * Shader type. Currently, only vertex shaders and fragment shaders are supported.
     */
    public enum Type {

        FRAGMENT(GL3.GL_FRAGMENT_SHADER),
        VERTEX(GL3.GL_VERTEX_SHADER);

        private int glShaderType;

        Type(int value) {
            this.glShaderType = value;
        }

        public int getGlShaderType() {
            return glShaderType;
        }
    }

    private static boolean isShaderValid(final GL3 gl, final int shaderObj) {
        int[] result = new int[1];
        gl.glGetShaderiv(shaderObj, gl.GL_COMPILE_STATUS, result, 0);

        return result[0] == gl.GL_TRUE;
    }

    private static String getShaderInfoLog(final GL3 gl, final int shaderObj) {

        final int[] infoLogLength = new int[1];
        gl.glGetShaderiv(shaderObj, gl.GL_INFO_LOG_LENGTH, infoLogLength, 0);

        if (infoLogLength[0] == 0) {
            return "valid";
        }
        final int[] charsWritten = new int[1];
        final byte[] infoLogBytes = new byte[infoLogLength[0]];
        gl.glGetShaderInfoLog(shaderObj, infoLogLength[0], charsWritten, 0, infoLogBytes, 0);

        return new String(infoLogBytes, 0, charsWritten[0]);
    }
}
