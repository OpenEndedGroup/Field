package field.core.plugins.drawing.opengl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import field.graphics.core.Base;
import field.graphics.core.BasicUtilities;
import field.graphics.dynamic.DynamicLine_long;
import field.graphics.dynamic.DynamicMesh_long;
import field.graphics.dynamic.DynamicPointlist;

public class CachedLineLayer {

	public ArrayList_Mod<CachedLine> line = new ArrayList_Mod<CachedLine>();

	LinkedHashSet<CachedLine> previous = new LinkedHashSet<CachedLine>();

	DynamicMesh_long geometry = null;
	DynamicPointlist geometry_points = null;

	int previousMod = -1;
	
	public CachedLineLayer() {

	}

	public class UpdateMesh extends BasicUtilities.OnePassElement {

		private DynamicMesh_long target;
		private DirectMesh m;

		public UpdateMesh(DynamicMesh_long target, DirectMesh m) {
			super(Base.StandardPass.transform);
			this.target = target;
			this.m = m;
		}

		@Override
		public void performPass() {
			updateMesh(target, m);
		}

		public CachedLineLayer getLayer() {
			return CachedLineLayer.this;
		}

	}

	public class UpdatePoint extends BasicUtilities.OnePassElement {

		private DynamicPointlist target;
		private DirectPoint m;

		public UpdatePoint(DynamicPointlist target, DirectPoint m) {
			super(Base.StandardPass.transform);
			this.target = target;
			this.m = m;
		}

		@Override
		public void performPass() {
			updatePoint(target, m);
		}

		public CachedLineLayer getLayer() {
			return CachedLineLayer.this;
		}

	}

	public class UpdateLine extends BasicUtilities.OnePassElement {

		private DynamicLine_long target;
		private DirectLine m;

		public UpdateLine(DynamicLine_long target, DirectLine m) {
			super(Base.StandardPass.transform);
			this.target = target;
			this.m = m;
		}

		@Override
		public void performPass() {
//			System.out.println(" update line :"+target+" "+m);
			updateLine(target, m);
		}

		public CachedLineLayer getLayer() {
			return CachedLineLayer.this;
		}

	}

	protected void updatePoint(DynamicPointlist target, DirectPoint dp) {
		
		if (line.getMod()==previousMod)
		{
			for (CachedLine c : line) {
				Integer n = c.getProperties().get(iLinearGraphicsContext.forceNew);
				if (n != null) {
					c.getProperties().remove(iLinearGraphicsContext.forceNew);
					c.mod();
				}
			}
			return;
		}
			
		
		LinkedHashSet<CachedLine> newLines = new LinkedHashSet<CachedLine>();
		LinkedHashSet<CachedLine> goneLines = new LinkedHashSet<CachedLine>();

		goneLines.addAll(previous);
		goneLines.removeAll(line);

		newLines.addAll(line);
		newLines.removeAll(previous);

		for (CachedLine c : line) {
			Integer n = c.getProperties().get(iLinearGraphicsContext.forceNew);
			if (n != null) {
				c.getProperties().remove(iLinearGraphicsContext.forceNew);
				c.mod();
			}
		}

		for (CachedLine c : newLines) {
			dp.wire(target, c);
			float ps = c.getProperties().getFloat(iLinearGraphicsContext.pointSize, 1f);
			target.getUnderlyingGeometry().setSize(ps);
		}

		for (CachedLine c : goneLines) {
			dp.unwire(target, c);
		}

		previous.clear();
		previous.addAll(line);
		previousMod = line.getMod();

	}

	protected void updateMesh(DynamicMesh_long target, DirectMesh dm) {
		if (line.getMod()==previousMod)
		{
			for (CachedLine c : line) {
				Integer n = c.getProperties().get(iLinearGraphicsContext.forceNew);
				if (n != null) {
					c.getProperties().remove(iLinearGraphicsContext.forceNew);
					c.mod();
				}
			}
			return;
		}

		LinkedHashSet<CachedLine> newLines = new LinkedHashSet<CachedLine>();
		LinkedHashSet<CachedLine> goneLines = new LinkedHashSet<CachedLine>();

		goneLines.addAll(previous);
		goneLines.removeAll(line);

		newLines.addAll(line);
		newLines.removeAll(previous);

		for (CachedLine c : line) {
			Integer n = c.getProperties().get(iLinearGraphicsContext.forceNew);
			if (n != null) {
				c.getProperties().remove(iLinearGraphicsContext.forceNew);
				c.mod();
			}
		}

		for (CachedLine c : newLines) {
			dm.wire(target, c);
		}

		for (CachedLine c : goneLines) {
			dm.unwire(target, c);
		}

		previous.clear();
		previous.addAll(line);
		previousMod = line.getMod();

	}

	protected void updateLine(DynamicLine_long target, DirectLine dm) {
		
		if (line.getMod()==previousMod)
		{
			for (CachedLine c : line) {
				Integer n = c.getProperties().get(iLinearGraphicsContext.forceNew);
				if (n != null) {
					c.getProperties().remove(iLinearGraphicsContext.forceNew);
					c.mod();
				}
			}
			return;
		}

		LinkedHashSet<CachedLine> newLines = new LinkedHashSet<CachedLine>();
		LinkedHashSet<CachedLine> goneLines = new LinkedHashSet<CachedLine>();

		goneLines.addAll(previous);
		goneLines.removeAll(line);

		newLines.addAll(line);
		newLines.removeAll(previous);

		for (CachedLine c : line) {
			Integer n = c.getProperties().get(iLinearGraphicsContext.forceNew);
			if (n != null) {
				c.getProperties().remove(iLinearGraphicsContext.forceNew);
				c.mod();
			}
		}

		for (CachedLine c : newLines) {
			dm.wire(target, c);
			float ps = c.getProperties().getFloat(iLinearGraphicsContext.thickness, 1f);
			target.getUnderlyingGeometry().setWidth(ps);
		}

		for (CachedLine c : goneLines) {
			dm.unwire(target, c);
		}

		previous.clear();
		previous.addAll(line);
		
		previousMod = line.getMod();

	}

	static public class ArrayList_Mod<T> extends ArrayList<T> {
		int getMod() {
			return modCount;
		}
	}

}
