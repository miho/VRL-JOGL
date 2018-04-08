/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vrl.vrljoglplugin;

import eu.mihosoft.vrl.system.*;
import eu.mihosoft.vrl.vrljoglplugin.glview.GLMeshCanvas;
import eu.mihosoft.vrl.vrljoglplugin.glview.Mesh;


/**
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class PluginConfigurator extends VPluginConfigurator {

    public PluginConfigurator() {
        //specify the plugin name and version
        setIdentifier(new PluginIdentifier("JOGL", "0.4.1"));

        // optionally allow other plugins to use the api of this plugin
        // you can specify packages that shall be
        // exported by using the exportPackage() method:
        //
        // exportPackage("com.your.package");

        exportClass(Mesh.class.getName());
        exportClass(JOGLCanvas3D.class.getName());
        exportClass(GLMeshCanvas.class.getName());
        exportClass(JoglType.class.getName());
        exportClass(STLViewer.class.getName());

        // describe the plugin
        setDescription("JOGL Plugin");

        // copyright info
        setCopyrightInfo("JOGL-Plugin",
                "(c) Michael Hoffer",
                "www.mihosoft.eu", "see VRL-Studio", "see VRL-Studio");

        // specify dependencies
        // addDependency(new PluginDependency("VRL", "0.4.0", "0.4.0"));

        this.setLoadNativeLibraries(false);
    }

    @Override
    public void register(PluginAPI api) {

        // register plugin with canvas
        if (api instanceof VPluginAPI) {
            VPluginAPI vapi = (VPluginAPI) api;

            // Register visual components:
            //
            // Here you can add additional components,
            // type representations, styles etc.
            //
            // ** NOTE **
            //
            // To ensure compatibility with future versions of VRL,
            // you should only use the vapi or api object for registration.
            // If you directly use the canvas or its properties, please make
            // sure that you specify the VRL versions you are compatible with
            // in the constructor of this plugin configurator because the
            // internal api is likely to change.
            //
            // examples:
            //
            // vapi.addComponent(MyComponent.class);
            // vapi.addTypeRepresentation(MyType.class);

            vapi.addTypeRepresentation(JoglType.class);
            vapi.addComponent(STLViewer.class);
        }
    }

    @Override
    public void unregister(PluginAPI api) {
        // nothing to unregister
    }

    @Override
    public void init(InitPluginAPI iApi) {
        // nothing to init
    }
}
