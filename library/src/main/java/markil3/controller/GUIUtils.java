package markil3.controller;

import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/**
 * A collection of utility methods for the screens used here.
 * @author Markil 3
 * @version 1.1
 */
public class GUIUtils
{
    /**
     * The color that a button takes on when the mouse is down on it. Feel
     * free to change it to suit your needs.
     */
    static ColorRGBA BUTTON_COLOR_ON = new ColorRGBA(0F, 0.1F, 0F, 1F);
    /**
     * The default color that a button. Feel free to change it to suit your
     * needs.
     */
    static ColorRGBA BUTTON_COLOR_OFF = new ColorRGBA(0F, 0.75F, 0.25F, 1F);

    private static final String BUTTON_ID = "button";

    /**
     * Creates a button. This button will have special user data fields that
     * can be detected by {@link #handleButtonPress(Node, Vector2f, boolean)}
     * to trigger events.
     * @param assets - The application asset manager.
     * @param guiFont - The font to use for the button.
     * @param isMobile - Whether or not the mobile size increase should be used.
     * @param id - The button ID. This is used to identify which button was
     *           pressed later on.
     * @param content - The text that will display in the button.
     * @return A node containing the button elements.
     */
    public static Node createButton(AssetManager assets, BitmapFont guiFont,
                                    boolean isMobile, String id, String content)
    {
        final float MOBILE_SIZE = 30F;
        BitmapText buttonText;
        Geometry buttonBackground;
        Node button = new Node();
        buttonText = guiFont.createLabel(content);
        if (isMobile)
        {
            buttonText.setSize(MOBILE_SIZE);
        }
        buttonText.setUserData(BUTTON_ID, id);
        buttonText.setBox(new Rectangle(0, 0, buttonText.getLineWidth(),
                buttonText.getLineHeight()));
        buttonText.setAlignment(BitmapFont.Align.Center);
        buttonText.setVerticalAlignment(BitmapFont.VAlign.Center);
        buttonBackground = new Geometry("button-" + id,
                new Quad(buttonText.getLineWidth() + 10,
                        buttonText.getHeight() + 5));
        buttonBackground.setMaterial(
                new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md"));
        buttonBackground.getMaterial().setColor("Color", BUTTON_COLOR_OFF);
        buttonBackground.setUserData(BUTTON_ID, id);
        buttonBackground
                .setLocalTranslation(-2.5F, -buttonText.getLineHeight() - 2.5F,
                        0);
        button.setUserData(BUTTON_ID, id);
        button.attachChild(buttonBackground);
        button.attachChild(buttonText);
        return button;
    }

    /**
     * Call this method from a mouse listener to search the provided node for
     * any buttons created with
     * {@link #createButton(AssetManager, BitmapFont, boolean, String, String)} and
     * returns their ID.
     * @param gui - The GUI to search.
     * @param cursor - The position of the mouse cursor.
     * @param isPressed - Whether or not the cursor is pressed. This will
     *                  change how the button is displayed.
     * @return The ID of the button that was clicked, or null if none were
     * clicked.
     */
    public static String handleButtonPress(Node gui, Vector2f cursor,
                                           boolean isPressed)
    {
        final String KEY_CURRENT = "currentButton";
        CollisionResults results;
        Ray ray;
        Geometry button;
        String buttonId = null;

        /*
         * We generally mark buttons on mouse down, and trigger that same
         * button on release. If we haven't even marked a button by the time
         * we release the mouse, we just won't bother.
         */
        if (!isPressed)
        {
            button = gui.getUserData(KEY_CURRENT);
            if (button == null)
            {
                return null;
            }
        }

        results = new CollisionResults();
        ray = new Ray(new Vector3f(cursor.x, cursor.y, 0),
                new Vector3f(0, 0, 1));
        gui.collideWith(ray, results);
        for (CollisionResult result : results)
        {
            button = result.getGeometry();
            buttonId = button.getUserData(BUTTON_ID);
            if (buttonId != null)
            {
                if (isPressed)
                {
                    /*
                     * Trigger the press graphic
                     */
                    button.getMaterial().setColor("Color", BUTTON_COLOR_ON);
                    gui.setUserData(KEY_CURRENT, button);
                    break;
                }
                else
                {
                    /*
                     * If the button we released isn't the same as the button
                     *  we originally pressed, don't trigger either.
                     */
                    button = gui.getUserData(KEY_CURRENT);
                    if (button != null)
                    {
                        if (button.getUserData(BUTTON_ID).equals(buttonId))
                        {
                            break;
                        }
                        else
                        {
                            buttonId = null;
                        }
                    }
                }
            }
        }
        if (!isPressed)
        {
            /*
             * Make sure the originally-clicked button was reset.
             */
            button = gui.getUserData(KEY_CURRENT);
            if (button != null)
            {
                button.getMaterial().setColor("Color", BUTTON_COLOR_OFF);
                gui.setUserData(KEY_CURRENT, null);
            }
        }
        return buttonId;
    }

    /**
     * Aligns the contents in a container so that all {@link BitmapText} and
     * button elements will align in a column.
     * @param cont - The container to align.
     * @param width - The width of the screen.
     * @param height - The height of the screen.
     * @return The total height of the container. This is useful in
     * positioning the container itself.
     */
    public static float alignContainer(Node cont, int width, int height)
    {
        float totalHeight = 0;
        for (Spatial node : cont.getChildren())
        {
            if (node instanceof BitmapText)
            {
                node.setLocalTranslation(
                        -((BitmapText) node).getLineWidth() / 2F, -totalHeight,
                        0);
                totalHeight += ((BitmapText) node).getHeight();
            }
            else if (node.getUserData(BUTTON_ID) != null)
            {
                Quad quad = ((Quad) ((Geometry) ((Node) node).getChild(0))
                        .getMesh());
                node.setLocalTranslation(-quad.getWidth() / 2F,
                        -totalHeight - 10, 0);
                totalHeight += quad.getHeight() + 10;
            }
        }
        return totalHeight;
    }
}
