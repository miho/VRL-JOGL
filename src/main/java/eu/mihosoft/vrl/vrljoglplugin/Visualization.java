/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.vrljoglplugin;

import eu.mihosoft.vrl.visual.Disposable;

import java.io.Serializable;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public interface Visualization extends Serializable {

    /**
     * Initializes this visualization.
     *
     * Custom GL visualizations should
     *
     * @param canvas
     */
    void init(JOGLCanvas3D canvas);
    void dispose(JOGLCanvas3D canvas);
}
