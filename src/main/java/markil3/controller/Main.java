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
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.system.AppSettings;

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

    /**
     * Triangle on Playstation, Y on Xbox, and X on Nintendo.
     */
    public static final String ACTION_TOP = JoystickButton.BUTTON_0;
    /**
     * Circle on Playstation, B on Xbox, and A on Nintendo.
     */
    public static final String ACTION_RIGHT = JoystickButton.BUTTON_1;
    /**
     * X on Playstation, A on Xbox, and B on Nintendo.
     */
    public static final String ACTION_BOTTOM = JoystickButton.BUTTON_2;
    /**
     * Square on Playstation, X on Xbox, and Y on Nintendo.
     */
    public static final String ACTION_LEFT = JoystickButton.BUTTON_3;
    public static final String L1 = JoystickButton.BUTTON_4;
    public static final String R1 = JoystickButton.BUTTON_5;
    /**
     * Some gamepads (Xbox controllers notable) will use
     * {@link JoystickAxis#LEFT_TRIGGER} instead.
     */
    public static final String L2 = JoystickButton.BUTTON_6;
    /**
     * Some gamepads (Xbox controllers notable) will use
     * {@link JoystickAxis#RIGHT_TRIGGER} instead.
     */
    public static final String R2 = JoystickButton.BUTTON_7;
    public static final String SELECT = JoystickButton.BUTTON_8;
    public static final String START = JoystickButton.BUTTON_9;
    /**
     * Pressing the left analog stick.
     */
    public static final String L3 = JoystickButton.BUTTON_10;
    /**
     * Pressing the right analog stick.
     */
    public static final String R3 = JoystickButton.BUTTON_11;
    /**
     * Most gamepads may use {@link JoystickAxis#POV_X} instead.
     */
    public static final String DPAD_LEFT = "12";
    /**
     * Most gamepads may use {@link JoystickAxis#POV_X} instead.
     */
    public static final String DPAD_RIGHT = "13";
    /**
     * Most gamepads may use {@link JoystickAxis#POV_Y} instead.
     */
    public static final String DPAD_UP = "14";
    /**
     * Most gamepads may use {@link JoystickAxis#POV_Y} instead.
     */
    public static final String DPAD_DOWN = "15";

    public static void main(String[] args)
    {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Joystick Preview");
        settings.setUseJoysticks(true);
        settings.setEmulateMouse(true);
        settings.setVSync(true);
        app.setSettings(settings);
        app.start();
    }

    public Main()
    {
        super(new StatsAppState(), new DebugKeysAppState(), new JoystickPreviewScreen());
    }

    @Override
    public void simpleInitApp()
    {

    }
}
