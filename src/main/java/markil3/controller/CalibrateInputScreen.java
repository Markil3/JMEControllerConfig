package markil3.controller;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BoxLayout;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static markil3.controller.JoystickPreviewScreen.ACTION_BOTTOM;
import static markil3.controller.JoystickPreviewScreen.ACTION_LEFT;
import static markil3.controller.JoystickPreviewScreen.ACTION_RIGHT;
import static markil3.controller.JoystickPreviewScreen.ACTION_TOP;
import static markil3.controller.JoystickPreviewScreen.DPAD_DOWN;
import static markil3.controller.JoystickPreviewScreen.DPAD_LEFT;
import static markil3.controller.JoystickPreviewScreen.DPAD_RIGHT;
import static markil3.controller.JoystickPreviewScreen.DPAD_UP;
import static markil3.controller.JoystickPreviewScreen.L1;
import static markil3.controller.JoystickPreviewScreen.L2;
import static markil3.controller.JoystickPreviewScreen.L3;
import static markil3.controller.JoystickPreviewScreen.R1;
import static markil3.controller.JoystickPreviewScreen.R2;
import static markil3.controller.JoystickPreviewScreen.R3;
import static markil3.controller.JoystickPreviewScreen.SELECT;
import static markil3.controller.JoystickPreviewScreen.START;
import static markil3.controller.Main.CALIBRATION_FILE;

/**
 * Provides a series of prompts that will build a controller calibration file.
 * @author Markil3
 */
public class CalibrateInputScreen extends BaseAppState
        implements Command<Button>, RawInputListener
{
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(CalibrateInputScreen.class);

    private static final LinkedHashMap<String, String> BUTTON_PROMPTS =
            new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> AXIS_PROMPTS =
            new LinkedHashMap<>();
    final ColorRGBA BUTTON_COLOR = new ColorRGBA(0F, 0.1F, 0F, 1F);
    final ColorRGBA HIGHLIGHTED_BUTTON_COLOR =
            new ColorRGBA(0F, 0.75F, 0.25F, 1F);

    static
    {
        BUTTON_PROMPTS.put(ACTION_TOP, "Top Action Button");
        BUTTON_PROMPTS.put(ACTION_RIGHT, "Right Action Button");
        BUTTON_PROMPTS.put(ACTION_BOTTOM, "Bottom Action Button");
        BUTTON_PROMPTS.put(ACTION_LEFT, "Left Action Button");
        BUTTON_PROMPTS.put(L1, "Left Bumper (L1)");
        BUTTON_PROMPTS.put(R1, "Right Bumper (R1)");
        BUTTON_PROMPTS.put(L2, "Left Trigger (L2)");
        BUTTON_PROMPTS.put(R2, "Right Trigger (R2)");
        BUTTON_PROMPTS.put(SELECT, "Back/Select");
        BUTTON_PROMPTS.put(START, "Start");
        BUTTON_PROMPTS.put(L3, "Left Analog Stick Button (L3)");
        BUTTON_PROMPTS.put(R3, "Right Analog Stick Button (R3)");

        AXIS_PROMPTS.put(JoystickAxis.X_AXIS, "Left Analog Stick (Horizontal)");
        AXIS_PROMPTS.put(JoystickAxis.Y_AXIS, "Left Analog Stick (Vertical)");
        AXIS_PROMPTS
                .put(JoystickAxis.Z_AXIS, "Right Analog Stick (Horizontal)");
        AXIS_PROMPTS
                .put(JoystickAxis.Z_ROTATION, "Right Analog Stick (Vertical)");
        AXIS_PROMPTS.put(JoystickAxis.POV_X, "D-Pad (Horizontal)");
        AXIS_PROMPTS.put(JoystickAxis.POV_Y, "D-Pad (Vertical)");
    }

    /**
     * This enum is used to determine whether we are working with buttons or
     * axes.
     */
    private enum PromptStage
    {
        BUTTON(BUTTON_PROMPTS), AXIS(AXIS_PROMPTS);

        private final HashMap<String, String> buttonNames;

        PromptStage(HashMap<String, String> buttonNames)
        {
            this.buttonNames = buttonNames;
        }
    }

    private Node gui;
    protected BitmapFont guiFont;

    private Container introCont;
    private Button startButton;
    private Container mainOptions;
    private Button skipButton;
    private Button cancelButton;
    private Button restartButton;
    private JoystickPreviewScreen.GamepadView gamepad;

    private Label currentJoystick;
    private Label currentElement;
    private Label currentTime;

    private boolean listeningRaw;
    private Joystick joystick;
    private Iterator<String> calibrationIter;
    private String currentButton;
    private boolean currentBias;
    private PromptStage currentStage = null;

    private Object focusedJoyElement;
    private float timeHeld = -1;
    private float focusValue;

    /**
     * Keeps track of whether the L2 and R2 functionalities are controlled by
     * buttons or triggers.
     */
    private boolean triggers2;
    private HashMap<String, Object> maps = new HashMap<>();
    private HashMap<String, Boolean> mapBias = new HashMap<>();

    private HashMap<Object, Float> defaultValues = new HashMap<>();

    private Node createButton(String id, String content)
    {
        BitmapText buttonText;
        Geometry buttonBackground;
        Node button = new Node();
        buttonText = this.guiFont.createLabel(content);
        buttonText.setUserData("button", id);
        buttonBackground = new Geometry("button-" + id,
                new Quad(buttonText.getLineWidth() + 10,
                        buttonText.getLineHeight() + 5));
        buttonBackground.setMaterial(
                new Material(this.getApplication().getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"));
        buttonBackground.getMaterial()
                .setColor("Color", HIGHLIGHTED_BUTTON_COLOR);
        buttonBackground.setUserData("button", id);
        buttonBackground.setLocalTranslation(0, -buttonText.getLineHeight(), 0);
        button.attachChild(buttonBackground);
        button.attachChild(buttonText);
        return button;
    }

    @Override
    protected void initialize(Application app)
    {
        this.gui = new Node();
        this.guiFont =
                app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        this.introCont = new Container();
        this.introCont.setLayout(new BoxLayout(Axis.Y, FillMode.Even));
        this.introCont.addChild(new Label(
                "This screen will let you properly calibrate your controller"));
        this.introCont.addChild(new Label(
                "inputs. When prompted, press the button or axis that"));
        this.introCont.addChild(new Label(
                " corresponds to the highlighted button and hold it "));
        this.introCont.addChild(new Label(
                "for one seconds. Do not apply any additional inputs"));
        this.introCont.addChild(
                new Label(" beyond what is prompted. Press \"Start\" to "));
        this.introCont.addChild(new Label("begin."));

        this.startButton = new Button("Start");
        this.startButton.addClickCommands(this);
        this.introCont.addChild(this.startButton);

        this.mainOptions = new Container();
        this.mainOptions.setLayout(new BoxLayout(Axis.Y, FillMode.Even));
        this.mainOptions.addChild(new Label("First, press any button or axis"));
        this.mainOptions.addChild(
                new Label("on the controller you want to calibrate."));

        this.skipButton = new Button("Skip");
        this.skipButton.addClickCommands(this);

        this.cancelButton = new Button("Cancel");
        this.cancelButton.addClickCommands(this);
        this.introCont.addChild(this.cancelButton);

        this.restartButton = new Button("Close Application");
        this.restartButton.addClickCommands(this);

        this.gui.attachChild(this.introCont);

        this.gamepad = new JoystickPreviewScreen.GamepadView(null,
                this.getApplication().getAssetManager());

        ((SimpleApplication) this.getApplication()).getGuiNode()
                .attachChild(this.gui);
    }

    @Override
    public void update(float tpf)
    {
        super.update(tpf);
        float MAX_HOLD_TIME = 1F;
        super.update(tpf);
        if (this.timeHeld >= 0)
        {
            this.timeHeld += tpf;
            this.currentTime.setText(
                    (int) Math.ceil(MAX_HOLD_TIME - this.timeHeld) +
                            " s left...");
            if (this.timeHeld >= MAX_HOLD_TIME)
            {
                switch (this.currentButton)
                {
                case L2:
                    if (this.focusedJoyElement instanceof JoystickAxis)
                    {
                        this.maps.put(JoystickAxis.LEFT_TRIGGER,
                                this.focusedJoyElement);
                    }
                    else
                    {
                        this.maps.
                                put(this.currentButton, this.focusedJoyElement);
                    }
                    break;
                case R2:
                    if (this.focusedJoyElement instanceof JoystickAxis)
                    {
                        this.maps.put(JoystickAxis.RIGHT_TRIGGER,
                                this.focusedJoyElement);
                    }
                    else
                    {
                        this.maps.
                                put(this.currentButton, this.focusedJoyElement);
                    }
                    break;
                case JoystickAxis.POV_X:
                    if (this.focusedJoyElement instanceof JoystickButton)
                    {
                        this.maps.put(this.focusValue > 0 ? DPAD_RIGHT :
                                DPAD_LEFT, this.focusedJoyElement);
                    }
                    else
                    {
                        this.maps.
                                put(this.currentButton, this.focusedJoyElement);
                    }
                    break;
                case JoystickAxis.POV_Y:
                    if (this.focusedJoyElement instanceof JoystickButton)
                    {
                        this.maps.put(this.focusValue > 0 ? DPAD_UP : DPAD_DOWN,
                                this.focusedJoyElement);
                    }
                    else
                    {
                        this.maps.
                                put(this.currentButton, this.focusedJoyElement);
                    }
                    break;
                default:
                    this.maps.put(this.currentButton, this.focusedJoyElement);
                    break;
                }
                this.mapBias.put(this.currentButton, this.focusValue > 0);
                this.timeHeld = -1;
                this.focusedJoyElement = null;
                this.focusValue = 0;
                if (this.currentStage == PromptStage.BUTTON)
                {
                    this.calibrateNextButton();
                }
                else
                {
                    this.calibrateNextAxis();
                }
            }
        }
    }

    @Override
    protected void cleanup(Application app)
    {
        ((SimpleApplication) this.getApplication()).getGuiNode()
                .detachChild(this.gui);
    }

    @Override
    protected void onEnable()
    {
        this.resize();
    }

    @Override
    protected void onDisable()
    {
        if (this.listeningRaw)
        {
            this.getApplication().getInputManager()
                    .removeRawInputListener(this);
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
        for (Node node : this.introCont.getLayout().getChildren())
        {
            if (node instanceof Label)
            {
                ((Label) node).setTextHAlignment(HAlignment.Center);
            }
        }
        for (Node node : this.mainOptions.getLayout().getChildren())
        {
            if (node instanceof Label)
            {
                ((Label) node).setTextHAlignment(HAlignment.Center);
            }
        }
//        this.gui.setPreferredSize(new Vector3f(width, height, 0));
//        this.gui.setLocalTranslation(0, this.gui.getPreferredSize().y, 0);
        this.introCont.setLocalTranslation(
                (width - this.introCont.getPreferredSize().x) / 2F,
                (height + this.introCont.getPreferredSize().y) / 2F, 0);
        this.mainOptions.setLocalTranslation(
                width / 2F - this.mainOptions.getPreferredSize().x,
                (height + this.mainOptions.getPreferredSize().y) / 2F, 0);
        this.gamepad.setLocalTranslation(width / 2F, height / 2F - 256, 0);
        if (this.currentJoystick != null)
        {
            this.currentJoystick.setLocalTranslation(
                    (width - this.currentJoystick.getPreferredSize().x) / 2F,
                    height, 0);
        }
        if (this.currentElement != null &&
                this.currentElement.getParent() != null)
        {
            this.currentElement.setLocalTranslation(
                    width / 2F - this.currentElement.getPreferredSize().x,
                    height - this.currentJoystick.getPreferredSize().y, 0);
        }
        if (this.currentTime != null && this.currentTime.getParent() != null)
        {
            this.currentTime.setLocalTranslation(width / 2F,
                    height - this.currentJoystick.getPreferredSize().y, 0);
        }
    }

    @Override
    public void execute(Button source)
    {
        if (source == this.startButton)
        {
            this.introCont.removeFromParent();
            this.gui.attachChild(this.mainOptions);
            this.gui.attachChild(this.gamepad);
            this.resize();

            this.listeningRaw = true;
            this.getApplication().getInputManager().addRawInputListener(this);
        }
        else if (source == this.skipButton)
        {
            if (this.currentStage != null)
            {
                this.timeHeld = -1;
                this.focusedJoyElement = null;
                this.focusValue = 0;
                if (this.currentStage == PromptStage.BUTTON)
                {
                    this.calibrateNextButton();
                }
                else
                {
                    this.calibrateNextAxis();
                }
            }
        }
        else if (source == this.cancelButton)
        {
            this.getStateManager().detach(this);
            this.getStateManager().attach(new NewJoystickPreviewScreen());
        }
        else if (source == this.restartButton)
        {
            this.getApplication().stop();
        }
    }

    /**
     * Set which joystick to use for calibration. This change affects all
     * joysticks sharing the same name, so you don't have to run this for
     * every controller you have if some of them are identical.
     * @param joystick - The joystick to use for calibration.
     */
    private void setJoystick(Joystick joystick)
    {
        this.joystick = joystick;
        this.currentJoystick = new Label(this.joystick.getName());
        this.gui.attachChild(this.currentJoystick);
        this.calibrationIter = BUTTON_PROMPTS.keySet().iterator();
        this.currentElement = new Label("");
        this.currentTime = new Label("");

        this.currentStage = PromptStage.BUTTON;
        calibrateNextButton();
    }

    /**
     * Sets up the scene to calibrate the next button on the list. If we have
     * gone through them all, we start on axis.
     */
    private void calibrateNextButton()
    {
        if (this.currentButton != null)
        {
            this.gamepad.setButtonValue(this.currentButton, false);
        }
        this.currentElement.removeFromParent();
        this.currentTime.removeFromParent();

        if (this.calibrationIter.hasNext())
        {
            this.currentButton = this.calibrationIter.next();
            if ((this.currentButton.equals(L2) ||
                    this.currentButton.equals(R2)) && this.triggers2)
            {
                this.calibrateNextButton();
                return;
            }
            this.mainOptions.clearChildren();
            this.mainOptions.addChild(new Label("Press the"));
            this.mainOptions.addChild(
                    new Label(BUTTON_PROMPTS.get(this.currentButton)));
            this.mainOptions.addChild(this.skipButton);
            this.mainOptions.addChild(this.cancelButton);
            this.gamepad.setButtonValue(this.currentButton, true);
            this.resize();
        }
        else
        {
            this.currentBias = true;
            this.currentButton = null;
            this.calibrationIter = AXIS_PROMPTS.keySet().iterator();
            this.currentStage = PromptStage.AXIS;
            this.calibrateNextAxis();
        }
    }

    /**
     * Sets up the scene to calibrate the next button on the list. If we have
     * gone through them all, we record the file.
     */
    private void calibrateNextAxis()
    {
        // Clears the display
        if (this.currentButton != null)
        {
            switch (this.currentButton)
            {
            case JoystickAxis.X_AXIS:
                this.gamepad.setXAxis(0);
                break;
            case JoystickAxis.Y_AXIS:
                this.gamepad.setYAxis(0);
                break;
            case JoystickAxis.Z_AXIS:
                this.gamepad.setZAxis(0);
                break;
            case JoystickAxis.Z_ROTATION:
                this.gamepad.setZRotation(0);
                break;
            case JoystickAxis.POV_X:
                if (this.currentBias)
                {
                    this.gamepad.
                            setButtonValue(DPAD_RIGHT, false);
                }
                else
                {
                    this.gamepad.setButtonValue(DPAD_LEFT, false);
                }
                break;
            case JoystickAxis.POV_Y:
                if (this.currentBias)
                {
                    this.gamepad.setButtonValue(DPAD_UP, false);
                }
                else
                {
                    this.gamepad.setButtonValue(DPAD_DOWN, false);
                }
                break;
            case JoystickAxis.LEFT_TRIGGER:
                this.gamepad.setButtonValue(L2, false);
                break;
            case JoystickAxis.RIGHT_TRIGGER:
                this.gamepad.setButtonValue(R2, false);
                break;
            }
        }
        this.currentElement.removeFromParent();
        this.currentTime.removeFromParent();

        // Increments the counter
        if (!this.currentBias)
        {
            this.currentBias = true;
        }
        else if (this.calibrationIter.hasNext())
        {
            this.currentBias = false;
            this.currentButton = this.calibrationIter.next();
            if (this.currentButton.equals(JoystickAxis.LEFT_TRIGGER) ||
                    this.currentButton.equals(JoystickAxis.RIGHT_TRIGGER))
            {
                this.currentBias = true;
                if (!this.triggers2)
                {
                    this.calibrateNextAxis();
                    return;
                }
            }
        }
        else
        {
            this.currentButton = null;
        }

        if (this.currentButton != null)
        {
            this.mainOptions.clearChildren();
            this.mainOptions.addChild(new Label("Press the"));
            this.mainOptions.addChild(new Label(
                    (this.currentBias ? "Positive " : "Negative ") +
                            AXIS_PROMPTS.get(this.currentButton)));
            this.mainOptions.addChild(this.skipButton);
            this.mainOptions.addChild(this.cancelButton);

            // Updates the display
            switch (this.currentButton)
            {
            case JoystickAxis.X_AXIS:
                this.gamepad.setXAxis(this.currentBias ? 1 : -1);
                break;
            case JoystickAxis.Y_AXIS:
                this.gamepad.setYAxis(this.currentBias ? 1 : -1);
                break;
            case JoystickAxis.Z_AXIS:
                this.gamepad.setZAxis(this.currentBias ? 1 : -1);
                break;
            case JoystickAxis.Z_ROTATION:
                this.gamepad.setZRotation(this.currentBias ? 1 : -1);
                break;
            case JoystickAxis.POV_X:
                if (this.currentBias)
                {
                    this.gamepad.setButtonValue(DPAD_RIGHT, true);
                }
                else
                {
                    this.gamepad.setButtonValue(DPAD_LEFT, true);
                }
                break;
            case JoystickAxis.POV_Y:
                if (this.currentBias)
                {
                    this.gamepad.setButtonValue(DPAD_UP, true);
                }
                else
                {
                    this.gamepad.setButtonValue(DPAD_DOWN, true);
                }
                break;
            case JoystickAxis.LEFT_TRIGGER:
                this.gamepad.setButtonValue(L2, true);
                break;
            case JoystickAxis.RIGHT_TRIGGER:
                this.gamepad.setButtonValue(R2, true);
                break;
            }
            this.resize();
        }
        else
        {
            this.recordFile();
        }
    }

    /**
     * Saves the calibration settings to the file and prompts for an
     * application restart.
     */
    private void recordFile()
    {
        boolean perComponentEnabled = false;
        JoystickAxis axis;
        JoystickButton button;
        Properties props = new Properties();

        this.currentStage = null;
        this.mainOptions.removeFromParent();
        this.gamepad.removeFromParent();

        if (!CALIBRATION_FILE.exists())
        {
            try
            {
                if (!CALIBRATION_FILE.createNewFile())
                {
                    throw new RuntimeException("Could not create calibration file.");
                }
            }
            catch (IOException ioe)
            {
                this.introCont.clearChildren();
                this.introCont.addChild(
                        new Label("Could not create Calibration File:"));
                this.introCont.addChild(this.cancelButton);
                this.introCont.addChild(new Label("Error Details:"));
                this.listStack(this.introCont, ioe, 0);
                this.gui.attachChild(this.introCont);
                this.resize();
            }
        }

        try (FileInputStream input = new FileInputStream(CALIBRATION_FILE))
        {
            props.load(input);
        }
        catch (IOException e)
        {
            logger.error("Could not load configuration file.", e);
        }
        finally
        {
            for (Map.Entry<String, Object> calibrationEntry : this.maps
                    .entrySet())
            {
                if (calibrationEntry.getValue() instanceof JoystickButton)
                {
                    button = (JoystickButton) calibrationEntry.getValue();
                    if (!button.getName().equals(calibrationEntry.getKey()))
                    {
                        props.put((perComponentEnabled ? "button." : "") +
                                this.joystick.getName() + "." +
                                button.getName(), calibrationEntry.getKey());
                    }
                }
                else if (calibrationEntry.getValue() instanceof JoystickAxis)
                {
                    axis = (JoystickAxis) calibrationEntry.getValue();
                    if (!axis.getName().equals(calibrationEntry.getKey()))
                    {
                        props.put((perComponentEnabled ? "axis." : "") +
                                        this.joystick.getName() + "." + axis.getName(),
                                calibrationEntry.getKey());
                    }
                }
            }
            try (FileOutputStream output = new FileOutputStream(
                    CALIBRATION_FILE))
            {
                props.store(output, "Joystick Calibration File");
            }
            catch (IOException ioe)
            {
                this.introCont.clearChildren();
                this.introCont.addChild(
                        new Label("Could not create Calibration File:"));
                this.introCont.addChild(this.cancelButton);
                this.introCont.addChild(new Label("Error Details:"));
                this.listStack(this.introCont, ioe, 0);
                this.gui.attachChild(this.introCont);
                this.resize();
            }
            this.introCont.clearChildren();
            this.introCont
                    .addChild(new Label("Calibration completed successfully."));
            this.introCont.addChild(
                    new Label("Close the application and restart to load"));
            this.introCont.addChild(new Label("the new settings."));
            this.introCont.addChild(this.restartButton);
            this.gui.attachChild(this.introCont);
            this.resize();
        }
    }

    /**
     * Display an error stack trace on the screen.
     * @param errorCont - The container to use for the error.
     * @param error - The error.
     * @param depth - How many levels of exceptions throwing exceptions we
     *              are in. This is for recursively calling the method.
     */
    private void listStack(Container errorCont, Throwable error, int depth)
    {
        StringBuilder depthStringBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++)
        {
            depthStringBuilder.append("\t");
        }
        String depthString = depthStringBuilder.toString();

        errorCont.addChild(new Label(depthString + error.getMessage()));
        for (StackTraceElement stack : error.getStackTrace())
        {
            errorCont
                    .addChild(new Label(depthString + "\t" + stack.toString()));
        }
        if (error.getCause() != null)
        {
            this.listStack(errorCont, error.getCause(), depth + 1);
        }
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt)
    {
//        this.setJoystick(evt.getAxis().getJoystick());
        if (this.joystick != null)
        {
            if (evt.getValue() != 0 && Math.abs(evt.getValue()) > 0.5 &&
                    (this.defaultValues.get(evt.getAxis()) == null || Math.abs(
                            this.defaultValues.get(evt.getAxis()) -
                                    evt.getValue()) > 0.001F))
            {
                if (this.focusedJoyElement != evt.getAxis() ||
                        this.focusValue != evt.getValue())
                {
                    this.focusedJoyElement = evt.getAxis();
                    this.timeHeld = 0;
                    this.focusValue = evt.getValue();
                    this.currentElement.setText(
                            (this.focusValue > 0 ? "+ " : "- ") +
                                    evt.getAxis().getName());
                    this.gui.attachChild(this.currentElement);
                    this.gui.attachChild(this.currentTime);
                    this.resize();
                }
            }
            else
            {
                if (this.focusedJoyElement == evt.getAxis())
                {
                    this.focusedJoyElement = null;
                    this.timeHeld = -1;
                    this.focusValue = 0;
                    this.currentElement.removeFromParent();
                    this.currentTime.removeFromParent();
                }
            }
        }
        else
        {
            this.defaultValues.put(evt.getAxis(), evt.getValue());
        }
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt)
    {
        if (this.joystick != null)
        {
            if (evt.isPressed() &&
                    (this.defaultValues.get(evt.getButton()) == null ||
                            Math.abs(this.defaultValues.get(evt.getButton()) -
                                    1F) > 0.001F))
            {
                if (this.focusedJoyElement != evt.getButton() ||
                        this.focusValue != 1F)
                {
                    this.focusedJoyElement = evt.getButton();
                    this.timeHeld = 0;
                    this.focusValue = 1;
                    this.currentElement.setText(evt.getButton().getName());
                    this.gui.attachChild(this.currentElement);
                    this.gui.attachChild(this.currentTime);
                    this.resize();
                }
            }
            else
            {
                if (this.focusedJoyElement == evt.getButton())
                {
                    this.focusedJoyElement = null;
                    this.timeHeld = -1;
                    this.focusValue = 0;
                    this.currentElement.removeFromParent();
                    this.currentTime.removeFromParent();
                }
            }
        }
        else
        {
            this.defaultValues
                    .put(evt.getButton(), evt.isPressed() ? 1.0F : 0.0F);
        }
        if (!evt.isPressed())
        {
            if (this.joystick == null)
            {
                this.setJoystick(evt.getButton().getJoystick());
            }
        }
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
    public void onMouseMotionEvent(MouseMotionEvent evt)
    {

    }

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
}
