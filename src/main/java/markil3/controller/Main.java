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
