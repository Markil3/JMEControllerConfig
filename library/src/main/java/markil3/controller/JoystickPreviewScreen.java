/*
 * Copyright 2020 Markil 3. All rights reserved.
 * Copyright (c) 2009-2020 jMonkeyEngine. All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package markil3.controller;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.JoystickConnectionListener;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Adding this app state will display a GUI screen showing the controllers
 * connected and information on what buttons are pressed. It is primarily
 * useful for debugging controllers.
 * <p>Note that this class relies on three textures not found in the default
 * JME core: "Interface/Joystick/gamepad-buttons.png",
 * "Interface/Joystick/gamepad-frame.png," and
 * "Interface/Joystick/gamepad-stick.png." These textures can be obtained
 * from the org.jmonkeyengine:jme3-testdata library.</p>
 *
 * @author Markil 3
 * @author Normen Hansen
 * @author Kirill Vainer
 * @author Paul Speed
 * @author dokthar
 * @author Stephen Gold
 */
public class JoystickPreviewScreen extends BaseAppState
        implements RawInputListener, JoystickConnectionListener, ActionListener
{
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(JoystickPreviewScreen.class);

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
    final String CLICK_MAPPING = "previewButtonClick";

    /**
     * This node serves as the center of logic for each gamepad connected to
     * the computer.
     */
    static class GamepadView extends Node
    {

        AssetManager assetManager;
        JoystickPreviewScreen prevScreen;

        float xAxis = 0;
        float yAxis = 0;
        float zAxis = 0;
        float zRotation = 0;

        float lastPovX = 0;
        float lastPovY = 0;

        float leftTrig = -1F;
        float rightTrig = -1F;

        Geometry leftStick;
        Geometry rightStick;

        Map<String, ButtonView> buttons = new HashMap<>();

        private boolean l2;

        private boolean r2;

        GamepadView(JoystickPreviewScreen prevScreen, AssetManager assetManager)
        {
            super("gamepad");

            this.prevScreen = prevScreen;
            this.assetManager = assetManager;

            // Sizes naturally for the texture size.  All positions will
            // be in that space because it's easier.
            int size = 512;

            Material m = new Material(assetManager,
                    "Common/MatDefs/Misc/Unshaded.j3md");
            m.setTexture("ColorMap", assetManager
                    .loadTexture("Interface/Joystick/gamepad-buttons.png"));
            m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            Geometry buttonPanel =
                    new Geometry("buttons", new Quad(size, size));
            buttonPanel.setLocalTranslation(0, 0, -1);
            buttonPanel.setMaterial(m);
            attachChild(buttonPanel);

            m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            m.setTexture("ColorMap", assetManager
                    .loadTexture("Interface/Joystick/gamepad-frame.png"));
            m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            Geometry frame = new Geometry("frame", new Quad(size, size));
            frame.setMaterial(m);
            attachChild(frame);

            m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            m.setTexture("ColorMap", assetManager
                    .loadTexture("Interface/Joystick/gamepad-stick.png"));
            m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            leftStick = new Geometry("leftStick", new Quad(64, 64));
            leftStick.setMaterial(m);
            attachChild(leftStick);
            rightStick = new Geometry("rightStick", new Quad(64, 64));
            rightStick.setMaterial(m);
            attachChild(rightStick);

            // A "standard" mapping... fits a majority of my game pads
            addButton(ACTION_TOP, 371, 512 - 176, 42, 42);
            addButton(ACTION_RIGHT, 407, 512 - 212, 42, 42);
            addButton(ACTION_BOTTOM, 371, 512 - 248, 42, 42);
            addButton(ACTION_LEFT, 334, 512 - 212, 42, 42);

            // Front buttons  Some of these have the top ones and the bottoms
            // ones flipped.
            addButton(L1, 67, 512 - 111, 95, 21);
            addButton(R1, 348, 512 - 111, 95, 21);
            addButton(L2, 67, 512 - 89, 95, 21);
            addButton(R2, 348, 512 - 89, 95, 21);

            // Select and start buttons
            addButton(SELECT, 206, 512 - 198, 48, 30);
            addButton(START, 262, 512 - 198, 48, 30);

            // Joystick push buttons
            addButton(L3, 147, 512 - 300, 75, 70);
            addButton(R3, 285, 512 - 300, 75, 70);

            //    +Y
            //  -X  +X
            //    -Y
            //
            addButton(DPAD_UP, 96, 512 - 174, 40, 38);
            addButton(DPAD_RIGHT, 128, 512 - 208, 40, 38);
            addButton(DPAD_DOWN, 96, 512 - 239, 40, 38);
            addButton(DPAD_LEFT, 65, 512 - 208, 40, 38);

            resetPositions();
        }

        private void addButton(String name, float x, float y, float width,
                               float height)
        {
            ButtonView b = new ButtonView(this.assetManager, name, x, y, width,
                    height);
            attachChild(b);
            buttons.put(name, b);
        }

        void setAxisValue(JoystickAxis axis, float value)
        {
            LoggerFactory.getLogger(axis.getJoystick().getName())
                    .info(axis.getJoystick().getName() + "\n\tAxis:" +
                            axis.getName() + " (" + axis.getAxisId() + ")=" +
                            value);
            if (this.prevScreen != null)
            {
                if (this.prevScreen.labels == null ||
                        this.prevScreen.labels[axis.getJoystick()
                                .getJoyId()][0] == null ||
                        !this.prevScreen.labels[axis.getJoystick()
                                .getJoyId()][0].getText()
                                .equals(axis.getJoystick().getName()))
                {
                    this.prevScreen.setLabels(axis.getJoystick());
                }
            }
            if (axis == axis.getJoystick().getXAxis())
            {
                setXAxis(value);
            }
            else if (axis == axis.getJoystick().getYAxis())
            {
                setYAxis(-value);
            }
            else if (axis == axis.getJoystick().getAxis(JoystickAxis.Z_AXIS))
            {
                // Note: in the above condition, we could check the axis name
                // but
                //       I have at least one joystick that reports 2 "Z Axis"
                //       axes.
                //       In this particular case, the first one is the right
                //       one so
                //       a name based lookup will find the proper one.  It's
                //       a problem
                //       because the erroneous axis sends a constant stream
                //       of values.
                setZAxis(value);
            }
            else if (axis ==
                    axis.getJoystick().getAxis(JoystickAxis.Z_ROTATION))
            {
                setZRotation(-value);
            }
            else if (axis ==
                    axis.getJoystick().getAxis(JoystickAxis.LEFT_TRIGGER))
            {
                if (axis.getJoystick().getButton(L2) == null)
                {
                    // left/right triggers sometimes only show up as axes
                    boolean pressed = value > 0;
                    if (pressed != this.buttons.get(L2).
                            isDown())
                    {
                        setButtonValue(L2, pressed);
                    }
                }
            }
            else if (axis ==
                    axis.getJoystick().getAxis(JoystickAxis.RIGHT_TRIGGER))
            {
                if (axis.getJoystick().getButton(R2) == null)
                {
                    // left/right triggers sometimes only show up as axes
                    boolean pressed = value > 0;
                    if (pressed != this.buttons.get(R2).
                            isDown())
                    {
                        setButtonValue(R2, pressed);
                    }
                }
            }
            else if (axis == axis.getJoystick().getPovXAxis())
            {
                if (lastPovX < 0)
                {
                    setButtonValue(DPAD_LEFT, false);
                }
                else if (lastPovX > 0)
                {
                    setButtonValue(DPAD_RIGHT, false);
                }
                if (value < 0)
                {
                    setButtonValue(DPAD_LEFT, true);
                }
                else if (value > 0)
                {
                    setButtonValue(DPAD_RIGHT, true);
                }
                lastPovX = value;
            }
            else if (axis == axis.getJoystick().getPovYAxis())
            {
                if (lastPovY < 0)
                {
                    setButtonValue(DPAD_DOWN, false);
                }
                else if (lastPovY > 0)
                {
                    setButtonValue(DPAD_UP, false);
                }
                if (value < 0)
                {
                    setButtonValue(DPAD_DOWN, true);
                }
                else if (value > 0)
                {
                    setButtonValue(DPAD_UP, true);
                }
                lastPovY = value;
            }
            if (this.prevScreen != null)
            {
                this.prevScreen.labels[axis.getJoystick().getJoyId()][axis.
                        getAxisId() * 2 + 2].setText(Float.toString(value));
            }
        }

        void setButtonValue(JoystickButton button, boolean isPressed)
        {
            LoggerFactory.getLogger(button.getJoystick().getName())
                    .info(button.getJoystick().getName() + "\n\tButton:" +
                            button.getName() + " (" + button.getButtonId() +
                            ")=" + (isPressed ? "Down" : "Up"));
            if (this.prevScreen != null)
            {
                if (this.prevScreen.labels == null ||
                        this.prevScreen.labels[button.getJoystick()
                                .getJoyId()][0] == null ||
                        !this.prevScreen.labels[button.getJoystick()
                                .getJoyId()][0].getText()
                                .equals(button.getJoystick().getName()))
                {
                    this.prevScreen.setLabels(button.getJoystick());
                }
            }
            if (button.getLogicalId().equals(L2))
            {
                l2 = isPressed;
                setButtonValue(button.getLogicalId(),
                        isPressed || this.leftTrig >= 0F);
            }
            else if (button.getLogicalId().equals(R2))
            {
                r2 = isPressed;
                setButtonValue(button.getLogicalId(),
                        isPressed || this.rightTrig >= 0F);
            }
            else
            {
                setButtonValue(button.getLogicalId(), isPressed);
            }
            if (this.prevScreen != null)
            {
                try
                {
                    this.prevScreen.labels[button.getJoystick().getJoyId()][2 *
                            (button.getButtonId() +
                                    button.getJoystick().getAxisCount()) + 3]
                            .setText(Boolean.toString(isPressed));
                }
                catch (ArrayIndexOutOfBoundsException aie)
                {
                    logger.error("Couldn't find index for button (" +
                            button.getName() + ", " + button.getLogicalId() +
                            ", " + button.getButtonId() + ")", aie);
//                    throw new RuntimeException(
//                            "Couldn't find index for button (" +
//                            button.getName() + ", " +
//                            button.getLogicalId() + ", " +
//                            button.getButtonId() + ")", aie);
                }
            }
            //			lastButton = button;
        }

        void setButtonValue(String name, boolean isPressed)
        {
            ButtonView view = buttons.get(name);
            if (view != null)
            {
                if (isPressed)
                {
                    view.down();
                }
                else
                {
                    view.up();
                }
            }
        }

        void setXAxis(float f)
        {
            xAxis = f;
            resetPositions();
        }

        void setYAxis(float f)
        {
            yAxis = f;
            resetPositions();
        }

        void setZAxis(float f)
        {
            zAxis = f;
            resetPositions();
        }

        void setZRotation(float f)
        {
            zRotation = f;
            resetPositions();
        }

        private void resetPositions()
        {

            float xBase = 155;
            float yBase = 212;

            Vector2f dir = new Vector2f(xAxis, yAxis);
            float length = Math.min(1, dir.length());
            dir.normalizeLocal();

            float angle = dir.getAngle();
            float x = FastMath.cos(angle) * length * 10;
            float y = FastMath.sin(angle) * length * 10;
            leftStick.setLocalTranslation(xBase + x, yBase + y, 0);

            xBase = 291;
            dir = new Vector2f(zAxis, zRotation);
            length = Math.min(1, dir.length());
            dir.normalizeLocal();

            angle = dir.getAngle();
            x = FastMath.cos(angle) * length * 10;
            y = FastMath.sin(angle) * length * 10;
            rightStick.setLocalTranslation(xBase + x, yBase + y, 0);
        }
    }

    /**
     * Applied to the buttons to highlight which ones are being pressed.
     */
    static class ButtonView extends Node
    {
        private static final ColorRGBA hilite =
                new ColorRGBA(0.0f, 0.75f, 0.75f, 0.5f);
        private int state = 0;
        private Material material;

        ButtonView(AssetManager assetManager, String name, float x, float y,
                   float width, float height)
        {
            super("Button:" + name);
            setLocalTranslation(x, y, -0.5f);

            this.material = new Material(assetManager,
                    "Common/MatDefs/Misc/Unshaded.j3md");
            this.material.setColor("Color", hilite);
            this.material.getAdditionalRenderState()
                    .setBlendMode(BlendMode.Alpha);

            Geometry g = new Geometry("highlight", new Quad(width, height));
            g.setMaterial(this.material);
            g.setUserData("view", this);
            attachChild(g);

            resetState();
        }

        /**
         * Updates the visual of whether the button is pressed or not.
         */
        private void resetState()
        {
            if (state <= 0)
            {
                setCullHint(CullHint.Always);
            }
            else
            {
                setCullHint(CullHint.Dynamic);
            }

            //			System.out.println(getName() + " state:" + state);
        }

        /**
         * Checks to see if the visual displays if the button is pressed.
         * @return True if the visual displays that the button is pressed,
         * false otherwise.
         */
        public boolean isDown()
        {
            return this.state > 0;
        }

        /**
         * Updates the button to display that it is pressed.
         * @see #up()
         */
        public void down()
        {
            this.state++;
            this.resetState();
        }

        /**
         * Updates the button to display that it is not pressed.
         * @see #down()
         */
        public void up()
        {
            this.state--;
            this.resetState();
        }
    }

    protected Node gui;
    protected BitmapFont guiFont;

    private Node[] gamepadCont;

    private GamepadView[] gamepadView;
    private Node[] gamepadHeaders;

    private BitmapText[][] labels;

    private BitmapText refLabel;

    private Map<JoystickAxis, Float> lastValues = new HashMap<>();

    @Override
    public void initialize(Application app)
    {
        this.gui = new Node();
        this.guiFont =
                app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        this.refLabel = this.guiFont.createLabel("Axis X/Axis Y");
        this.gui.attachChild(this.refLabel);

        this.updateGamepad();

        ((SimpleApplication) this.getApplication()).getGuiNode()
                .attachChild(this.gui);

        app.getInputManager().addMapping(CLICK_MAPPING,
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    }

    /**
     * Adds all the GUI elements to the screen.
     */
    private void updateGamepad()
    {
        int mostButtons = 0;
        Joystick joy;
        int l = this.getApplication().getInputManager().getJoysticks().length;

        /*
         * Removes any existing gamepads.
         */
        if (this.gamepadCont != null)
        {
            for (Node cont : this.gamepadCont)
            {
                if (cont != null)
                {
                    cont.removeFromParent();
                }
            }
        }

        /*
         * Removes the labels, just in case something changed and the
         * previous ones are incompatible.
         */
        if (this.labels != null)
        {
            for (BitmapText[] cont : this.labels)
            {
                for (BitmapText cont2 : cont)
                {
                    if (cont2 != null)
                    {
                        cont2.removeFromParent();
                    }
                }
            }
        }
        /*
         * Initialize the array of labels and gamepads.
         */
        for (int i = 0; i < l; i++)
        {
            joy = this.getApplication().getInputManager().getJoysticks()[i];
            mostButtons = Math.max(joy.getAxisCount() + joy.getButtonCount(),
                    mostButtons);
        }
        this.labels = new BitmapText[l][mostButtons * 2 + 3];
        this.gamepadCont = new Node[this.getApplication().getInputManager()
                .getJoysticks().length];
        this.gamepadView =
                new GamepadView[this.getApplication().getInputManager()
                        .getJoysticks().length];

        for (int i = 0; i < l; i++)
        {
            this.gamepadCont[i] = new Node();
            this.gamepadView[i] = new GamepadView(this,
                    this.getApplication().getAssetManager());
            this.gamepadView[i].setLocalTranslation(-128, -384, 0);
            this.gamepadCont[i].attachChild(this.gamepadView[i]);
            if (i == 0)
            {
                this.gui.attachChild(this.gamepadCont[i]);
            }
        }

        this.addButtons();
    }

    /**
     * Adds the tab buttons used to switch between which gamepad is currently
     * being viewed.
     */
    private void addButtons()
    {
        int i, l;

        /*
         * Clears out any old tab buttons.
         */
        if (this.gamepadHeaders != null)
        {
            for (i = 0, l = this.gamepadHeaders.length; i < l; i++)
            {
                this.gamepadHeaders[i].removeFromParent();
            }
        }

        l = this.gamepadCont.length;
        this.gamepadHeaders = new Node[l];
        for (i = 0, l = this.gamepadCont.length; i < l; i++)
        {
            this.gamepadHeaders[i] = GUIUtils.createButton(
                    this.getApplication().getAssetManager(), this.guiFont,
                    "gamepad" + i, "Gamepad " +
                            this.getApplication().getInputManager()
                                    .getJoysticks()[i].getJoyId());
            this.gui.attachChild(this.gamepadHeaders[i]);
        }
    }

    /**
     * Obtain the size of the screen based on the game camera.
     * @return The screen size in a two-dimensional float vector. Note that
     * the numbers will always be integers.
     */
    private Vector2f getScreenSize()
    {
        return new Vector2f(this.getApplication().getCamera().getWidth(),
                this.getApplication().getCamera().getHeight());
    }

    @Override
    protected void cleanup(Application app)
    {
        this.gui.removeFromParent();
        app.getInputManager().deleteMapping(CLICK_MAPPING);
    }

    @Override
    protected void onEnable()
    {
        this.getApplication().getInputManager().addRawInputListener(this);
        this.getApplication().getInputManager()
                .addJoystickConnectionListener(this);
        this.resize();
        this.getApplication().getInputManager()
                .addListener(this, CLICK_MAPPING);
    }

    @Override
    protected void onDisable()
    {
        this.getApplication().getInputManager().removeRawInputListener(this);
        this.getApplication().getInputManager()
                .removeJoystickConnectionListener(this);
        this.getApplication().getInputManager().removeListener(this);
    }

    /**
     * Updates the values of the gamepad labels as buttons and axis are
     * manipulated.
     * @param joy - The gamepad to update.
     */
    private void setLabels(Joystick joy)
    {
        /*
         * Removes the old labels.
         */
        if (labels != null)
        {
            for (BitmapText label : this.labels[joy.getJoyId()])
            {
                if (label != null)
                {
                    label.removeFromParent();
                }
            }
        }
        //		this.labels = new Label[joy.getJoyId()][(joy.getAxisCount() +
        //		joy.getButtonCount()) * 2 + 1];
        if (this.labels != null)
        {
            /*
             * The name of the gamepad.
             */
            this.labels[joy.getJoyId()][0] = this.guiFont.createLabel(
                    "Gamepad " + joy.getJoyId() + ": " + joy.getName());
            this.labels[joy.getJoyId()][0].setLocalTranslation(20, -25, 0);
            this.gamepadCont[joy.getJoyId()]
                    .attachChild(this.labels[joy.getJoyId()][0]);
            /*
             * The key for the axis. Each of the axis rows will display the
             * index of the axis, its given name, its logical ID after
             * joystick remapping has occurred, and the axis index again.
             * TODO - Whenever I add the " Gamepad X: " part to the previous
             *  label, this one displays all funky.
             */
            this.labels[joy.getJoyId()][1] = this.guiFont
                    .createLabel("Axis Index: Axis Name (logical ID, axis ID)");
            this.labels[joy.getJoyId()][1].setLocalTranslation(20, -50, 0);
            this.gamepadCont[joy.getJoyId()]
                    .attachChild(this.labels[joy.getJoyId()][1]);
            /*
             * Loop through all the axes.
             */
            for (int i = 0; i < joy.getAxisCount(); i++)
            {
                JoystickAxis axis = joy.getAxes().get(i);
                try
                {
                    /*
                     * The name and information of the axis.
                     */
                    BitmapText label = this.guiFont.createLabel(
                            (i) + ": " + axis.getName() + " (" +
                                    axis.getLogicalId() + ", " +
                                    axis.getAxisId() + "): ");
                    label.setLocalTranslation(20, -25 * (i + 3), 0);
                    this.labels[joy.getJoyId()][i * 2 + 1] = label;

                    /*
                     * The current value of the axis.
                     */
                    BitmapText label2 = this.guiFont.createLabel("-1.0");
                    label2.setLocalTranslation(label.getLocalTranslation()
                            .add(label.getLineWidth(), 0, 0));
                    this.labels[joy.getJoyId()][i * 2 + 2] = label2;

                    this.gamepadCont[joy.getJoyId()].attachChild(label);
                    this.gamepadCont[joy.getJoyId()].attachChild(label2);
                }
                catch (ArrayIndexOutOfBoundsException aie)
                {
                    logger.error(
                            "Couldn't find index for axis (" + axis.getName() +
                                    ", " + axis.getLogicalId() + ", " +
                                    axis.getAxisId() + ")", aie);
//                    throw new RuntimeException(
//                            "Couldn't find index for button " + i + " (" +
//                            axis.getName() + ", " +
//                            axis.getLogicalId() + ", " +
//                            axis.getAxisId() + ")", aie);
                }
            }

            int firstButtonIndex = 2 * joy.getAxisCount() + 1;
            /*
             * The key for the buttons. Each of the button rows will display the
             * index of the button offset by the number of axes we had, its
             * given name, its logical ID after
             * joystick remapping has occurred, and the actual button index.
             */
            this.labels[joy.getJoyId()][firstButtonIndex] = this.guiFont
                    .createLabel(
                            "Button Index: Button Name (logical ID, button " +
                                    "ID)");
            this.labels[joy.getJoyId()][firstButtonIndex].setLocalTranslation(
                    this.getScreenSize().x -
                            this.labels[joy.getJoyId()][firstButtonIndex]
                                    .getLineWidth(), -50, 0);
            this.gamepadCont[joy.getJoyId()]
                    .attachChild(this.labels[joy.getJoyId()][firstButtonIndex]);

            /*
             * Loop through all the buttons.
             */
            for (int i = 0; i < joy.getButtonCount(); i++)
            {
                JoystickButton button = joy.getButtons().get(i);
                try
                {
                    /*
                     * The current value of the button. We create this first
                     since the name will be placed in relation to this, due
                     to it being right-justified.
                     */
                    BitmapText label2 = this.guiFont.createLabel("false");
                    label2.setLocalTranslation(
                            this.getScreenSize().x - label2.getLineWidth(),
                            -25 * (i + 3), 0);

                    /*
                     * The name and information for the button.
                     */
                    BitmapText label = this.guiFont.createLabel(
                            (i + joy.getAxisCount()) + ": " + button.getName() +
                                    " (" + button.getLogicalId() + ", " +
                                    button.getButtonId() + "): ");
                    label.setLocalTranslation(label2.getLocalTranslation()
                            .add(-label.getLineWidth(), 0, 0));

                    this.labels[joy.getJoyId()][2 * (i + joy.getAxisCount()) +
                            2] = label;
                    this.labels[joy.getJoyId()][2 * (i + joy.getAxisCount()) +
                            3] = label2;

                    this.gamepadCont[joy.getJoyId()].attachChild(label);
                    this.gamepadCont[joy.getJoyId()].attachChild(label2);
                }
                catch (ArrayIndexOutOfBoundsException aie)
                {
                    logger.error("Couldn't find index for button (" +
                            button.getName() + ", " + button.getLogicalId() +
                            ", " + button.getButtonId() + ")", aie);
//                    throw new RuntimeException(
//                            "Couldn't find index for button " + i + " (" +
//                            button.getName() + ", " +
//                            button.getLogicalId() + ", " +
//                            button.getButtonId() + ")", aie);
                }
            }
        }
    }

    /**
     * Automatically scales and positions elements based on the camera
     * dimensions.
     */
    public void resize()
    {
        Camera camera = this.getApplication().getCamera();
        this.resize(camera.getWidth(), camera.getHeight());
    }

    /**
     * Scales and positions elements of this screen.
     * @param width - The width to scale to.
     * @param height - The height to scale to.
     */
    protected void resize(int width, int height)
    {
        this.gui.setLocalTranslation(0, this.getScreenSize().y, 0);
        if (this.gamepadHeaders != null)
        {
            Node button;
            for (int i = 0, l = this.gamepadHeaders.length; i < l; i++)
            {
                button = this.gamepadHeaders[i];
                button.setLocalTranslation(128 * i, 0, 0);
            }
        }
        if (this.gamepadView != null)
        {
            for (GamepadView view : this.gamepadView)
            {
                view.setLocalTranslation(width / 2F - 256F, -512F, 0);
            }
        }
        this.refLabel.setLocalTranslation(
                (width - this.refLabel.getLineWidth()) / 2F, 0, 0);
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt)
    {
        //		setViewedJoystick(evt.getAxis().getJoystick());
//        this.gamepadView[evt.getJoyIndex()]
//                .setAxisValue(evt.getAxis(), evt.getValue());
        Float last = this.lastValues.remove(evt.getAxis());
        float value = evt.getValue();

        // Check the axis dead zone.  InputManager normally does this
        // by default but not for raw events like we get here.
        float effectiveDeadZone = Math.max(this.getApplication().
                getInputManager().getAxisDeadZone(), evt.
                getAxis().getDeadZone());
        if (Math.abs(value) < effectiveDeadZone)
        {
            if (last == null)
            {
                // Just skip the event
                return;
            }
            // Else set the value to 0
            lastValues.remove(evt.getAxis());
            value = 0;
        }
        this.gamepadView[evt.getJoyIndex()].setAxisValue(evt.getAxis(), value);
        if (value != 0)
        {
            lastValues.put(evt.getAxis(), value);
        }
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt)
    {
        //		setViewedJoystick(evt.getButton().getJoystick());
        this.gamepadView[evt.getJoyIndex()]
                .setButtonValue(evt.getButton(), evt.isPressed());
    }

    @Override
    public void beginInput()
    {
    }

    @Override
    public void endInput()
    {
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf)
    {
        String buttonId;
        int gamepad;
        if (name.equals(CLICK_MAPPING))
        {
            buttonId = GUIUtils.handleButtonPress(this.gui,
                    this.getApplication().getInputManager().getCursorPosition(),
                    isPressed);
            if (!isPressed && buttonId != null)
            {
                if (buttonId.startsWith("gamepad"))
                {
                    gamepad = Integer.parseInt(buttonId.substring(7));
                    for (int i = 0, l = this.gamepadCont.length; i < l; i++)
                    {
                        if (i == gamepad)
                        {
                            this.gui.attachChild(this.gamepadCont[i]);
                        }
                        else
                        {
                            this.gamepadCont[i].removeFromParent();
                        }
                    }
                }
            }
        }
    }

    /**
     * Displays which button is which as the user hovers over the button.
     * TODO - Doesn't trigger.
     * @param evt - Input event data.
     */
    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt)
    {
//        ButtonView button = null;
//        CollisionResults results = this.checkMouse(evt.getX(), evt.getY());
//        if (results.size() > 0)
//        {
//            for (CollisionResult result : results)
//            {
//                if (result.getGeometry().getUserData("view") != null)
//                {
//                    if (result.getGeometry()
//                            .getUserData("view") instanceof ButtonView)
//                    {
//                        button = result.getGeometry().getUserData("view");
//                        break;
//                    }
//                }
//            }
//        }
//        if (button != null)
//        {
//            this.refLabel.setText(button.getName().substring(8));
//            this.resize();
//        }
//        else
//        {
//            if (this.refLabel.getText().length() > 0)
//            {
//                this.refLabel.setText("");
//            }
//        }
    }

    /**
     * Triggers the gamepad tab buttons.
     * @param evt - Input event data.
     */
    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt)
    {
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt)
    {
    }

    @Override
    public void onTouchEvent(TouchEvent evt)
    {
    }

    @Override
    public void onConnected(Joystick joystick)
    {
        this.updateGamepad();
        this.resize();
    }

    @Override
    public void onDisconnected(Joystick joystick)
    {
        /*
         * TODO - This callback will fire before the joystick is actually
         *  removed. This is helpful at times, but it does mean we will have a
         *  blank slot because this code still thinks we have the one that was
         *  removed.
         */
        this.updateGamepad();
        this.resize();
    }
}
