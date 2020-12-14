/*
 * Copyright 2020 Markil 3. All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package markil3.controller;

import com.jme3.app.SimpleApplication;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickCompatibilityMappings;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.focus.FocusNavigationState;
import com.simsilica.lemur.style.BaseStyles;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;

/**
 * Launch point for the game.
 *
 * @author Markil 3
 */
public class Main extends SimpleApplication
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.
            getLogger(Main.class);

    public static File GAME_FOLDER;
    public static File CALIBRATION_FILE;

    private static void initializeJoystickMappings()
    {
        if (CALIBRATION_FILE == null)
        {
            CALIBRATION_FILE =
                    new File(GAME_FOLDER, "controllerCalibration.properties");
            URL mappingUrl;
            switch (JmeSystem.getPlatform())
            {
            case Windows32:
            case Windows64:
                mappingUrl = Main.class.
                        getResource("/joystick-mapping.windows.properties");
                break;
            case MacOSX32:
            case MacOSX64:
            case MacOSX_PPC32:
            case MacOSX_PPC64:
                mappingUrl = Main.class.
                        getResource("/joystick-mapping.osx.properties");
                break;
            case Linux32:
            case Linux64:
            case Linux_ARM32:
            case Linux_ARM64:
                mappingUrl = Main.class.
                        getResource("/joystick-mapping.linux.properties");
                break;
            case Android_ARM5:
            case Android_ARM6:
            case Android_ARM7:
            case Android_ARM8:
            case Android_X86:
            case Android_Other:
                mappingUrl = Main.class.
                        getResource("/joystick-mapping.android.properties");
                break;
            case iOS_ARM:
            case iOS_X86:
                mappingUrl = Main.class.
                        getResource("/joystick-mapping.ios.properties");
                break;
            default:
                mappingUrl = null;
            }
            if (mappingUrl != null)
            {
                try
                {
                    JoystickCompatibilityMappings
                            .loadMappingProperties(mappingUrl);
                }
                catch (IOException e)
                {
                    logger.error("Unable to load joystick mappings for " +
                            mappingUrl, e);
                }
            }
            mappingUrl = Main.class.
                    getResource("/joystick-mapping." +
                            JmeSystem.getPlatform().toString().toLowerCase() +
                            ".properties");
            if (mappingUrl != null)
            {
                try
                {
                    JoystickCompatibilityMappings
                            .loadMappingProperties(mappingUrl);
                }
                catch (IOException e)
                {
                    logger.error("Unable to load joystick mappings for " +
                            mappingUrl, e);
                }
            }
            if (CALIBRATION_FILE.isFile())
            {
                try
                {
                    JoystickCompatibilityMappings.loadMappingProperties(
                            CALIBRATION_FILE.toURI().toURL());
                }
                catch (IOException e)
                {
                    logger.error("Unable to load joystick mappings.", e);
                }
            }
        }
    }

    public static void main(String[] args)
    {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        try
        {
            if (Arrays.binarySearch(args, "--defaultSettings") < 0)
            {
                settings.load("JMEControllerConfig");
            }
        }
        catch (BackingStoreException e1)
        {
            e1.printStackTrace();
        }
        finally
        {
            settings.setTitle("JMEControllerConfig");
            settings.setUseJoysticks(true);
            settings.setEmulateMouse(false);
            settings.setVSync(true);
            try
            {
                //				BufferedImage[] icons = new BufferedImage[] {
                //				ImageIO.read(BootstrapClass.class.getResource
                //				("/Interface/icon16.png")), ImageIO.read
                //				(BootstrapClass.class.getResource
                //				("/Interface/icon32.png")), ImageIO.read
                //				(BootstrapClass.class.getResource
                //				("/Interface/icon64.png")), ImageIO.read
                //				(BootstrapClass.class.getResource
                //				("/Interface/icon128.png")), ImageIO.read
                //				(BootstrapClass.class.getResource
                //				("/Interface/icon256.png")), ImageIO.read
                //				(BootstrapClass.class.getResource
                //				("/Interface/icon512.png")) };
                //				settings.setIcons(icons);
            }
            //			catch (IOException e)
            //			{
            //				e.printStackTrace();
            //			}
            finally
            {
                //				this.app.setShowSettings(!settings.getBoolean
                //				("initialConfirmed"));
                // Marks that we have already shown the initial setup
                // settings, and don't need to do so again.
                settings.putBoolean("initialConfirmed", true);
                app.setSettings(settings);
                try
                {
                    settings.save(settings.getTitle());
                }
                catch (BackingStoreException ex)
                {
                    logger.warn("Failed to save setting changes", ex);
                }
                finally
                {
                    app.start();
                }
            }
        }
    }

    @Override
    public void initialize()
    {
        /*
         * Add custom joystick mappings before the input manager is loaded.
         */
//        initializeJoystickMappings();
        super.initialize();
    }

    @Override
    public void simpleInitApp()
    {
        for (Joystick stick : this.inputManager.getJoysticks())
        {
            for (JoystickAxis axis : stick.getAxes())
            {
                if (axis instanceof DefaultJoystickAxis)
                {
                    ((DefaultJoystickAxis) axis).setDeadZone(0.4F);
                }
            }
        }

        // Lemur
        GuiGlobals.initialize(this);
        GuiGlobals.getInstance().setCursorEventsEnabled(false);
        this.getStateManager().detach(this.getStateManager()
                .getState(FocusNavigationState.class));
        BaseStyles.loadStyleResources("/com/simsilica/lemur/style/base/glass-styles.groovy");
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("crate");
        Box b = new Box(1F, 1F, 1F); // create cube shape
//        Sphere b = new Sphere(16, 32, 1F); // create cube shape
        Geometry geom =
                new Geometry("Box", b); // create cube geometry from the shape
        Material mat = new Material(this.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"); // create a simple
//        mat.setBoolean("UseMaterialColors", true);
        // material
        mat.setColor("Color", ColorRGBA.Blue); // set color of material
//        to blue
        geom.setMaterial(mat); // set the cube's material
        rootNode.attachChild(geom); // make the cube appear in the scene
    }
}
