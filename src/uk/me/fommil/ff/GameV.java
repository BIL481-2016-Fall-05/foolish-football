/*
 * Copyright Samuel Halliday 2009
 * 
 * This file is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this file.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package uk.me.fommil.ff;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import javax.media.j3d.BoundingPolytope;
import static java.lang.Math.*;
import javax.swing.JPanel;
import javax.vecmath.Point3d;

/**
 * The view (V) for the game play.
 * 
 * @author Samuel Halliday
 */
@SuppressWarnings("serial")
public class GameV extends JPanel {

	private static final Logger log = Logger.getLogger(GameV.class.getName());

	private final boolean debugging = false;

//	private static final Font SHIRT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 8);
	private final int zoom = 3;

	private final Team a;

	private final BufferedImage pitch;

	private final Map<Integer, Sprite> teamSprites = Maps.newHashMap();

	private final Map<Integer, Sprite> ballSprites = Maps.newHashMap();

	private final Map<Integer, Sprite> teamNumberSprites = Maps.newHashMap();

	private final KeyListener keyboardInput = new KeyAdapter() {

		private final Collection<PlayerMC.Action> actions = Sets.newHashSet();

		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					actions.add(PlayerMC.Action.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					actions.add(PlayerMC.Action.RIGHT);
					break;
				case KeyEvent.VK_UP:
					actions.add(PlayerMC.Action.UP);
					break;
				case KeyEvent.VK_DOWN:
					actions.add(PlayerMC.Action.DOWN);
					break;
				case KeyEvent.VK_SPACE:
					actions.add(PlayerMC.Action.KICK);
					break;
				case KeyEvent.VK_ENTER:
					actions.add(PlayerMC.Action.TACKLE);
					break;
				default:
					return;
			}
			modelController.setPlayerActions(GameV.this.a, actions);
		}

		@Override
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					actions.remove(PlayerMC.Action.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					actions.remove(PlayerMC.Action.RIGHT);
					break;
				case KeyEvent.VK_UP:
					actions.remove(PlayerMC.Action.UP);
					break;
				case KeyEvent.VK_DOWN:
					actions.remove(PlayerMC.Action.DOWN);
					break;
				case KeyEvent.VK_SPACE:
					actions.remove(PlayerMC.Action.KICK);
					break;
				case KeyEvent.VK_ENTER:
					actions.remove(PlayerMC.Action.TACKLE);
					break;
				default:
					return;
			}
			modelController.setPlayerActions(GameV.this.a, actions);
		}
	};

	private final GameMC modelController;

	/**
	 * @param a
	 * @param b
	 * @param pitch
	 * @param sprites
	 */
	public GameV(Team a, Team b, BufferedImage pitch, Map<Integer, Sprite> sprites) {
		this.pitch = pitch;
		this.a = a;
		this.modelController = new GameMC(a, b);
		modelController.setView(this);

		// HACK: eventually take in input controls
		setFocusable(true);
		addKeyListener(keyboardInput);

		Map<Color, Color> teamColours = a.getTeamColors();
		for (int i = 0; i < 76; i++) {
			teamSprites.put(i, sprites.get(i + 341).copyWithReplace(teamColours));
		}
		for (int i = 0; i < 4; i++) {
			ballSprites.put(i, sprites.get(i + 1179));
		}
		for (int i = 0; i < 11; i++) {
			teamNumberSprites.put(i + 1, sprites.get(i + 162));
		}
	}

	/**
	 * There are various different coordinate systems in place when painting.
	 * We use Hungarian Notation to keep us right, because there is no type safety for
	 * {@code int} values.
	 * <ul>
	 * <li>{@code gVar}: The {@link Point} locations of the {@link Graphics2D} object.</li>
	 * <li>{@code sVar}: The set of pixels on the screen of those {@link Point}s,
	 *     the same if there is no {@link AffineTransform} applied.</li>
	 * <li>{@code pVar}: The {@link Point3d} position of the objects using our physics model.</li>
	 * </ul>
	 *
	 * @param graphics
	 */
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;

		// apply the zoom and calculate the portion of the screen we can see
		AffineTransform affine = new AffineTransform();
		affine.scale(zoom, zoom);
		g.setTransform(affine);
		Dimension gSize = new Dimension(getWidth() / zoom, getHeight() / zoom);

		// we are centred over the ball
		// g never goes outside the pitch image
		Point3d pBall = modelController.getBall().getPosition();
		int gMinX = (int) Math.round(pBall.x - gSize.width / 2.0);
		int gMinY = (int) Math.round(pBall.y - gSize.height / 2.0);
		// account for falling off up/left
		gMinX = max(gMinX, 0);
		gMinY = max(gMinY, 0);
		// account for falling off down/right, where screen could be bigger than the pitch image
		gMinX = min(gMinX, max(0, pitch.getWidth() - gSize.width));
		gMinY = min(gMinY, max(0, pitch.getHeight() - gSize.height));
		Rectangle gBounds = new Rectangle(gMinX, gMinY, gSize.width, gSize.height);

		// draw the pitch
		BufferedImage sub = pitch.getSubimage(gMinX, gMinY,
				min(gSize.width, pitch.getWidth()),
				min(gSize.height, pitch.getHeight()));
		g.drawImage(sub, 0, 0, null);

		// draw the ball
		drawBall(g, gBounds);

		// draw the zones
		if (debugging) {
			g.setColor(Color.GREEN);
			for (int i = 0; i <= 5; i++) {
				int x = 81 + i * (590 - 81) / 5;
				g.drawLine(x - gMinX, 129 - gMinY, x - gMinX, 769 - gMinY);
			}
			for (int i = 0; i <= 7; i++) {
				int y = 129 + i * (769 - 129) / 7;
				g.drawLine(81 - gMinX, y - gMinY, 590 - gMinX, y - gMinY);
			}
		}

		// draw the players that are in view
		for (PlayerMC pm : modelController.getPlayers()) {
			drawPlayer(g, gBounds, pm);
		}
	}

	private void drawPlayer(Graphics2D g, Rectangle gBounds, PlayerMC pm) {
		Point3d pPos = pm.getPosition();
		// p is the pixel location of the centre of the player
		Point gPos = pToG(gBounds, pPos);

		// assumes sprite size
		Rectangle gPmSprite = new Rectangle(gBounds.x + gPos.x - 20, gBounds.y + gPos.y - 20, 40, 40);
		if (!gBounds.intersects(gPmSprite)) {
			// off screen, don't draw
			return;
		}

		if (debugging && pm == modelController.getSelected()) {
			// draw the bounds, scattergun and not efficient
			g.setColor(Color.WHITE);
			BoundingPolytope pBounds = pm.getBounds();
			for (int x = (int) (pPos.x - 50); x < pPos.x + 50; x++) {
				for (int y = (int) (pPos.y - 50); y < pPos.y + 50; y++) {
					Point3d pTest = new Point3d(x, y, 0); // TODO: use ball height
					if (pBounds.intersect(pTest)) {
						Point gP = pToG(gBounds, pTest);
						g.fillRect(gP.x, gP.y, 1, 1);
					}
				}
			}
		}

		int spriteIndex;
		double angle = pm.getAngle();
		if (angle <= - 3 * Math.PI / 4) {
			spriteIndex = 12;
		} else if (angle <= -Math.PI / 2) {
			spriteIndex = 9;
		} else if (angle <= -Math.PI / 4) {
			spriteIndex = 18;
		} else if (angle <= 0) {
			spriteIndex = 0;
		} else if (angle <= Math.PI / 4) {
			spriteIndex = 21;
		} else if (angle <= Math.PI / 2) {
			spriteIndex = 6;
		} else if (angle <= 3 * Math.PI / 4) {
			spriteIndex = 15;
		} else {
			spriteIndex = 3;
		}

		// 0/+1/+2 depending on timestamp and motion
		if (pm.getVelocity().length() > 0) {
			long t = modelController.getTimestamp() % 500L;
			if (t < 250) {
				spriteIndex += 1;
			} else {
				spriteIndex += 2;
			}
		}

		Sprite sprite = teamSprites.get(spriteIndex);
		Point s = sprite.getCentre();
		g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1, gPos.y - s.y / 2, null);

		if (pm == modelController.getSelected()) {
			sprite = teamNumberSprites.get(pm.getShirt());
			s = sprite.getCentre();
			g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1, gPos.y - s.y / 2 - 13, null);
		}
	}

	private void drawBall(Graphics2D g, Rectangle gBounds) {
		// 0/+1/+2/+3 depending on timestamp and motion
		BallMC ball = modelController.getBall();
		int spriteIndex = 0;
		if (ball.getVelocity().length() > 0) {
			long t = modelController.getTimestamp() % 600L;
			if (t < 200) {
				spriteIndex += 1;
			} else if (t < 400) {
				spriteIndex += 2;
			} else {
				spriteIndex += 3;
			}
		}

		Sprite sprite = ballSprites.get(spriteIndex);
		Point s = sprite.getCentre();
		Point gPos = pToG(gBounds, ball.getPosition());
		g.drawImage(sprite.getImage(), gPos.x - s.x / 2 - 1, gPos.y - s.y / 2, null);
	}

	private Point pToG(Rectangle gView, Point3d p) {
		return new Point((int) round(p.x - gView.getX()), (int) round(p.y - gView.getY()));
	}
}
