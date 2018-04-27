package eu.mihosoft.vrl.vrljoglplugin.glview;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();

        // init GL capabilities
        GLProfile glp = GLProfile.get(GLProfile.GL3);
        GLCapabilities capabilities = new GLCapabilities(glp);
        capabilities.setPBuffer(true);
        capabilities.setFBO(true);
//        capabilities.setAlphaBits(8);
//        capabilities.setRedBits(8);
//        capabilities.setGreenBits(8);
//        capabilities.setBlueBits(8);
        capabilities.setDoubleBuffered(true);

        // create jogl panel
        GLJPanel panel = new GLJPanel(capabilities);
        panel.setFocusable(true);

        Mesh mesh = null;

        try {
            mesh = new STLLoader().loadMesh(
                    new File("/Users/miho/tmp/jogltest.stl")
            );
            mesh.colors = new float[]{
                    1,0,0,1,
                    0,1,0,1,
                    0,0,1,1,
                    1,1,1,1,
                    0,1,1,1,

            };
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        GLMeshCanvas canvas = new GLMeshCanvas(mesh);
        canvas.setScalableSurface(panel);

        panel.addGLEventListener(canvas);
        panel.addMouseListener(canvas);
        panel.addMouseMotionListener(canvas);
        panel.addMouseWheelListener(canvas);

        // main frame setup
        window.getContentPane().add(panel);
        window.setSize(400, 400);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
