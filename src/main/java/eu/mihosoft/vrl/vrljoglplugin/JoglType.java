/*
 * Shape3DType.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009–2012 Steinbeis Forschungszentrum (STZ Ölbronn),
 * Copyright (c) 2006–2012 by Michael Hoffer
 *
 * This file is part of Visual Reflection Library (VRL).
 *
 * VRL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation.
 *
 * see: http://opensource.org/licenses/LGPL-3.0
 *      file://path/to/VRL/src/eu/mihosoft/vrl/resources/license/lgplv3.txt
 *
 * VRL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * This version of VRL includes copyright notice and attribution requirements.
 * According to the LGPL this information must be displayed even if you modify
 * the source code of VRL. Neither the VRL Canvas attribution icon nor any
 * copyright statement/attribution may be removed.
 *
 * Attribution Requirements:
 *
 * If you create derived work you must do three things regarding copyright
 * notice and author attribution.
 *
 * First, the following text must be displayed on the Canvas:
 * "based on VRL source code". In this case the VRL canvas icon must be removed.
 *
 * Second, the copyright notice must remain. It must be reproduced in any
 * program that uses VRL.
 *
 * Third, add an additional notice, stating that you modified VRL. In addition
 * you must cite the publications listed below. A suitable notice might read
 * "VRL source code modified by YourName 2012".
 *
 * Note, that these requirements are in full accordance with the LGPL v3
 * (see 7. Additional Terms, b).
 *
 * Publications:
 *
 * M. Hoffer, C.Poliwoda, G.Wittum. Visual Reflection Library -
 * A Framework for Declarative GUI Programming on the Java Platform.
 * Computing and Visualization in Science, 2011, in press.
 */

package eu.mihosoft.vrl.vrljoglplugin;

import com.jogamp.opengl.GLCapabilities;
import eu.mihosoft.vrl.annotation.TypeInfo;
import eu.mihosoft.vrl.reflection.CustomParamData;
import eu.mihosoft.vrl.reflection.RepresentationType;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.visual.VBoxLayout;
import eu.mihosoft.vrl.visual.VContainer;
import eu.mihosoft.vrl.visual.VGraphicsUtil;
import groovy.lang.Script;

import java.awt.Color;
import java.awt.Dimension;
import javax.media.j3d.*;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * TypeRepresentation for <code>javax.media.j3d.Shape3D</code>. The easiest
 * way to create 3D shapes is to use {@link eu.mihosoft.vrl.v3d.VTriangleArray}
 * or {@link eu.mihosoft.vrl.v3d.TxT2Geometry}. For simple scenarios, Instead
 * of directly using Java3D objects (Shape3D) it is suggested to use
 * {@link eu.mihosoft.vrl.v3d.VGeometry3D} objects and the corresponding type
 * representation.
 *
 * <p>Sample:</p>
 * <br/>
 * <img src="doc-files/shape3d-default-01.png"/>
 * <br/>
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 * @see eu.mihosoft.vrl.v3d.VTriangleArray
 * @see eu.mihosoft.vrl.types.VGeometry3DType
 */
@TypeInfo(type = Visualization.class, input = false, output = true, style = "default")
public class JoglType extends TypeRepresentationBase {

    private static final long serialVersionUID = 4412789368035724900L;
    private JOGLCanvas3D canvas;
    private VContainer container;
    public static final String ORIENTATION_KEY = "orientation";
    private Dimension previousVCanvas3DSize;
    protected Dimension minimumVCanvas3DSize;
    private GLCapabilities capabilities;

    /**
     * Constructor.
     */
    public JoglType() {
        init();

        if (!VGraphicsUtil.NO_3D) {

            init3DView(new JOGLCanvas3D(this, capabilities));

        } else {
            add(new JLabel("JOGL/Java3D support disabled!"));
        }
    }


    /**
     * Initializes this type representation.
     */
    protected void init() {
        setUpdateLayoutOnValueChange(false);
        VBoxLayout layout = new VBoxLayout(this, VBoxLayout.X_AXIS);
        setLayout(layout);

        nameLabel.setText("");
        nameLabel.setAlignmentY(0.5f);
        this.add(nameLabel);

        this.capabilities = new GLCapabilities(null);
        capabilities.setPBuffer(true); // important for macOS
    }

    /**
     * Initializes the 3D view of this type representation.
     *
     * @param canvas the 3D canvas
     */
    protected void init3DView(JOGLCanvas3D canvas) {
        dispose3D(); // very important to prevent memory leaks of derived classes!

        if (container != null) {
            this.remove(container);
        }

        this.canvas = canvas;

        setValueOptions("width=160;height=120;");

        minimumVCanvas3DSize = new Dimension(160, 120);

        container = new VContainer();
        container.add(canvas);

        this.add(container);
        this.setInputComponent(container);
    }

    @Override
    public void setValue(Object o) {
        System.out.println("SET: " + o);
        super.setValue(o);
    }

    @Override
    public void setViewValue(Object o) {
        System.out.println("VIEW: " + o);
        if(o!=null) {
            Visualization visualization = (Visualization) o;
            visualization.init(canvas);
            viewValue = o;
        }
    }

    @Override
    public void emptyView() {
        System.out.println("EMPTY: " + viewValue);
        if(viewValue!=null) {
            Visualization visualization = (Visualization) viewValue;
            visualization.dispose(canvas);
        }
    }

    /**
     * Defines the Vcanvas3D size by evaluating a groovy script.
     *
     * @param script the script to evaluate
     */
    private void setVCanvas3DSizeFromValueOptions(Script script) {
        Integer w = null;
        Integer h = null;
        Object property = null;

        if (getValueOptions() != null) {

            if (getValueOptions().contains("width")) {
                property = script.getProperty("width");
            }

            if (property != null) {
                w = (Integer) property;
            }

            property = null;

            if (getValueOptions().contains("height")) {
                property = script.getProperty("height");
            }

            if (property != null) {
                h = (Integer) property;
            }
        }

        if (w != null && h != null) {
            // TODO find out why offset is 10
            getCanvas().setPreferredSize(new Dimension(w - 10, h));
            getCanvas().setSize(new Dimension(w - 10, h));
        }

        System.out.println(getValueOptions());
    }

    /**
     * Defines render options by evaluating a groovy script.
     *
     * @param script the script to evaluate
     */
    private void setRenderOptionsFromValueOptions(Script script) {

        Object property = null;
        Boolean enableRenderOptimization = true;
        Boolean enableRealtimeOptimization = false;
        Float blurValue = 0.7f;

        if (getValueOptions() != null) {

            if (getValueOptions().contains("renderOptimization")) {
                property = script.getProperty("renderOptimization");
            }

            if (property != null) {
                enableRenderOptimization = (Boolean) property;
//                canvas.setRenderOptimizationEnabled(
//                        enableRenderOptimization);
            }

            property = null;

            if (getValueOptions().contains("realtimeOptimization")) {
                property = script.getProperty("realtimeOptimization");
            }

            if (property != null) {
                enableRealtimeOptimization = (Boolean) property;
//                canvas.setRealTimeRenderOptimization(
//                        enableRealtimeOptimization);
            }

            property = null;

            if (getValueOptions().contains("blurValue")) {
                property = script.getProperty("blurValue");
            }

            if (property != null) {
                blurValue = (Float) property;
//                canvas.setBlurValue(blurValue);
            }
        }
    }

    @Override
    protected void evaluationRequest(Script script) {
        setVCanvas3DSizeFromValueOptions(script);
        setRenderOptionsFromValueOptions(script);
    }

    //    @Override
//    public void evaluateValueOptions() {
//        setVCanvas3DSizeFromValueOptions();
//        setRenderOptionsFromValueOptions();
//    }
    @Override
    public CustomParamData getCustomData() {

        if (VGraphicsUtil.NO_3D) {
            return new CustomParamData();
        }

        CustomParamData result = super.getCustomData();

        Transform3D t3d = new Transform3D();
        double[] values = new double[16];
        t3d.get(values);

        result.put(ORIENTATION_KEY, values);

        return result;
    }

    @Override
    public void evaluateCustomParamData() {
        Transform3D t3d = new Transform3D();
        double[] values = (double[]) super.getCustomData().get(ORIENTATION_KEY);
        if (values != null) {
            t3d.set(values);
//            getUniverseCreator().getRootGroup().setTransform(t3d);
        }
    }

    /**
     * Returns the canvas used for rendering
     *
     * @return the canvas used for rendering
     */
    public JOGLCanvas3D getCanvas() {
        return canvas;
    }

//    /**
//     * Returns the canvas used for offscreen rendering
//     * @return the canvas used for offscreen rendering
//     */
//    public VOffscreenCanvas3D getOffscreenCanvas() {
//        return universeCreator.getOffscreenCanvas();
//    }


    /**
     * Disposes 3D resources.
     */
    private void dispose3D() {
        if (!VGraphicsUtil.NO_3D) {
            emptyView();
            value = null;

            if (canvas != null) {
                canvas.destroy();
            }
            canvas = null;
        }
    }

    @Override
    public void dispose() {
        dispose3D();
        super.dispose();
    }

    @Override
    public void enterFullScreenMode(Dimension size) {
        super.enterFullScreenMode(size);
        previousVCanvas3DSize = canvas.getSize();
        container.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        container.setMinimumSize(null);
        container.setMaximumSize(null);

        canvas.setPreferredSize(null);
        canvas.setMinimumSize(minimumVCanvas3DSize);
        canvas.setMaximumSize(null);

        revalidate();
    }

    @Override
    public void leaveFullScreenMode() {
        super.leaveFullScreenMode();
        container.setPreferredSize(null);
        container.setMinimumSize(null);
        container.setMaximumSize(null);

        canvas.setSize(previousVCanvas3DSize);
        canvas.setPreferredSize(previousVCanvas3DSize);
        canvas.setMinimumSize(minimumVCanvas3DSize);

        revalidate();
    }

    @Override
    public JComponent customViewComponent() {

//        final BufferedImage img = plotPane.getImage();
//
//        JPanel panel = new JPanel() {
//            @Override
//            public void paintComponent(Graphics g) {
//                g.drawImage(img, 0, 0, 640, 480,  null);
//            }
//        };
//
//        return panel;
        return null;
    }

    @Override
    public boolean noSerialization() {
        // we cannot serialize shape3d objects
        return true;
    }
}
