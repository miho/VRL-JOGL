/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.vrljoglplugin;


import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;

import java.io.File;
import java.io.Serializable;

/**
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
@ComponentInfo(name = "SampleJoglComponent", category = "JOGL")
public class SampleJoglComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    private STLVisualization visualization;

    public Visualization view(@ParamInfo(
            style="load-dialog",
            options="endings=[\".stl\"]; description=\"*.stl - Files\"") File stlFile) {

        visualization =  new STLVisualization();
        visualization.setSTLFile(stlFile);

        return visualization;
    }

    public Visualization view() {

        return visualization;
    }
}





