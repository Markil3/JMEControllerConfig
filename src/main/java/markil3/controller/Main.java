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

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.input.JoystickCompatibilityMappings;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Launch point for the application. This is primarily for demo purposes, and
 * games using this as a library can safely ignore it.
 *
 * @author Markil 3
 * @version 1.0
 */
public class Main extends SimpleApplication
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.
            getLogger(Main.class);

    public static File GAME_FOLDER;
    public static File CALIBRATION_FILE;

    public static File getGameFolder()
    {
        return GAME_FOLDER;
    }

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
        Main app;
        AppSettings settings;
        app = new Main();
        settings = new AppSettings(true);
        settings.setTitle("Joystick Preview");
        settings.setUseJoysticks(true);
        settings.setEmulateMouse(true);
        settings.setVSync(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        app.setSettings(settings);
        app.start();
    }

    public Main()
    {
        super(new StatsAppState(), new DebugKeysAppState(),
                new NewJoystickPreviewScreen());
    }

    @Override
    public void initialize()
    {
        if (GAME_FOLDER == null)
        {
            GAME_FOLDER = new File(System.getProperty("user.dir"));
            if (!GAME_FOLDER.isDirectory())
            {
                if (!GAME_FOLDER.mkdir())
                {
                    throw new RuntimeException(
                            "Could not create game directory folder.");
                }
            }
        }
        /*
         * Add custom joystick mappings before the input manager is loaded.
         */
        initializeJoystickMappings();
        super.initialize();
    }

    @Override
    public void simpleInitApp()
    {
    }
}
