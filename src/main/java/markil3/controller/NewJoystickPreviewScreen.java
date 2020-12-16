package markil3.controller;

import com.jme3.app.Application;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

class NewJoystickPreviewScreen extends JoystickPreviewScreen
{
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(NewJoystickPreviewScreen.class);

    private Node calibrateButton;

    @Override
    public void initialize(Application app)
    {
        super.initialize(app);
        this.calibrateButton = new Node();
        BitmapText buttonText = this.guiFont.createLabel("Calibrate Gamepad");
        buttonText.setUserData("calibrate", true);
        Geometry buttonBackground = new Geometry("calibrate",
                new Quad(buttonText.getLineWidth() + 10,
                        buttonText.getLineHeight() + 5));
        buttonBackground.setMaterial(new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"));
        buttonBackground.getMaterial()
                .setColor("Color", new ColorRGBA(0F, 0.75F, 0.25F, 1F));
        buttonBackground.setUserData("calibrate", true);
        buttonBackground.setLocalTranslation(0, -buttonText.getLineHeight(), 0);
        this.calibrateButton.attachChild(buttonBackground);
        this.calibrateButton.attachChild(buttonText);
        this.gui.attachChild(this.calibrateButton);
    }

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);
        this.calibrateButton.setLocalTranslation(
                (this.getApplication().getCamera().getWidth() -
                        ((BitmapText) this.calibrateButton.getChild(1))
                                .getLineWidth() - 10) / 2F,
                0, 0);
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt)
    {
        super.onMouseButtonEvent(evt);
        if (!evt.isConsumed())
        {
            if (evt.isPressed() && evt.getButtonIndex() == 0)
            {
                CollisionResults results =
                        this.checkMouse(evt.getX(), evt.getY());
                if (results.size() > 0)
                {
                    for (CollisionResult result : results)
                    {
                        if (result.getGeometry().getUserData("calibrate") !=
                                null)
                        {
                            evt.setConsumed();
                            this.getStateManager().detach(this);
                            logger.info("Calibrating!");
                            break;
                        }
                    }
                }
            }
        }
    }
}
