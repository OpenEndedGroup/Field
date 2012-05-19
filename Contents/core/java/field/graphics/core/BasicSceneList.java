package field.graphics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.core.util.PythonCallableMap;
import field.graphics.core.Base.LocalPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iPass;
import field.graphics.core.Base.iSceneListElement;
import field.launch.iUpdateable;
import field.math.graph.NodeImpl;
import field.math.graph.iMutable;

/**
 * children are scenelist elements /** use the addChild(...) etc to add things
 * to this class
 */
public class BasicSceneList extends NodeImpl<iSceneListElement> implements iSceneListElement, iUpdateable, iAcceptsSceneListElement {

	
	public interface iGlobalEarly
	{
		public void early();
	}
	
	private static final long serialVersionUID = 1L;

	/* implementation of scenelist */
	protected List<iPass> passList = new ArrayList<iPass>();

	/** use the addChild(...) etc to add things to this class */

	protected boolean passListIsUnsorted = false;

	protected Map standards = new HashMap();

	protected Comparator<iPass> passComparator = new Comparator<iPass>() {
		public int compare(iPass o1, iPass o2) {
			if (o1.getValue() > o2.getValue())
				return 1;
			return -1;
		}
	};
	
	public void addChildOnce(iSceneListElement newChild) {
		if (!isChild(newChild))
			super.addChild(newChild);
	}

	public void addChild(iSceneListElement newChild) {
		super.addChild(newChild);
	}


	public void removeChild(iSceneListElement newChild) {
		super.removeChild(newChild);
	}

	int xxx = 0;

	public BasicSceneList() {
	}

	@HiddenInAutocomplete
	public void listElements(String xx) {
		int i;
		for (iSceneListElement element : getChildren()) {
			String nn = element.getClass().toString();
			nn = nn.substring(nn.lastIndexOf('.'));
			if (element instanceof BasicSceneList)
				((BasicSceneList) element).listElements(xx + xx);
		}
	}

	@HiddenInAutocomplete
	public void performPass(iPass p) {
		// update();

		// if (passList.contains(p)) for (iSceneListElement element :
		// getChildren())
		// element.performPass(p);
	}

	@HiddenInAutocomplete
	public iPass requestPass(iPass pass) {
		if (!passList.contains(pass))
			passList.add(pass);
		Collections.sort(passList, passComparator);
		return pass;
	}

	@HiddenInAutocomplete
	public iPass requestPassAfter(iPass pass) {
		// bracket this pass
		int index = passList.indexOf(pass);
		if (index == -1)
			throw new IllegalArgumentException(" ( couldn't find pass <" + pass + "> ) ");
		float above;
		float below;
		if (index == passList.size() - 1)
			above = 1 + (below = ((LocalPass) passList.get(passList.size() - 1)).getValue());
		else {
			above = ((LocalPass) passList.get(index)).getValue();
			below = ((LocalPass) passList.get(index + 1)).getValue();
		}
		LocalPass ret;
		passList.add(index + 1, ret = new LocalPass((above + below) / 2));
		return ret;
	}

	@HiddenInAutocomplete
	public iPass requestPassAfterAndBefore(iPass after, iPass before) {
		return requestPassBefore(before);
	}

	@HiddenInAutocomplete
	public iPass requestPassBefore(iPass pass) {
		// bracket this pass
		int index = passList.indexOf(pass);
		if (index == -1)
			throw new IllegalArgumentException(" ( couldn't find pass <" + pass + "> ) ");
		index -= 1;

		float above;
		float below;

		if (index == passList.size() - 1)
			above = 1 + (below = ((LocalPass) passList.get(passList.size() - 1)).getValue());
		else {
			above = ((LocalPass) passList.get(index)).getValue();
			below = ((LocalPass) passList.get(index + 1)).getValue();
		}
		LocalPass ret;
		passList.add(index + 1, ret = new LocalPass((above + below) / 2));
		return ret;
	}

	@HiddenInAutocomplete
	public void update() {
		for (iPass pass : passList) {
			for (iSceneListElement element : getChildren()) {
				element.performPass(pass);
			}
		}
	}

	@HiddenInAutocomplete
	public void updateFromButNotIncluding(iPass from) {
		for (iPass pass : passList) {
			if (pass.isLaterThan(from)) {
				// System.err.println("from pass <" + pass +
				// "> <" + System.identityHashCode(this) + "> <"
				// + this.getClass() + ">");
				for (iSceneListElement element : new ArrayList<iSceneListElement>(getChildren()))
				{
					element.performPass(pass);
				}
			}
		}
	}

	@HiddenInAutocomplete
	public void updateFromButNotIncluding(iPass from, iPass upToAndIncluding) {
		for (iPass pass : passList) {
			if (pass.isLaterThan(from)) {
				if (!pass.isEarlierThan(upToAndIncluding) && !pass.equals(upToAndIncluding))
					return;

				// System.err.println("from pass <" + pass +
				// "> to <"+upToAndIncluding+"> <" +
				// System.identityHashCode(this) + "> <" +
				// this.getClass() + ">");
				for (iSceneListElement element : getChildren())
					element.performPass(pass);
			}
			if (!pass.isEarlierThan(upToAndIncluding))
				return;
		}
	}

	// these are useful for subclasses of this thing that want to do
	// something cleaver
	@HiddenInAutocomplete
	public void updateUpToAndIncluding(iPass to) {
		// System.err.println(" ordered pass list is <"+passList+">");
		for (iPass pass : passList) {
			if (!pass.isEarlierThan(to) && !pass.equals(to))
				return;

			// System.err.println("upto pass <" + pass + "> <" +
			// System.identityHashCode(this) + "> <" +
			// this.getClass() + ">");
			for (iSceneListElement element : new ArrayList<iSceneListElement>(getChildren())) {
				// System.err.println("       on <" + element +
				// "> <" + element.getClass() + "> in ....");
				element.performPass(pass);
				// System.err.println("       on <" + element +
				// "> <" + element.getClass() + "> out ....");
			}
			if (!pass.isEarlierThan(to))
				return;
		}
	}

	public boolean isChild(iSceneListElement e) {
		return getChildren().contains(e);
	}
	
	When when;
	
	public PythonCallableMap add(Base.StandardPass pass)
	{
		return getWhen().getMap(pass);
	}

	public PythonCallableMap add(int pass)
	{
		return getWhen().getMap(pass);
	}
	
	@HiddenInAutocomplete
	public When getWhen() {
		if (when == null)
			when = new When(this);
		return when;
	}
	
	
	@HiddenInAutocomplete
	public void performGlobalEarly()
	{
		for(iSceneListElement e : getChildren())
		{
			if (e instanceof iGlobalEarly)
			{
				((iGlobalEarly)e).early();
			}
			if (e instanceof BasicSceneList)
			{
				 ((BasicSceneList)e).performGlobalEarly();
			}
		}
			
	}
	
}
