/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.vrljoglplugin;

import java.io.Serializable;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public interface Visualization extends Serializable{

    public void initRendering(JOGLCanvas3D canvas);
}
