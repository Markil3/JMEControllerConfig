# JMEControllerConfig
Derived from jMonkeyEngine's TestJoypad example, this is a simple utility library for JMonkeyEngine-based games that allow for the inclusion of a gamepad view to monitor how exactly the JMonkeyEngine is viewing the gamepad. It can easilly be implemented into any JME3-based application without the need for external dependencies.

## Getting started
Download the [latest release build](https://github.com/Markil3/JMEControllerConfig/releases) and store it in your project's library folder. Point your build tool to use the library. Then, ensure that your jMonkey application's settings (com.jme3.system.AppSettings#setUseJoysticks) are set to enable joypad support After that, just attach the markil3.controller.JoystickPreviewScreen to the jMonkey applications state manager to display the gamepad information. Simply detach the state to remove the information.

### Requirements
* Java 8+
* jMonkeyEngine 3.3+
    * jme3-core
    * "Interface/Joystick/gamepad-buttons.png" from jme3-testdata
    * "Interface/Joystick/gamepad-frame.png" from jme3-testdata
    * "Interface/Joystick/gamepad-stick.png" from jme3-testdata
    * jme3-desktop (if running as standalone)
    * jme3-lwjgl3 or jme3-lwjgl (if running as standalone)
* slf4j-api 1.7.15+

## Building from Source
To build from source, start by downloading the source from github. If you have the [git command line tool](https://git-scm.com/downloads) installed, the following line will download the git repository from github:

<code>git clone https://github.com/Markil3/JMEControllerConfig.git</code>

Alternatively, you can [download the zip file](https://github.com/Markil3/JMEControllerConfig/archive/main.zip) and extract it to your project directory if you don't want the version control data and just want the latest published source build.

Once downloaded, point the command line to the directory that the source was downloaded and extracted to and run

<code>gradlew library:build</code>

This will create three .jar files in the library/build/libs directory, one binary, the second (with the -source suffix) containing the source code, and the final one (with the -javadoc suffix) containing documentation.

## Testing
If you want to test this without implementing it in your software, simply download the source as outlined above and run

<code>gradlew run</code>

This will run a bare-bones version dedicated to the utility.

## FAQ
### Can I use this library in Adroid/iOS?
While I haven't fully tested it out, I foresee no problems. Just be sure to enable joysticks.

Note that on Android at least, the accelerometer is treated as gamepad 0.

## Troubleshooting

### I can't see anything!
There are two reasons that nothing would show up. First of all, the application state simply may not be attached properly to the state manager. Use a debugger to check this. Second, the application state may not detect any controllers in the com.jme3.input.InputManager#joysticks array. Ensure that your gamepads are connected and are registering in jMonkeyEngine.

### Every time I disconnect a controller, its entry is still present in the tab bar
This is a result of the controller disconnect callback being called before the controller is actually removed from the engine.

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
