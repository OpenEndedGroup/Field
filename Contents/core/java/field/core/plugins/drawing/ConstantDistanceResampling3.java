package field.core.plugins.drawing;

import java.util.ArrayList;
import java.util.List;

import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineCursor;
import field.core.plugins.drawing.opengl.LineUtils;
import field.core.plugins.drawing.opengl.Small3dLineEmitter_long;
import field.core.plugins.drawing.opengl.iLine;
import field.core.plugins.drawing.opengl.iLinearGraphicsContext;
import field.math.linalg.Vector3;
import field.util.Dict;

/**
 * attempts to have a spline that has a control node evenly spaced through
 * distance
 */
public class ConstantDistanceResampling3 {

	private final float minResolution;

	private final int maxSubdiv;

	public ConstantDistanceResampling3(float minResolution, int maxSubdiv) {
		this.minResolution = minResolution;
		this.maxSubdiv = maxSubdiv;
	}

	boolean firstOut = true;

	public CachedLine resample(int num, CachedLine input) {
		return resample(num, input, false);
	}

	List<Integer> mapping = new ArrayList<Integer>();

	public CachedLine resample(float num, CachedLine input, boolean numIsDistance) {
		firstOut = true;
		CachedLine fine = new CachedLine();
		final iLine in = fine.getInput();
		Small3dLineEmitter_long em = new Small3dLineEmitter_long() {
			Vector3 lastOut = null;

			@Override
			public void emitLinearFrame(Vector3 a, Vector3 b, java.util.List<Object> name, java.util.List<Object> name2, Dict properties, iLinearGraphicsContext contex) {
				if (firstOut || lastOut.distanceFrom(a) > 1e-5) {
					in.moveTo(a.x, a.y);
					in.setPointAttribute(iLinearGraphicsContext.z_v, a.z);
					firstOut = false;
					lastOut = new Vector3(b);
				}
				in.lineTo(b.x, b.y);
				in.setPointAttribute(iLinearGraphicsContext.z_v, b.z);
				lastOut.x = b.x;
				lastOut.y = b.y;
				lastOut.z = b.z;
			}

			@Override
			public void emitCubicFrame(Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, List<Object> name, List<Object> name2, Dict properties, iLinearGraphicsContext context) {
				if (firstOut || lastOut.distanceFrom(a) > 1e-5) {
					in.moveTo(a.x, a.y);
					in.setPointAttribute(iLinearGraphicsContext.z_v, a.z);
					firstOut = false;
					lastOut = new Vector3(b);
				}
				in.cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y);
				in.setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(c1.z, c2.z, b.z));
				lastOut.x = b.x;
				lastOut.y = b.y;
				lastOut.z = b.z;
			}

			@Override
			protected boolean shouldTerm(float flatness, Vector3 a, Vector3 c1, Vector3 c2, Vector3 b, int n) {
				return n > maxSubdiv || a.distanceFrom(b) < minResolution;
			}
		}.setCanEmitCubic(true);

		drawInto(input, em, fine);

		// compute overall length

		List<Float> subsegmentLengths = new ArrayList<Float>();

		Vector3 a = new Vector3();
		Vector3 b = new Vector3();
		Vector3 c1 = new Vector3();
		Vector3 c2 = new Vector3();
		Vector3 out = new Vector3();
		{
			CachedLineCursor cursor = new CachedLineCursor(fine);
			while (cursor.hasNextSegment()) {
				float length = 0;

				while (cursor.hasNextInSpline()) {

					if (cursor.nextIsCubic()) {
						if (cursor.nextCubicFrame3(a, c1, c2, b)) {
							LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f, out);
							length += a.distanceFrom(out);
							length += out.distanceFrom(b);
						}
					} else {
						if (cursor.nextLinearFrame3(a, b)) {
							length += a.distanceFrom(b);
						}
					}

					cursor.next();
				}

				subsegmentLengths.add(length);

				if (cursor.hasNextSegment())
					cursor.next();
			}
		}

		if (numIsDistance && num == minResolution)
			return fine;

		Vector3 c12 = new Vector3();
		Vector3 c21 = new Vector3();
		Vector3 m = new Vector3();
		Vector3 tmp = new Vector3();
		CachedLine resampled = new CachedLine();
		iLine resampledIn = resampled.getInput();
		{
			CachedLineCursor cursor = new CachedLineCursor(fine);
			while (cursor.hasNextSegment()) {
				float length = subsegmentLengths.remove(0);

				float subsegTargetLength = numIsDistance ? num : length / num;
				float currentLength = 0;

				List<Vector3> stream = new ArrayList<Vector3>();

				boolean first = true;
				Vector3 last = null;

				segment: while (cursor.hasNextInSpline()) {

					System.err.println(" processing <" + cursor.getCurrentIndex() + ">");
					if (cursor.nextIsCubic()) {
						if (cursor.nextCubicFrame3(a, c1, c2, b)) {
							if (first) {
								stream.add(new Vector3(a));
								first = false;
							}
							inner: while (true) {
								System.err.println(" subdividing frame <" + a + " " + c1 + " " + c2 + " " + b + ">");
								LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f, out);
								float l1 = a.distanceFrom(out);
								System.out.println("        " + l1 + " / " + subsegTargetLength + " " + currentLength);
								if (currentLength + l1 > subsegTargetLength) {
									float alpha = (subsegTargetLength - currentLength) / l1;
									if (Float.isInfinite(alpha))
										alpha = 0;
									if (Float.isNaN(alpha))
										alpha = 0;
									if (alpha < 0)
										alpha = 0;
									if (alpha > 1)
										alpha = 1;
									LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f * alpha, out);

									stream.add(new Vector3(out));
									System.out.println(" over 1, added <" + out + ">");

									float nowLength = currentLength + out.distanceFrom(a);
									subsegTargetLength = length / num - (nowLength - length / num) / 2;
									currentLength = 0;

									LineUtils.splitCubicFrame3(a, c1, c2, b, 0.5f * alpha, c12, m, c21, tmp);

									a.setValue(m);
									c1.setValue(c21);

									if (a.distanceFrom(b) > subsegTargetLength) {
										System.out.println(" still some more <" + a.distanceFrom(b) + ">");
										continue inner;
									} else {
										System.out.println(" no more <" + a.distanceFrom(b) + ">");
										currentLength += a.distanceFrom(b);
										cursor.next();
										continue segment;
									}
								}
								float l2 = out.distanceFrom(b);
								System.out.println("         " + l2 + " / " + subsegTargetLength + " " + currentLength);
								if (currentLength + l1 + l2 > subsegTargetLength) {
									float alpha = (subsegTargetLength - currentLength - l1) / l2;
									if (Float.isInfinite(alpha))
										alpha = 0;
									if (Float.isNaN(alpha))
										alpha = 0;
									if (alpha < 0)
										alpha = 0;
									if (alpha > 1)
										alpha = 1;
									LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f + 0.5f * alpha, out);

									stream.add(new Vector3(out));
									System.out.println(" over 2, added <" + out + ">");

									float nowLength = currentLength + out.distanceFrom(a);
									subsegTargetLength = length / num - (nowLength - length / num) / 2;
									currentLength = 0;
									LineUtils.splitCubicFrame3(a, c1, c2, b, 0.5f + 0.5f * alpha, c12, m, c21, tmp);

									a.setValue(m);
									c1.setValue(c21);

									if (a.distanceFrom(b) > subsegTargetLength) {
										System.out.println(" still some more <" + a.distanceFrom(b) + ">");
									} else {
										System.out.println(" no more <" + a.distanceFrom(b) + ">");
										currentLength += a.distanceFrom(b);
										cursor.next();
										continue segment;
									}
								} else {
									currentLength += l1 + l2;
									break;
								}

							}
						}
					} else {
						if (cursor.nextLinearFrame3(a, b)) {
							
							c1 = new Vector3().lerp(a, b, 1/3f);
							c2 = new Vector3().lerp(a, b, 2/3f);
							
							if (first) {
								stream.add(new Vector3(a));
								first = false;
							}
							inner: while (true) {
								System.err.println(" subdividing frame <" + a + " " + c1 + " " + c2 + " " + b + ">");
								LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f, out);
								float l1 = a.distanceFrom(out);
								System.out.println("        " + l1 + " / " + subsegTargetLength + " " + currentLength);
								if (currentLength + l1 > subsegTargetLength) {
									float alpha = (subsegTargetLength - currentLength) / l1;
									if (Float.isInfinite(alpha))
										alpha = 0;
									if (Float.isNaN(alpha))
										alpha = 0;
									if (alpha < 0)
										alpha = 0;
									if (alpha > 1)
										alpha = 1;
									LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f * alpha, out);

									stream.add(new Vector3(out));
									System.out.println(" over 1, added <" + out + ">");

									float nowLength = currentLength + out.distanceFrom(a);
									subsegTargetLength = length / num - (nowLength - length / num) / 2;
									currentLength = 0;

									LineUtils.splitCubicFrame3(a, c1, c2, b, 0.5f * alpha, c12, m, c21, tmp);

									a.setValue(m);
									c1.setValue(c21);

									if (a.distanceFrom(b) > subsegTargetLength) {
										System.out.println(" still some more <" + a.distanceFrom(b) + ">");
										continue inner;
									} else {
										System.out.println(" no more <" + a.distanceFrom(b) + ">");
										currentLength += a.distanceFrom(b);
										cursor.next();
										continue segment;
									}
								}
								float l2 = out.distanceFrom(b);
								System.out.println("         " + l2 + " / " + subsegTargetLength + " " + currentLength);
								if (currentLength + l1 + l2 > subsegTargetLength) {
									float alpha = (subsegTargetLength - currentLength - l1) / l2;
									if (Float.isInfinite(alpha))
										alpha = 0;
									if (Float.isNaN(alpha))
										alpha = 0;
									if (alpha < 0)
										alpha = 0;
									if (alpha > 1)
										alpha = 1;
									LineUtils.evaluateCubicFrame3(a, c1, c2, b, 0.5f + 0.5f * alpha, out);

									stream.add(new Vector3(out));
									System.out.println(" over 2, added <" + out + ">");

									float nowLength = currentLength + out.distanceFrom(a);
									subsegTargetLength = length / num - (nowLength - length / num) / 2;
									currentLength = 0;
									LineUtils.splitCubicFrame3(a, c1, c2, b, 0.5f + 0.5f * alpha, c12, m, c21, tmp);

									a.setValue(m);
									c1.setValue(c21);

									if (a.distanceFrom(b) > subsegTargetLength) {
										System.out.println(" still some more <" + a.distanceFrom(b) + ">");
									} else {
										System.out.println(" no more <" + a.distanceFrom(b) + ">");
										currentLength += a.distanceFrom(b);
										cursor.next();
										continue segment;
									}
								} else {
									currentLength += l1 + l2;
									break;
								}

							}
						}
					}

					cursor.next();
				}
				stream.add(b);

				convertStreamToSmoothSpline(stream, resampledIn);

				subsegmentLengths.add(length);

				if (cursor.hasNextSegment())
					cursor.next();
			}
		}

		return resampled;

	}

	protected void convertStreamToSmoothSpline(List<Vector3> stream, iLine resampledIn) {
		if (stream.size() < 3)
			return;
		for (int i = 0; i < stream.size(); i++) {
			if (i == 0) {
				resampledIn.moveTo(stream.get(i).x, stream.get(i).y);
				resampledIn.setPointAttribute(iLinearGraphicsContext.z_v, stream.get(i).z);
			} else if (i < stream.size() - 1 && i > 1) {
				Vector3 t1 = new Vector3(stream.get(i + 1)).sub(stream.get(i - 1)).scale(-1 / 6f).add(stream.get(i));
				Vector3 t2 = new Vector3(stream.get(i)).sub(stream.get(i - 2)).scale(1 / 6f).add(stream.get(i - 1));
				resampledIn.cubicTo(t2.x, t2.y, t1.x, t1.y, stream.get(i).x, stream.get(i).y);
				resampledIn.setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(t2.z, t1.z, stream.get(i).z));

			} else if (i == 1) {
				Vector3 t1 = new Vector3(stream.get(i + 1)).sub(stream.get(i - 1)).scale(-1 / 6f).add(stream.get(i));
				Vector3 t2 = new Vector3(stream.get(i)).sub(stream.get(i - 1)).scale(1 / 3f).add(stream.get(i - 1));
				resampledIn.cubicTo(t2.x, t2.y, t1.x, t1.y, stream.get(i).x, stream.get(i).y);
				resampledIn.setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(t2.z, t1.z, stream.get(i).z));
			} else if (i == stream.size() - 1) {
				Vector3 t1 = new Vector3(stream.get(i)).sub(stream.get(i - 1)).scale(-1 / 3f).add(stream.get(i));
				Vector3 t2 = new Vector3(stream.get(i)).sub(stream.get(i - 2)).scale(1 / 6f).add(stream.get(i - 1));
				resampledIn.cubicTo(t2.x, t2.y, t1.x, t1.y, stream.get(i).x, stream.get(i).y);
				resampledIn.setPointAttribute(iLinearGraphicsContext.z_v, new Vector3(t2.z, t1.z, stream.get(i).z));
			}
		}
	}

	public List<Integer> getMapping() {
		return mapping;
	}

	public void drawInto(CachedLine line, Small3dLineEmitter_long lineEmitter, CachedLine in) {

		CachedLineCursor cursor = new CachedLineCursor(line);

		line.finish();

		Vector3 a = new Vector3();
		Vector3 b = new Vector3();
		Vector3 c1 = new Vector3();
		Vector3 c2 = new Vector3();
		lineEmitter.begin();
		while (cursor.hasNextSegment()) {
			lineEmitter.beginContour();
			while (cursor.hasNextInSpline()) {

				if (cursor.nextIsCubic()) {
					if (cursor.nextCubicFrame3(a, c1, c2, b)) {
						lineEmitter.flattenCubicFrame(a, c1, c2, b, lineEmitter.packet(cursor.getCurrent()), lineEmitter.packet(cursor.getAfter()), null, null, 0);
					}
					mapping.add(in.events.size());
				} else {
					if (cursor.nextLinearFrame3(a, b)) {
						lineEmitter.flattenLinearFrame(a, b, lineEmitter.packet(cursor.getCurrent()), lineEmitter.packet(cursor.getAfter()), null, null, 0);
					}
					mapping.add(in.events.size());
				}

				cursor.next();
			}
			lineEmitter.endContour();

			if (cursor.hasNextSegment())
				cursor.next();
		}
		lineEmitter.end();
	}

}
