/*
 * Copyright Samuel Halliday 2010
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import javax.media.j3d.BoundingBox;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * The model (M) and controller (C) for a goal during game play.
 *
 * @author Samuel Halliday
 */
public class GoalMC {

	private final BoundingBox roof, west, east, back, bbox;

	/**
	 * @param bbox
	 * @param posts the width of the posts, inset from the bounding box
	 * @param facing
	 */
	public GoalMC(BoundingBox bbox, double posts, Direction facing) {
		Preconditions.checkNotNull(bbox);
		Preconditions.checkArgument(facing == Direction.UP || facing == Direction.DOWN);
		this.bbox = bbox;

		Point3d lower = Utils.getLower(bbox);
		Point3d upper = Utils.getUpper(bbox);

		// build the goal posts and net out of four individual components
		Point3d westLower = new Point3d(lower.x, lower.y, lower.z);
		Point3d westUpper = new Point3d(lower.x + posts, upper.y, upper.z);
		west = new BoundingBox(westLower, westUpper);

		Point3d eastLower = new Point3d(upper.x - posts, lower.y, lower.z);
		Point3d eastUpper = new Point3d(upper.x, upper.y, upper.z);
		east = new BoundingBox(eastLower, eastUpper);

		Point3d roofLower = new Point3d(lower.x, lower.y, upper.z - posts);
		Point3d roofUpper = new Point3d(upper.x, upper.y, upper.z);
		roof = new BoundingBox(roofLower, roofUpper);

		Point3d backLower = new Point3d(lower.x, lower.y, lower.z);
		Point3d backUpper = new Point3d(upper.x, upper.y, upper.z);
		if (facing == Direction.UP) {
			backLower.y = upper.y - posts;
		} else if (facing == Direction.DOWN) {
			backUpper.y = lower.y + posts;
		}
		back = new BoundingBox(backLower, backUpper);
	}

	public void bounce(Point3d p, Vector3d v, Point3d oldPosition) {
		if (!Utils.intersect(bbox, oldPosition, p))
			return;
		for (BoundingBox box : Lists.newArrayList(roof, west, east, back)) {
			bounce(oldPosition, p, v, box);
		}
	}

	private void bounce(Point3d oldS, Point3d s, Vector3d v, BoundingBox box) {
		if (v.length() == 0 || !Utils.intersect(box, oldS, s))
			return;
		s.set(Utils.exitPoint(box, s, v, 0.1)); // ?? energy loss
		s.set(Utils.entryPoint(box, s, v, 0.1)); // ?? energy loss
		v.set(Utils.rebound(box, s, v));
		v.scale(0.5);
	}

	public boolean inside(Point3d p) {
		if (!bbox.intersect(p))
			return false;
		for (BoundingBox box : Lists.newArrayList(roof, west, east, back)) {
			if (box.intersect(p))
				return false;
		}
		return true;
	}
}