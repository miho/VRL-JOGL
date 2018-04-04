package eu.mihosoft.vrl.vrljoglplugin;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import eu.mihosoft.vrl.vrljoglplugin.glview.GLMeshCanvas;
import eu.mihosoft.vrl.vrljoglplugin.glview.Mesh;
import eu.mihosoft.vrl.vrljoglplugin.glview.STLLoader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

@ComponentInfo(name = "STL Viewer", category = "JOGL")
public class STLViewer implements Serializable {
    private static final long serialVersionUID = 1L;

    public Visualization view(@ParamInfo(
            style="load-dialog",
            options="endings=[\".stl\"]; description=\"*.stl - Files\"") File stlFile) throws IOException {

        Mesh mesh = new STLLoader().loadMesh(stlFile);

        Visualization visualization =  new GLMeshCanvas(mesh);

        return visualization;
    }
}
