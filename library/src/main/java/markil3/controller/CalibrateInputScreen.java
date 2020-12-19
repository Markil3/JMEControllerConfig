package markil3.controller;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
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
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import java.io.File;
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

/**
 * Provides a series of prompts that will build a controller calibration file.
 * @author Markil3
 * @version 1.1
 */
public class CalibrateInputScreen extends BaseAppState
        implements RawInputListener, ActionListener
{
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(CalibrateInputScreen.class);

    private static final LinkedHashMap<String, String> BUTTON_PROMPTS =
            new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> AXIS_PROMPTS =
            new LinkedHashMap<>();
    final String CLICK_MAPPING = "calibrateButtonClick";

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

    private final File calibrationFile;
    private Node gui;
    protected BitmapFont guiFont;

    private Node introCont;
    private Node startButton;
    private Node mainOptions;
    private Node skipButton;
    private Node cancelButton;
    private Node restartButton;
    private JoystickPreviewScreen.GamepadView gamepad;

    private BitmapText currentJoystick;
    private BitmapText currentElement;
    private BitmapText currentTime;

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

    /**
     * Creates a screen for calibrating and remapping a game controller.
     * @param calibrationFile - The file to store the results in.
     */
    public CalibrateInputScreen(File calibrationFile)
    {
        this.calibrationFile = calibrationFile;
    }

    @Override
    protected void initialize(Application app)
    {
        BitmapText text;

        this.gui = new Node();
        this.guiFont =
                app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        this.introCont = new Node();

        text = this.guiFont.createLabel(
                "This screen will let you properly calibrate your " +
                        "controller\n" +
                        "inputs. When prompted, press the button or axis " +
                        "that\n" +
                        "corresponds to the highlighted button and hold it\n" +
                        "for one seconds. Do not apply any additional " +
                        "inputs\n" +
                        "beyond what is prompted. Press \"Start\" to \n" +
                        "begin.");
        text.setBox(new Rectangle(0, 0, text.getLineWidth(), text.getHeight()));
        text.setAlignment(BitmapFont.Align.Center);
        text.setVerticalAlignment(BitmapFont.VAlign.Center);
        this.introCont.attachChild(text);

        this.startButton =
                GUIUtils.createButton(app.getAssetManager(), this.guiFont, app.getContext().getTouchInput() != null,
                        "start", "Start");
//        this.startButton.addClickCommands(this);
        this.introCont.attachChild(this.startButton);

        this.mainOptions = new Node();

        this.mainOptions.attachChild(this.guiFont.createLabel(
                "First, press any button or axis\n" +
                        "on the controller you want to calibrate."));

        this.skipButton =
                GUIUtils.createButton(app.getAssetManager(), this.guiFont, app.getContext().getTouchInput() != null,
                        "skip", "Skip");

        this.cancelButton =
                GUIUtils.createButton(app.getAssetManager(), this.guiFont, app.getContext().getTouchInput() != null,
                        "cancel", "Cancel");
        this.introCont.attachChild(this.cancelButton);

        this.restartButton =
                GUIUtils.createButton(app.getAssetManager(), this.guiFont, app.getContext().getTouchInput() != null,
                        "close", "Close Application");

        this.gui.attachChild(this.introCont);

        this.gamepad = new JoystickPreviewScreen.GamepadView(null,
                this.getApplication().getAssetManager());

        ((SimpleApplication) this.getApplication()).getGuiNode()
                .attachChild(this.gui);

        app.getInputManager().addMapping(CLICK_MAPPING,
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
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
        app.getInputManager().deleteMapping(CLICK_MAPPING);
        ((SimpleApplication) this.getApplication()).getGuiNode()
                .detachChild(this.gui);
    }

    @Override
    protected void onEnable()
    {
        this.resize();
        this.getApplication().getInputManager()
                .addListener(this, CLICK_MAPPING);
    }

    @Override
    protected void onDisable()
    {
        if (this.listeningRaw)
        {
            this.getApplication().getInputManager()
                    .removeRawInputListener(this);
        }
        this.getApplication().getInputManager().removeListener(this);
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
        float introHeight =
                GUIUtils.alignContainer(this.introCont, width, height);
        float mainHeight =
                GUIUtils.alignContainer(this.mainOptions, width, height);
        this.gui.setLocalTranslation(0, height / 2F, 0);
        this.introCont.setLocalTranslation((width) / 2F, (introHeight) / 2F, 0);
        this.mainOptions
                .setLocalTranslation((width) / 4F, (mainHeight) / 2F, 0);
        this.gamepad.setLocalTranslation(width / 2F, -256, 0);
        if (this.currentJoystick != null)
        {
            this.currentJoystick.setLocalTranslation(
                    (width - this.currentJoystick.getLineWidth()) / 2F,
                    height / 2F, 0);
        }
        if (this.currentElement != null &&
                this.currentElement.getParent() != null)
        {
            this.currentElement.setLocalTranslation(
                    width / 2F - this.currentElement.getLineWidth() - 10,
                    height / 2F - this.currentJoystick.getHeight(), 0);
        }
        if (this.currentTime != null && this.currentTime.getParent() != null)
        {
            this.currentTime.setLocalTranslation(width / 2F + 10,
                    height / 2F - this.currentJoystick.getHeight(), 0);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf)
    {
        String buttonId;
        if (name.equals(CLICK_MAPPING))
        {
            buttonId = GUIUtils.handleButtonPress(this.gui,
                    this.getApplication().getInputManager().getCursorPosition(),
                    isPressed);
            if (!isPressed && buttonId != null)
            {
                switch (buttonId)
                {
                case "start":
                    this.introCont.removeFromParent();
                    this.gui.attachChild(this.mainOptions);
                    this.gui.attachChild(this.gamepad);
                    this.resize();

                    this.listeningRaw = true;
                    this.getApplication().getInputManager()
                            .addRawInputListener(this);
                    break;
                case "skip":
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
                    break;
                case "cancel":
                    this.getStateManager().detach(this);
                    this.getStateManager().attach(new JoystickPreviewScreen());
                    break;
                case "close":
                    this.getApplication().stop();
                    break;
                }
            }
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
        this.currentJoystick =
                this.guiFont.createLabel(this.joystick.getName());
        this.currentJoystick
                .setBox(new Rectangle(0, 0, this.currentJoystick.getLineWidth(),
                        this.currentJoystick.getHeight()));
        this.currentJoystick.setAlignment(BitmapFont.Align.Center);
        this.gui.attachChild(this.currentJoystick);
        this.calibrationIter = BUTTON_PROMPTS.keySet().iterator();
        this.currentElement = this.guiFont.createLabel("");
        this.currentTime = this.guiFont.createLabel("");

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
            this.mainOptions.detachAllChildren();
            this.mainOptions.attachChild(this.guiFont.createLabel("Press the"));
            this.mainOptions.attachChild(this.guiFont
                    .createLabel(BUTTON_PROMPTS.get(this.currentButton)));
            this.mainOptions.attachChild(this.skipButton);
            this.mainOptions.attachChild(this.cancelButton);
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
            this.mainOptions.detachAllChildren();
            this.mainOptions.attachChild(this.guiFont.createLabel("Press the"));
            this.mainOptions.attachChild(this.guiFont.createLabel(
                    (this.currentBias ? "Positive " : "Negative ") +
                            AXIS_PROMPTS.get(this.currentButton)));
            this.mainOptions.attachChild(this.skipButton);
            this.mainOptions.attachChild(this.cancelButton);

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
        /*
         * Enable this in JME 3.4.
         */
        boolean perComponentEnabled = false;
        JoystickAxis axis;
        JoystickButton button;
        Properties props = new Properties();

        this.currentStage = null;
        this.mainOptions.removeFromParent();
        this.gamepad.removeFromParent();

        if (!calibrationFile.exists())
        {
            try
            {
                if (!calibrationFile.createNewFile())
                {
                    throw new IOException("Could not create calibration file.");
                }
            }
            catch (IOException ioe)
            {
                this.introCont.detachAllChildren();
                this.introCont.attachChild(this.guiFont
                        .createLabel("Could not create Calibration File:"));
                this.introCont.attachChild(this.cancelButton);
                this.introCont.attachChild(
                        this.guiFont.createLabel("Error Details:"));
                this.listStack(this.introCont, ioe, 0);
                this.gui.attachChild(this.introCont);
                this.resize();
            }
        }

        try (FileInputStream input = new FileInputStream(calibrationFile))
        {
            props.load(input);
        }
        catch (IOException e)
        {
            logger.error("Could not load calibration file", e);
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
                    calibrationFile))
            {
                props.store(output, "Joystick Calibration File");
                this.introCont.detachAllChildren();
                this.introCont.attachChild(this.guiFont
                        .createLabel("Calibration completed successfully."));
                this.introCont.attachChild(this.guiFont.createLabel(
                        "Close the application and restart to load"));
                this.introCont.attachChild(
                        this.guiFont.createLabel("the new settings."));
                this.introCont.attachChild(this.restartButton);
                this.gui.attachChild(this.introCont);
                this.resize();
            }
            catch (IOException ioe)
            {
                this.introCont.detachAllChildren();
                this.introCont.attachChild(this.guiFont
                        .createLabel("Could not create Calibration File:"));
                this.introCont.attachChild(this.cancelButton);
                this.introCont.attachChild(
                        this.guiFont.createLabel("Error Details:"));
                this.listStack(this.introCont, ioe, 0);
                this.gui.attachChild(this.introCont);
                this.resize();
            }
        }
    }

    /**
     * Display an error stack trace on the screen.
     * @param errorCont - The container to use for the error.
     * @param error - The error.
     * @param depth - How many levels of exceptions throwing exceptions we
     *              are in. This is for recursively calling the method.
     */
    private void listStack(Node errorCont, Throwable error, int depth)
    {
        StringBuilder errorBuilder = new StringBuilder();
        Throwable cause = error;
        while (cause != null)
        {
            errorBuilder.append(error.getClass().getName());
            errorBuilder.append(": ");
            errorBuilder.append(error.getMessage());
            errorBuilder.append('\n');
            for (StackTraceElement stack : error.getStackTrace())
            {
                errorBuilder.append('\t');
                errorBuilder.append(stack.toString());
                errorBuilder.append('\n');
            }
            errorBuilder.append('\n');
            cause = error.getCause();
        }
        errorCont
                .attachChild(this.guiFont.createLabel(errorBuilder.toString()));
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
