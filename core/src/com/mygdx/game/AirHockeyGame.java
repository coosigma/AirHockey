package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;


import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static sun.audio.AudioPlayer.player;

/*
 * The main class of this game application.
 */
public class AirHockeyGame extends ApplicationAdapter implements InputProcessor {

	// Declare variables
	static float FPS = 1f/60f;
	static float PUCK_RADIUS = 15;
	static float STRIKER_RADIUS = 25;
	static int MAX_GOALS = 5;
	private int WINNER = 0;

	private Sound hitSound, dropSound;

	SpriteBatch batch;
	Sprite sprPuck,sprStriker, sprKeeper;
	Texture imgPuck, imgStriker, imgKeeper;
	World world;
	Body puck,striker, keeper;
	Body bodyEdgeScreenUp, bodyEdgeScreenDown, bodyEdgeScreenLeft, bodyEdgeScreenRight;

	Matrix4 debugMatrix;
	Box2DDebugRenderer debugRenderer;
	OrthographicCamera camera;

	private Vector2 RIVAL_POST1, RIVAL_POST2, PLAYER_POST1, PLAYER_POST2;

	int playerScore = 0;
	int rivalScore = 0;

	private Texture background, restart;
	private float restartX, restartY, restartW, restartH;
	private BitmapFont score, message;

	private int debug = 0;
	private int start = 1;

	final float PIXELS_TO_METERS = 100f;

	// Variables for touch events
	private Vector2 touchPos = new Vector2();
	private Vector2 dragPos = new Vector2();
	private Vector2 movingVector = new Vector2();
	private boolean isTouched = false;

	/*
	 * Function: create: to create things when the application is loading
	 */
	@Override
	public void create() {
		batch = new SpriteBatch();

		// Load Sounds
		hitSound = Gdx.audio.newSound(Gdx.files.local("data/hit.wav"));
		dropSound = Gdx.audio.newSound(Gdx.files.internal("data/drop.wav"));

		// Load Images
        background = new Texture("field4v3.png");
		restart = new Texture("restart.png");
		restartX = Gdx.graphics.getWidth()/2*0.82f;
		restartY = Gdx.graphics.getHeight()/2 * 0.77f;
		restartW = 30;
		restartH = 30;
		score = new BitmapFont();
		score.getData().setScale(2,2);
		score.setColor(Color.BROWN);
		message = new BitmapFont();
		message.getData().setScale(5,5);
		message.setColor(Color.ROYAL);
		imgPuck = new Texture("puck.png");
		imgStriker = new Texture("striker.png");
		imgKeeper = new Texture("striker.png");
		// Create two identical sprites slightly offset from each other vertically
		sprPuck = new Sprite(imgPuck);
		sprPuck.setPosition(-sprPuck.getWidth()/2,-sprPuck.getHeight()/2);
		sprStriker = new Sprite(imgStriker);
		sprStriker.setPosition(200,-sprStriker.getHeight()/2);
		sprKeeper = new Sprite(imgKeeper);
		sprKeeper.setPosition(-300,-sprKeeper.getHeight()/2);
		float w = Gdx.graphics.getWidth()/PIXELS_TO_METERS;
		float h = Gdx.graphics.getHeight()/PIXELS_TO_METERS;

		// The position of the goals
		if (RIVAL_POST1 == null) {
			float posi_y = h*0.2564f*0.5f + PUCK_RADIUS/PIXELS_TO_METERS-0.2f;
			float nega_y = posi_y * -1;
			float posi_x = w / 2 - 0.3f;
			float nega_x = -1 * posi_x;
			RIVAL_POST1 = new Vector2(nega_x, posi_y);
			RIVAL_POST2 = new Vector2(nega_x, nega_y);
			PLAYER_POST1 = new Vector2(posi_x, posi_y);
			PLAYER_POST2 = new Vector2(posi_x, nega_y);
		}

		// Set the world
		world = new World(new Vector2(0f, 0f),false);

		// Sprite Puck's Physics body
		BodyDef PuckBodyDef = new BodyDef();
		PuckBodyDef.type = BodyDef.BodyType.DynamicBody;
		PuckBodyDef.position.set((sprPuck.getX() + sprPuck.getWidth()/2) /
						PIXELS_TO_METERS,
				(sprPuck.getY() + sprPuck.getHeight()/2) / PIXELS_TO_METERS);


		puck = world.createBody(PuckBodyDef);

		// Sprite Striker's physics body
		BodyDef StrikerBodyDef = new BodyDef();
		StrikerBodyDef.type = BodyDef.BodyType.DynamicBody;
		StrikerBodyDef.position.set((sprStriker.getX() + sprStriker.getWidth()/2) /
						PIXELS_TO_METERS,
				(sprStriker.getY() + sprStriker.getHeight()/2) / PIXELS_TO_METERS);
		StrikerBodyDef.fixedRotation = true;

		striker = world.createBody(StrikerBodyDef);

		// Keeper
		BodyDef KeeperBodyDef = new BodyDef();
		KeeperBodyDef.type = BodyDef.BodyType.DynamicBody;
		KeeperBodyDef.position.set((sprKeeper.getX() + sprKeeper.getWidth()/2) /PIXELS_TO_METERS,
				(sprKeeper.getY() + sprKeeper.getHeight()/2) / PIXELS_TO_METERS);
		KeeperBodyDef.fixedRotation = true;
		keeper = world.createBody(KeeperBodyDef);


		CircleShape circleP = new CircleShape();
		circleP.setRadius(PUCK_RADIUS/PIXELS_TO_METERS);
		CircleShape circleS = new CircleShape();
		circleS.setRadius(STRIKER_RADIUS/PIXELS_TO_METERS);

		// Sprite Puck
		FixtureDef fixtureDefP = new FixtureDef();
		fixtureDefP.shape = circleP;
		fixtureDefP.density = 0.2f;
		fixtureDefP.restitution = 0.5f;
		fixtureDefP.friction = 0f;


		// Sprite Striker
		FixtureDef fixtureDefS = new FixtureDef();
		fixtureDefS.shape = circleS;
		fixtureDefS.density = 1f;
		fixtureDefS.restitution = 0.01f;
		fixtureDefS.friction = 0.1f;

		// Sprite Striker
		FixtureDef fixtureDefK = new FixtureDef();
		fixtureDefK.shape = circleS;
		fixtureDefK.density = 1f;
		fixtureDefK.restitution = 0.01f;
		fixtureDefK.friction = 0.1f;

		puck.createFixture(fixtureDefP);
		striker.createFixture(fixtureDefS);
		keeper.createFixture(fixtureDefK);

		circleP.dispose();
		circleS.dispose();

		// Now the physics body of the bottom edge of the screen

		bodyEdgeScreenUp = createEdge(-w/2,h/2*0.95f,w/2,h/2*0.95f);
		bodyEdgeScreenDown = createEdge(-w/2,-h/2*0.95f,w/2,-h/2*0.95f);
		bodyEdgeScreenLeft = createEdge(-w/2*0.98f,h/2,-w/2*0.98f,-h/2);
		bodyEdgeScreenRight = createEdge(w/2*0.98f,h/2,w/2*0.98f,-h/2);

		Gdx.input.setInputProcessor(this);

		debugRenderer = new Box2DDebugRenderer();
		camera = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());

		// To detect contact event
		world.setContactListener(new ContactListener() {
			// Give a impulse to puck when it is hit
			@Override
			public void beginContact(Contact contact) {
				// Check to see if the collision is between the second sprite and the bottom of the screen
				// If so apply a random amount of upward force to both objects... just because
				if((contact.getFixtureA().getBody() == puck &&
						contact.getFixtureB().getBody() == striker)
						||
						(contact.getFixtureA().getBody() == striker &&
								contact.getFixtureB().getBody() == puck))
				{
					hitSound.play();
					float impact_x = (puck.getPosition().x + striker.getPosition().x) / 2;
					float impact_y = (puck.getPosition().y + striker.getPosition().y) / 2;
					float m = striker.getMass();
					Vector2 v = movingVector;
					float impulse_x = m * v.x * FPS;
					float impulse_y = m * v.y * FPS;
					puck.applyLinearImpulse(impulse_x, impulse_y, impact_x, impact_y, true);
				} else if ((contact.getFixtureA().getBody() == puck)
						||
						(contact.getFixtureB().getBody() == puck)) {
					hitSound.play();
				}
			}

			@Override
			public void endContact(Contact contact) {
			}

			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
			}

			// Prevent rebounding the striker
			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
				if(contact.getFixtureB().getBody() == striker
						||
						contact.getFixtureA().getBody() == striker)
				{
					Vector2 v = striker.getLinearVelocity();
					striker.setLinearVelocity(0, 0);
				}
			}
		});
	}

	/*
	 * Function: createEdge: to create things when the application is loading
	 * @params: float startX, startY, endX, endY
	 */
	private Body createEdge(float v1x, float v1y, float v2x, float v2y) {
		BodyDef bodyDefWall = new BodyDef();
		bodyDefWall.type = BodyDef.BodyType.StaticBody;

		bodyDefWall.position.set(0,0);
		FixtureDef fixtureDefWall = new FixtureDef();

		EdgeShape edgeShape = new EdgeShape();
		edgeShape.set(v1x,v1y,v2x,v2y);
		fixtureDefWall.shape = edgeShape;

		Body body = world.createBody(bodyDefWall);
		body.createFixture(fixtureDefWall);
		edgeShape.dispose();
		return body;
	}


	/*
	 * Function: keeperMove: to move the keeper by the strategy
	 */
	private void keeperMove() {
		if (keeper != null)
		{
			// chasing
			float ballX = puck.getPosition().x;
			float ballY = puck.getPosition().y;
			float keeperX = keeper.getPosition().x;
			float keeperY = keeper.getPosition().y;
			Vector2 ballV = puck.getLinearVelocity();
			float moveX = (ballX - keeperX) * FPS + ballV.x;
			float moveY = (ballY - keeperY) * FPS + ballV.y;
			if (start == 1) {
				keeper.applyForceToCenter(moveX, moveY, true);
			}

			Gdx.app.setLogLevel(Application.LOG_DEBUG);

			// Show lines if in the debug mode
			if (debug == 1) {
				ShapeRenderer sr = new ShapeRenderer();
				sr.setColor(Color.BLACK);
				sr.setProjectionMatrix(camera.combined);
				sr.begin(ShapeRenderer.ShapeType.Filled);
				sr.rectLine(keeperX * PIXELS_TO_METERS, keeperY * PIXELS_TO_METERS, moveX * PIXELS_TO_METERS, moveY * PIXELS_TO_METERS, 2);
				sr.end();
			}
		}
	}

	/*
	 * Function: checkGoal: to check whether is goal or not
	 */
	private void checkGoal() {
		if (puck != null) {
			float puckX = puck.getPosition().x;
			float puckY = puck.getPosition().y;
			float w = Gdx.graphics.getWidth() / PIXELS_TO_METERS;
			float h = Gdx.graphics.getHeight() / PIXELS_TO_METERS;

			//if (abs(puckY) - PUCK_RADIUS/PIXELS_TO_METERS+0.2 <= h*0.2564*0.5) {
			if (abs(puckY)  <= RIVAL_POST1.y) {
				//if (puckX > w / 2 - 0.3f) {
				if (puckX > PLAYER_POST1.x) {
					if (start == 1) {
						dropSound.play();
						rivalScore++;
					}
					puck.setTransform(0, 0, 0);
					puck.setLinearVelocity(0, 0);
				}
				//if (puckX < -w / 2 + 0.3f) {
				if (puckX < RIVAL_POST2.x)
				{
					if (start == 1) {
						dropSound.play();
						playerScore++;
					}
					puck.setTransform(0, 0, 0);
					puck.setLinearVelocity(0, 0);
				}
			}

			if (rivalScore >= MAX_GOALS ) {
				gameOver("rival");
			}
			else if (playerScore >= MAX_GOALS) {
				gameOver("player");
			}
		}
	}

	/*
	 * Function: gameOver: to stop the game and show who wins
	 * @params String winner
	 */
	public void gameOver(String winner) {
	    resetGame(false);
		start = 0;
		if (winner == "player") {
			WINNER = 1;
		} else {
			WINNER = 2;
		}
	}


	/*
	 * Function: render: the main draw loop
	 */
	@Override
	public void render() {
		camera.update();
		// Step the physics simulation forward at a rate of 60hz
		world.step(FPS, 30, 30);

		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		//checkGoal();

		sprPuck.setPosition((puck.getPosition().x * PIXELS_TO_METERS) - sprPuck.getWidth()/2 ,
				(puck.getPosition().y * PIXELS_TO_METERS) -sprPuck.getHeight()/2 );
		sprPuck.setRotation((float)Math.toDegrees(puck.getAngle()));
		sprStriker.setPosition((striker.getPosition().x * PIXELS_TO_METERS) - sprStriker.
						getWidth()/2 ,
				(striker.getPosition().y * PIXELS_TO_METERS) -sprStriker.getHeight()/2 );
		sprStriker.setRotation((float)Math.toDegrees(striker.getAngle()));
		sprKeeper.setPosition((keeper.getPosition().x * PIXELS_TO_METERS) - sprKeeper.
						getWidth()/2 ,
				(keeper.getPosition().y * PIXELS_TO_METERS) -sprKeeper.getHeight()/2 );
		sprKeeper.setRotation((float)Math.toDegrees(keeper.getAngle()));

		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(camera.combined);
		debugMatrix = batch.getProjectionMatrix().cpy().scale(PIXELS_TO_METERS,
				PIXELS_TO_METERS, 0);
		batch.begin();
        batch.draw(background, -Gdx.graphics.getWidth()/2, -Gdx.graphics.getHeight()/2, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.draw(sprPuck, sprPuck.getX(), sprPuck.getY(),sprPuck.getOriginX(),
				sprPuck.getOriginY(),
				sprPuck.getWidth(),sprPuck.getHeight(),sprPuck.getScaleX(),sprPuck.
						getScaleY(),sprPuck.getRotation());
		batch.draw(sprStriker, sprStriker.getX(), sprStriker.getY(),sprStriker.getOriginX(),
				sprStriker.getOriginY(),
				sprStriker.getWidth(),sprStriker.getHeight(),sprStriker.getScaleX(),sprStriker.
						getScaleY(),sprStriker.getRotation());
		batch.draw(sprKeeper, sprKeeper.getX(), sprKeeper.getY(),sprKeeper.getOriginX(),
				sprKeeper.getOriginY(),
				sprKeeper.getWidth(),sprKeeper.getHeight(),sprKeeper.getScaleX(),sprKeeper.
						getScaleY(),sprKeeper.getRotation());
		score.draw(batch, rivalScore+" : "+playerScore, -30, h/2 - 20);
		batch.draw(restart, restartX, restartY, restartW, restartH);
		if (WINNER == 1) {
			message.draw(batch, "You Win!", -140, 50);
		} else if (WINNER == 2) {
			message.draw(batch, "You Lose!", -140, 50);
		}

		batch.end();
		keeperMove();
		checkGoal();

		//debugRenderer.render(world, debugMatrix);
	}

	/*
	 * Function: dispose: to dispose resources
	 */
	@Override
	public void dispose() {
		imgPuck.dispose();
		imgStriker.dispose();
		imgKeeper.dispose();
		background.dispose();
		restart.dispose();
		world.dispose();
	}

	/*
	 * Function: keyDown: to react with user input
	 * @params int keycode
	 */
	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.SPACE) // reset strikers
		{
			if (striker != null) {
				striker.setTransform(200/PIXELS_TO_METERS, 0, 0);
				striker.setLinearVelocity(0,0);
				keeper.setTransform(-300/PIXELS_TO_METERS, 0, 0);
				keeper.setLinearVelocity(0,0);
			}
		} else if (keycode == Input.Keys.R) { // reset puck
			if (puck != null && keeper != null) {
				puck.setTransform(100/PIXELS_TO_METERS, 0, 0);
				puck.setLinearVelocity(0,0);
			}
		} else if (keycode == Input.Keys.D) { // set into debug mode (show lines)
			debug = (debug == 1)? 0 : 1;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	/*
	 * Function: resetGame: to reset the game
	 * @params boolean clearSocre (clear teh score is needed or not)
	 */
	public void resetGame(boolean clearScore) {
		striker.setTransform(200/PIXELS_TO_METERS, 0, 0);
		striker.setLinearVelocity(0,0);
		keeper.setTransform(-300/PIXELS_TO_METERS, 0, 0);
		keeper.setLinearVelocity(0,0);
		puck.setTransform(100/PIXELS_TO_METERS, 0, 0);
		puck.setLinearVelocity(0,0);
		if (clearScore) {
			playerScore = 0;
			rivalScore = 0;
		}
		start = 1;
		WINNER = 0;
	}

	/*
	 * Function: touchDown: to react with touch down event
	 * @params int screenX, int screenY, int pointer, int button
	 */
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		Vector3 point = camera.unproject(new Vector3(screenX, screenY, 0));
		//Vector3 pnt = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		//createCircle(point.x, point.y,random.nextInt(emoticons.size()-1));
		if (striker != null) { // Control the striker
			for (Fixture fixture : striker.getFixtureList()) {
				Shape.Type type = fixture.getType();
				if (type == Shape.Type.Circle) {
					CircleShape circle = (CircleShape) fixture.getShape();
					Vector2 t = new Vector2();
					t.set(circle.getPosition());
					striker.getTransform().mul(t);
					Vector3 pos = new Vector3(t.x, t.y, 0);
					Vector3 world_p = new Vector3(point.x/PIXELS_TO_METERS, point.y/PIXELS_TO_METERS, 0);
					double dis = pos.dst(world_p);
					float myRadius = circle.getRadius();
					boolean intersect = (dis <= myRadius);
					if (intersect) {
						isTouched = true;
						touchPos.set(point.x, point.y);
					}
				}
			}
		}
		// When clicking reset button
		if (point.x >= restartX && point.x <= restartX+restartW) {
			if (point.y >= restartY && point.y <= restartY+restartH) {
				resetGame(true);
			}
		}

		return true;
	}

	/*
	 * Function: touchDown: to react with touch up event
	 * @params int screenX, int screenY, int pointer, int button
	 */
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		isTouched = false;
		return true;
	}

	/*
	 * Function: touchDown: to react with dragged down event ( to move the position of the striker)
	 * @params int screenX, int screenY, int pointer
	 */
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		Vector3 point = camera.unproject(new Vector3(screenX, screenY, 0));
		dragPos.set(point.x, point.y);

		float moveX = (dragPos.x - touchPos.x) / PIXELS_TO_METERS;
		float moveY = (dragPos.y - touchPos.y) / PIXELS_TO_METERS;
		movingVector.x = (dragPos.x - touchPos.x);
		movingVector.y = (dragPos.y - touchPos.y);

		if (striker != null && isTouched) {
			float angle = striker.getAngle();
			Vector2 pos = striker.getPosition();
			float w = Gdx.graphics.getWidth();
			float h = Gdx.graphics.getHeight();
			float newPosX = pos.x+moveX;
			float newPosY = pos.y+moveY;
			if ( newPosX > (w / 2 - 0.05f)) {
				newPosX = (w / 2 - 0.05f);
			}
			if ( newPosX < -(w / 2 - 0.05f)) {
				newPosX = -(w / 2 - 0.05f);
			}
			if ( newPosY > (h / 2 - 0.1f)) {
				newPosY = (h / 2 - 0.1f);
			}
			if ( newPosY < -(h / 2 - 0.1f)) {
				newPosY = -(h / 2 - 0.1f);
			}
			striker.setTransform(newPosX, newPosY, angle);
			touchPos.set(dragPos);
		}
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
