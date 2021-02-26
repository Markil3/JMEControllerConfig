# JMEControllerConfig
Derived from jMonkeyEngine's TestJoypad example, this is a simple utility library for JMonkeyEngine-based games that allow for the inclusion of in-game utilities to manage and configure game controllers in your game. The library can easily be implemented into any JME3-based application without the need for additional dependencies besides what you likely already have.

There are two screens included in the library. The JoystickPreviewScreen state will display the controllers currently connected to the computer (and update it as controllers are connected and disconnected on properly configured applications), as well as the values of all their inputs.

The CalibrateInputScreen state will show a series of prompts that will guide users through the process of creating a controller remapping properties file compatible with com.jme3.input.JoystickCompatibilityMappings.

## Getting started
Download the [latest release build](https://github.com/Markil3/JMEControllerConfig/releases) and store it in your project's library folder. Point your build tool to use the library. Then, ensure that your jMonkey application's settings (com.jme3.system.AppSettings#setUseJoysticks) are set to enable joypad support After that, just attach the markil3.controller.JoystickPreviewScreen to the jMonkey applications state manager to display the gamepad information, or attach CalibrateInputScreen with a link to the output file to configure gamepads. Simply detach the state you used to remove the information.

If you need examples as to how to implement these, see the :desktop submodule.

If you wish to change the colors of the buttons, simply change the color values in the markil3.controller.GUIUtils class.

### Requirements
* Java 8+
* jMonkeyEngine 3.3+
    * jme3-core
    * "Interface/Joystick/gamepad-buttons.png" from jme3-testdata
    * "Interface/Joystick/gamepad-frame.png" from jme3-testdata
    * "Interface/Joystick/gamepad-stick.png" from jme3-testdata
    * jme3-desktop (if running as standalone)
    * jme3-lwjgl3 or jme3-lwjgl (if running as standalone)
    * jme3-android (if running on Android)
    * jme3-android-natives (if running on Android)
* slf4j-api 1.7.30+

## Building from Source
To build from source, start by downloading the source from github. If you have the [git command line tool](https://git-scm.com/downloads) installed, the following line will download the git repository from github:

<code>git clone https://github.com/Markil3/JMEControllerConfig.git</code>

Alternatively, you can [download the zip file](https://github.com/Markil3/JMEControllerConfig/archive/main.zip) and extract it to your project directory if you don't want the version control data and just want the latest published source build.

Once downloaded, point the command line to the directory that the source was downloaded and extracted to and run

<code>gradlew library:build</code>

This will create three .jar files in the library/build/libs directory, one binary, the second (with the -source suffix) containing the source code, and the final one (with the -javadoc suffix) containing documentation.

## Testing
If you want to test this without implementing it in your software, simply download the source as outlined above and run

<code>gradlew desktop:run</code>

This will run a bare-bones version dedicated to the utility using LWJGL3.

Alternatively, you can use the :desktopLegacy subproject for LWJGL2, or the :android subproject for testing on Android.

## Troubleshooting

### I can't see anything!
There are two reasons that nothing would show up. First of all, the application state simply may not be attached properly to the state manager. Use a debugger to check this. Second, the application state may not detect any controllers in the com.jme3.input.InputManager#joysticks array. Ensure that your gamepads are connected and are registering in jMonkeyEngine.

### Every time I disconnect a controller, its entry is still present in the tab bar
This is a result of the controller disconnect callback being called before the controller is actually removed from the engine. This is a limitation of the jMonkey API.

### My application crashes every time I attach it!
Be sure to enable joystick support in the application settings.

    public static void main(String[] args)
    {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("My Game");
        settings.setUseJoysticks(true); // THIS IS IMPORTANT
        settings.setEmulateMouse(true);
        settings.setVSync(true);
        app.setSettings(settings);
        app.start();
    }

### Connecting/Pressing some buttons on Android causes crashes
Support for gamepads on Android appear to be a little spotty. This again is a limitation of the jmonkey engine. https://github.com/jMonkeyEngine/jmonkeyengine/issues/1448

### Some buttons show up under two mappings
This is a quirk with some gamepads. At the moment, this can interfere with the automapper. Workarounds are forthcoming.

### The triggers are always active during remapping
Many triggers are set to -1.0 in their default position. This can sometimes cause confusion to the automapper. Workarounds are forthcoming.
