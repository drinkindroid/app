package com.drinkindroid;

import static org.andengine.extension.physics.box2d.util.constants.PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

import android.graphics.Typeface;
import android.util.Log;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.*;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.shape.IAreaShape;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TickerText;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;

import android.hardware.SensorManager;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

import java.util.Random;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 *
 * @author Nicolas Gramlich
 * @since 18:47:08 - 19.03.2010
 */
public class PhysicsExample extends SimpleBaseGameActivity implements IAccelerationListener, IOnSceneTouchListener {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final int CAMERA_WIDTH = 720;
    private static final int CAMERA_HEIGHT = 480;

    private static final int NUM_SHAPES = 20;

    private Random rand = new Random();

    private static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);

    // ===========================================================
    // Fields
    // ===========================================================

    private BitmapTextureAtlas mBitmapTextureAtlas;

    private TiledTextureRegion mBoxFaceTextureRegion;
    private TiledTextureRegion mCircleFaceTextureRegion;
    private TiledTextureRegion mTriangleFaceTextureRegion;
    private TiledTextureRegion mHexagonFaceTextureRegion;

    private Scene mScene;

    private PhysicsWorld mPhysicsWorld;
    private int mFaceCount = 0;
    private Font mFont;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public EngineOptions onCreateEngineOptions() {
//        Toast.makeText(this, "Touch the screen to add objects.", Toast.LENGTH_LONG).show();

        final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
    }

    @Override
    public void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 64, 128, TextureOptions.BILINEAR);
        this.mBoxFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_box_tiled.png", 0, 0, 2, 1); // 64x32
        this.mCircleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_circle_tiled.png", 0, 32, 2, 1); // 64x32
        this.mTriangleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_triangle_tiled.png", 0, 64, 2, 1); // 64x32
        this.mHexagonFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_hexagon_tiled.png", 0, 96, 2, 1); // 64x32
        this.mBitmapTextureAtlas.load();

        this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, TextureOptions.BILINEAR, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 64, Color.WHITE_ARGB_PACKED_INT);
        this.mFont.load();
    }

    @Override
    public Scene onCreateScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());

        this.mScene = new Scene();
        this.mScene.setBackground(new Background(0, 0, 0));
        this.mScene.setOnSceneTouchListener(this);

        this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

        final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
        final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle roofLeft = new Rectangle(0, 0, CAMERA_WIDTH / 3, 2, vertexBufferObjectManager);
        final Rectangle roofRight = new Rectangle(2 * CAMERA_WIDTH / 3, 0, CAMERA_WIDTH / 3, 2, vertexBufferObjectManager);
        final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
        final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);

        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, roofLeft, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, roofRight, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

        this.mScene.attachChild(ground);
        this.mScene.attachChild(roofLeft);
        this.mScene.attachChild(roofRight);
        this.mScene.attachChild(left);
        this.mScene.attachChild(right);

        for (int i = 0; i < NUM_SHAPES; i++) {
            addFace(rand.nextInt(CAMERA_WIDTH), rand.nextInt(CAMERA_HEIGHT));
        }

        this.mScene.registerUpdateHandler(this.mPhysicsWorld);

        showSpinInText("FREE THE DUDES\n\nGO!");

        IUpdateHandler onScreenHandler = new IUpdateHandler() {

            private float freq = 1.0f;
            private float timeSinceCheck = 0f;
            private float totalTime = 0;

            @Override
            public void onUpdate(float pSecondsElapsed) {
                timeSinceCheck += pSecondsElapsed;
                if (timeSinceCheck > freq) {
                    int foundSprites = 0;
                    for (int i = 0; i < mScene.getChildCount(); i++) {
                        IEntity child = mScene.getChild(i);
                        if (child instanceof AnimatedSprite) {
                            if (child.getY() < -10) {
                                Log.i("DND", "Removed face.");
                                removeFace((AnimatedSprite) child);
                            } else {
                                foundSprites++;
                            }
                        }
                    }
                    if (foundSprites == 0) {
                        Log.w("DND", "--- GAME ENDED ---");
                        showSpinInText("YOU DID IT\n\nIN " + totalTime + "s");
                    } else {
                        totalTime += timeSinceCheck;
                    }
                    timeSinceCheck = 0;
                }
            }

            @Override
            public void reset() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        mScene.registerUpdateHandler(onScreenHandler);

        return this.mScene;
    }

    private void showSpinInText(String message) {
        final Text text = new TickerText(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2, this.mFont, message, new TickerText.TickerTextOptions(HorizontalAlign.CENTER, 20), this.getVertexBufferObjectManager());
        text.setPosition((CAMERA_WIDTH - text.getWidth()) / 2, (CAMERA_HEIGHT - text.getHeight()) / 2);
        text.registerEntityModifier(
                new SequenceEntityModifier(
                        new ParallelEntityModifier(
                                new AlphaModifier(1, 0.0f, 1.0f),
                                new ScaleModifier(1, 0.5f, 1.0f),
                                new RotationModifier(1, -360, 0)
                        ),
                        new AlphaModifier(1, 1.0f, 0.0f)
                )
        );
//        text.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        mScene.attachChild(text);
    }

    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
/*
        if (this.mPhysicsWorld != null) {
            if (pSceneTouchEvent.isActionDown()) {
                this.addFace(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
                return true;
            }
        }
*/
        return false;
    }

    @Override
    public void onAccelerationAccuracyChanged(final AccelerationData pAccelerationData) {

    }

    @Override
    public void onAccelerationChanged(final AccelerationData pAccelerationData) {
        final Vector2 gravity = Vector2Pool.obtain(pAccelerationData.getX(), pAccelerationData.getY());
        this.mPhysicsWorld.setGravity(gravity);
        Vector2Pool.recycle(gravity);
    }

    @Override
    public void onResumeGame() {
        super.onResumeGame();

        this.enableAccelerationSensor(this);
    }

    @Override
    public void onPauseGame() {
        super.onPauseGame();

        this.disableAccelerationSensor();
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private void addFace(final float pX, final float pY) {
        this.mFaceCount++;
        Debug.d("Faces: " + this.mFaceCount);

        final AnimatedSprite face;
        final Body body;

        if (this.mFaceCount % 4 == 0) {
            face = new AnimatedSprite(pX, pY, this.mBoxFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, face, BodyType.DynamicBody, FIXTURE_DEF);
        } else if (this.mFaceCount % 4 == 1) {
            face = new AnimatedSprite(pX, pY, this.mCircleFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsFactory.createCircleBody(this.mPhysicsWorld, face, BodyType.DynamicBody, FIXTURE_DEF);
        } else if (this.mFaceCount % 4 == 2) {
            face = new AnimatedSprite(pX, pY, this.mTriangleFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsExample.createTriangleBody(this.mPhysicsWorld, face, BodyType.DynamicBody, FIXTURE_DEF);
        } else {
            face = new AnimatedSprite(pX, pY, this.mHexagonFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsExample.createHexagonBody(this.mPhysicsWorld, face, BodyType.DynamicBody, FIXTURE_DEF);
        }

        face.animate(200);

        this.mScene.attachChild(face);
        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(face, body, true, true));
    }

    private void removeFace(final AnimatedSprite face) {
        final PhysicsConnector facePhysicsConnector = this.mPhysicsWorld.getPhysicsConnectorManager().findPhysicsConnectorByShape(face);

        this.mPhysicsWorld.unregisterPhysicsConnector(facePhysicsConnector);
        this.mPhysicsWorld.destroyBody(facePhysicsConnector.getBody());

        this.mScene.unregisterTouchArea(face);
        this.mScene.detachChild(face);

        System.gc();
    }

    /**
     * Creates a {@link Body} based on a {@link PolygonShape} in the form of a triangle:
     * <pre>
     *  /\
     * /__\
     * </pre>
     */
    private static Body createTriangleBody(final PhysicsWorld pPhysicsWorld, final IAreaShape pAreaShape, final BodyType pBodyType, final FixtureDef pFixtureDef) {
        /* Remember that the vertices are relative to the center-coordinates of the Shape. */
        final float halfWidth = pAreaShape.getWidthScaled() * 0.5f / PIXEL_TO_METER_RATIO_DEFAULT;
        final float halfHeight = pAreaShape.getHeightScaled() * 0.5f / PIXEL_TO_METER_RATIO_DEFAULT;

        final float top = -halfHeight;
        final float bottom = halfHeight;
        final float left = -halfHeight;
        final float centerX = 0;
        final float right = halfWidth;

        final Vector2[] vertices = {
                new Vector2(centerX, top),
                new Vector2(right, bottom),
                new Vector2(left, bottom)
        };

        return PhysicsFactory.createPolygonBody(pPhysicsWorld, pAreaShape, vertices, pBodyType, pFixtureDef);
    }

    /**
     * Creates a {@link Body} based on a {@link PolygonShape} in the form of a hexagon:
     * <pre>
     *  /\
     * /  \
     * |  |
     * |  |
     * \  /
     *  \/
     * </pre>
     */
    private static Body createHexagonBody(final PhysicsWorld pPhysicsWorld, final IAreaShape pAreaShape, final BodyType pBodyType, final FixtureDef pFixtureDef) {
        /* Remember that the vertices are relative to the center-coordinates of the Shape. */
        final float halfWidth = pAreaShape.getWidthScaled() * 0.5f / PIXEL_TO_METER_RATIO_DEFAULT;
        final float halfHeight = pAreaShape.getHeightScaled() * 0.5f / PIXEL_TO_METER_RATIO_DEFAULT;

        /* The top and bottom vertex of the hexagon are on the bottom and top of hexagon-sprite. */
        final float top = -halfHeight;
        final float bottom = halfHeight;

        final float centerX = 0;

        /* The left and right vertices of the heaxgon are not on the edge of the hexagon-sprite, so we need to inset them a little. */
        final float left = -halfWidth + 2.5f / PIXEL_TO_METER_RATIO_DEFAULT;
        final float right = halfWidth - 2.5f / PIXEL_TO_METER_RATIO_DEFAULT;
        final float higher = top + 8.25f / PIXEL_TO_METER_RATIO_DEFAULT;
        final float lower = bottom - 8.25f / PIXEL_TO_METER_RATIO_DEFAULT;

        final Vector2[] vertices = {
                new Vector2(centerX, top),
                new Vector2(right, higher),
                new Vector2(right, lower),
                new Vector2(centerX, bottom),
                new Vector2(left, lower),
                new Vector2(left, higher)
        };

        return PhysicsFactory.createPolygonBody(pPhysicsWorld, pAreaShape, vertices, pBodyType, pFixtureDef);
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
