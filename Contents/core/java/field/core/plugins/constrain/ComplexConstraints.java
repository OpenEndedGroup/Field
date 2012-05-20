package field.core.plugins.constrain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.iPlugin;
import field.core.plugins.SimpleConstraints.Constraint;
import field.core.plugins.constrain.cassowary.ClConstraint;
import field.core.plugins.constrain.cassowary.ClSimplexSolver;
import field.core.plugins.constrain.cassowary.ClStrength;
import field.core.plugins.constrain.cassowary.ClVariable;
import field.core.plugins.constrain.cassowary.ExCLConstraintNotFound;
import field.core.plugins.constrain.cassowary.ExCLError;
import field.core.plugins.constrain.cassowary.ExCLInternalError;
import field.core.plugins.constrain.cassowary.ExCLRequiredFailure;
import field.core.plugins.drawing.BasicDrawingPlugin;
import field.core.plugins.drawing.BasicDrawingPlugin.FrameManipulation;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.python.PythonPluginEditor;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.math.graph.NodeImpl;
import field.math.graph.iMutableContainer;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Generics.Pair;
import field.util.SimpleHashQueue;

@GenerateMethods
public class ComplexConstraints implements iPlugin {
	public class LocalVisualElement extends NodeImpl<iVisualElement> implements iVisualElement {

		public <T> void deleteProperty(VisualElementProperty<T> p) {
		}

		public void dispose() {
		}

		public Rect getFrame(Rect out) {
			return null;
		}

		public <T> T getProperty(iVisualElement.VisualElementProperty<T> p) {
			if (p == overrides)
				return (T) elementOverride;
			Object o = properties.get(p);
			return (T) o;
		}

		public String getUniqueID() {
			return pluginId;
		}

		public Map<Object, Object> payload() {
			return properties;
		}

		public void setFrame(Rect out) {
		}

		public iMutableContainer<Map<Object, Object>, iVisualElement> setPayload(Map<Object, Object> t) {
			properties = t;
			return this;
		}

		public <T> iVisualElement setProperty(iVisualElement.VisualElementProperty<T> p, T to) {
			properties.put(p, to);
			return this;
		}

		public void setUniqueID(String uid) {
		}
	}

	public class Overrides extends iVisualElementOverrides.DefaultOverride {
		@Override
		public VisitCode deleted(iVisualElement source) {
			if (offeredConstraint.containsKey(source)) {
				ClConstraint m = offeredConstraint.remove(source);
				if (m != null) {
					try {

						solver.removeConstraint(m);
					} catch (ExCLConstraintNotFound e) {
						e.printStackTrace();
					} catch (ExCLInternalError e) {
						e.printStackTrace();
					}
				}
			}
			if (participatesInConstraints.containsKey(source)) {
				VariablesForRect m = participatesInConstraints.remove(source);
				List<iVisualElement> a = StandardFluidSheet.allVisualElements(root);
				Set<iVisualElement> needsDeleting = new HashSet<iVisualElement>();

				for(iVisualElement v : a)
				{
					Map<String, iVisualElement> cp = v.getProperty(BaseConstraintOverrides.constraintParameters);
					if (cp!=null)
					{
						for(iVisualElement v2 : cp. values())
						{
							if (v2 == source)
							{
								needsDeleting.add(v);
							}
						}
					}
				}

				for(iVisualElement v : needsDeleting)
				{
					PythonPluginEditor.delete(v,v);
				}
			}

			return super.deleted(source);
		}

		@Override
		public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
			if (prop == BasicDrawingPlugin.frameManipulationBegin) {
				// ;//System.out.println(" frame manip begin <" +
				// to + ">");

				currentlyMoving.add(source);

			} else if (prop == BasicDrawingPlugin.frameManipulationEnd) {

				if (participatesInConstraints.get(source) != null) {

					updateVariablesFromFrame(getVariablesFor(source), ((FrameManipulation) to.get()).originalFrame);
					updateQueueNow();

					VariablesForRect r = participatesInConstraints.get(source);
					Rect rect = new Rect(r.variableX.value(), r.variableY.value(), r.variableW.value(), r.variableH.value());
					// ;//System.out.println(" would set frame
					// to be (3) <" + rect + ">");
					((FrameManipulation) to.get()).originalFrame.setValue(rect);

					resetSolver();

				}

				currentlyMoving.remove(source);

			}
			return super.setProperty(source, prop, to);
		}

		@Override
		public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
			if (inLoop)
				return VisitCode.cont;

			if (participatesInConstraints.get(source) != null) {

				if (currentlyMoving.contains(source)) {
					updateVariablesFromFrame(getVariablesFor(source), new Rect(0, 0, 0, 0).setValue(newFrame));
					boolean touched = updateQueueNow();

					if (touched) {
						VariablesForRect r = participatesInConstraints.get(source);
						Rect rect = new Rect(r.variableX.value(), r.variableY.value(), r.variableW.value(), r.variableH.value());
						newFrame.setValue(rect);
					} else
						;//System.out.println(" error or nothing to do ?");
				} else {
					updateVariablesFromFrame(getVariablesFor(source), newFrame);
				}
			}

			return VisitCode.cont;
		}
	}

	public class VariablesForRect {
		public iVisualElement rect;

		public ClVariable variableX;

		public ClVariable variableY;

		public ClVariable variableW;

		public ClVariable variableH;
	}

	static public final String pluginId = "//plugin_complexConstraints";

	public static final VisualElementProperty<ComplexConstraints> complexConstraints_plugin = new VisualElementProperty<ComplexConstraints>("//plugin_complexConstraints");

	private final LocalVisualElement lve;

	private iVisualElement root;

	// every constraint is represented by a visualelement (for interaction,
	// deletion etc...)

	private SelectionGroup<iComponent> group;

	ComplexConstraints_m instance = new ComplexConstraints_m(this);

	ClSimplexSolver solver = new ClSimplexSolver();

	HashMap<iVisualElement, VariablesForRect> participatesInConstraints = new HashMap<iVisualElement, VariablesForRect>();
	HashMap<iVisualElement, ClConstraint> offeredConstraint = new LinkedHashMap<iVisualElement, ClConstraint>();

	CachedLine constraints_thickLineCL;

	HashSet<iVisualElement> currentlyMoving = new HashSet<iVisualElement>();

	SimpleHashQueue preEditQueue = new SimpleHashQueue();

	SimpleHashQueue queue = new SimpleHashQueue();

	SimpleHashQueue postEditQueue = new SimpleHashQueue();

	HashSet<VariablesForRect> queued = new HashSet<VariablesForRect>();

	boolean editOngoing = false;

	boolean inLoop = false;

	iVisualElementOverrides elementOverride;

	Map<Object, Object> properties = new HashMap<Object, Object>();

	public ComplexConstraints() {
		lve = new LocalVisualElement();
	}

	public void close() {
	}

	public ClConstraint getConstraintForElement(iVisualElement ve) {
		return offeredConstraint.get(ve);
	}

	public Object getPersistanceInformation() {
		return new Pair<String, Collection<Collection<Constraint>>>(pluginId + "version_1", null);
	}

	public VariablesForRect getVariablesFor(iVisualElement ve) {
		VariablesForRect r = participatesInConstraints.get(ve);

		if (r == null) {
			final VariablesForRect nr = r = new VariablesForRect();
			r.rect = ve;
			final Rect rect = ve.getFrame(null);
			r.variableX = new ClVariable(ve.toString() + ".x", rect.x) {

				boolean initialized = false;

				@Override
				public void change_value(double value) {
					double diff = Math.abs(this.value() - value);
					if (diff > 20 && initialized)
						return;
					super.change_value(value);
					if (initialized && diff > -1) {
						postEditQueue.queueSingleUpdate(nr, instance.updateFrameFromVariables.updateable(nr));
					} else
						initialized = true;
				}
			};
			r.variableY = new ClVariable(ve.toString() + ".y", rect.y) {
				boolean initialized = false;

				@Override
				public void change_value(double value) {
					double diff = Math.abs(this.value() - value);
					if (diff > 20 && initialized)
						return;
					super.change_value(value);
					if (initialized && diff > -1) {
						postEditQueue.queueSingleUpdate(nr, instance.updateFrameFromVariables.updateable(nr));
					} else
						initialized = true;
				}
			};
			r.variableW = new ClVariable(ve.toString() + ".w", rect.w) {
				boolean initialized = false;

				@Override
				public void change_value(double value) {
					double diff = Math.abs(this.value() - value);
					if (diff > 20 && initialized)
						return;
					super.change_value(value);
					if (initialized && diff > -1) {
						postEditQueue.queueSingleUpdate(nr, instance.updateFrameFromVariables.updateable(nr));
					} else
						initialized = true;
				}
			};
			r.variableH = new ClVariable(ve.toString() + ".h", rect.h) {
				boolean initialized = false;

				@Override
				public void change_value(double value) {
					double diff = Math.abs(this.value() - value);
					if (diff > 20 && initialized)
						return;
					super.change_value(value);
					if (initialized && diff > -1) {
						postEditQueue.queueSingleUpdate(nr, instance.updateFrameFromVariables.updateable(nr));
					} else
						initialized = true;
				}
			};

			try {
				solver.addStay(r.variableX, ClStrength.weak);
				solver.addStay(r.variableY, ClStrength.weak);
				solver.addStay(r.variableW, ClStrength.weak);
				solver.addStay(r.variableH, ClStrength.weak);
			} catch (ExCLRequiredFailure e) {
				e.printStackTrace();
			} catch (ExCLInternalError e) {
				e.printStackTrace();
			}

			participatesInConstraints.put(ve, r);
		}

		return r;
	}

	public iVisualElement getWellKnownVisualElement(String id) {
		if (id.equals(pluginId))
			return lve;
		return null;
	}

	public void registeredWith(iVisualElement root) {

		this.root = root;
		solver.setAutosolve(false);

		// add a next to root that adds some overrides
		root.addChild(lve);

		lve.setProperty(complexConstraints_plugin, this);
		// register for selection updates? (no, do it in subclass)
		group = root.getProperty(iVisualElement.selectionGroup);

		elementOverride = createElementOverrides();

		GLComponentWindow window = root.getProperty(iVisualElement.enclosingFrame);
	}

	public void resetSolver() {
		solver = new ClSimplexSolver();
		solver.setAutosolve(false);
		Collection<ClConstraint> constraints = offeredConstraint.values();
		try {
			for (ClConstraint c : constraints) {
				solver.addConstraint(c);
			}

			Collection<VariablesForRect> var = participatesInConstraints.values();
			for (VariablesForRect v : var) {
				solver.addStay(v.variableX);
				solver.addStay(v.variableY);
				solver.addStay(v.variableW);
				solver.addStay(v.variableH);
			}
		} catch (ExCLRequiredFailure e) {
			e.printStackTrace();
		} catch (ExCLInternalError e) {
			e.printStackTrace();
		}
	}

	public void setConstraintForElement(iVisualElement ve, ClConstraint nc) {
		try {
			ClConstraint c = getConstraintForElement(ve);

			if (c != null && c != nc) {
				offeredConstraint.remove(ve);
				solver.removeConstraint(c);
			}

			if (c != nc) {
				offeredConstraint.put(ve, nc);
				solver.addConstraint(nc);
			}

			solver.solve();
		} catch (ExCLConstraintNotFound e) {
			e.printStackTrace();
		} catch (ExCLInternalError e) {
			e.printStackTrace();
		} catch (ExCLRequiredFailure e) {
			e.printStackTrace();
		}
	}

	public void setPersistanceInformation(Object o) {
	}

	public void update() {
		updateQueueNow();

	}

	private double roundy(double d) {
		return d;
	}

	@Mirror
	protected void addEditFor(VariablesForRect r, Rect f) {
		try {
			// r.variableX.set_value(f.x);
			solver.addEditVar(r.variableX, ClStrength.strong);
			// r.variableY.set_value(f.y);
			solver.addEditVar(r.variableY, ClStrength.strong);
			// r.variableW.set_value(f.w);
			solver.addEditVar(r.variableW, ClStrength.strong);
			// r.variableH.set_value(f.h);
			solver.addEditVar(r.variableH, ClStrength.strong);
		} catch (ExCLInternalError e) {
			e.printStackTrace();
		} catch (ExCLError e) {
			e.printStackTrace();
		}
	}

	@Mirror
	protected void addSuggestionFor(VariablesForRect r, Rect rr) {

		try {
			solver.suggestValue(r.variableX, rr.x);
			solver.suggestValue(r.variableY, rr.y);
			solver.suggestValue(r.variableW, rr.w);
			solver.suggestValue(r.variableH, rr.h);
		} catch (ExCLInternalError e) {
			e.printStackTrace();
		} catch (ExCLError e) {
			e.printStackTrace();
		}
	}

	protected iVisualElementOverrides createElementOverrides() {
		return new Overrides() {
		}.setVisualElement(lve);
	}

	@Mirror
	protected void updateFrameFromVariables(VariablesForRect r) {
		Rect rect = new Rect(roundy(r.variableX.value()), roundy(r.variableY.value()), roundy(r.variableW.value()), roundy(r.variableH.value()));
		Rect oldRect = r.rect.getFrame(null);
		if (!rect.equals(oldRect)) {
			new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(r.rect).shouldChangeFrame(r.rect, rect, oldRect, true);
		}
	}

	protected boolean updateQueueNow() {
		try {
			if (preEditQueue.getNumTasks() > 0) {
				try {
					solver.solve();

					inLoop = true;
					;//System.out.println(" -- preedit --");
					preEditQueue.update();
					;//System.out.println(" -- begin--");
					solver.beginEdit();
					queue.update();
					;//System.out.println(" info <" + solver.getInternalInfo() + ">");
					;//System.out.println(" -- end--");
					solver.endEdit();
					;//System.out.println(" -- postedit--");
					postEditQueue.update();
					;//System.out.println(" -- finished--");

					resetSolver();

					return true;
				} catch (ExCLInternalError e) {
					e.printStackTrace();
					return false;
				}
			}
		} finally {
			inLoop = false;
		}

		return false;
	}
	@Mirror
	protected void updateVariablesFromFrame(VariablesForRect r, Rect q) {
		Rect oldRect = new Rect(roundy(r.variableX.value()), roundy(r.variableY.value()), roundy(r.variableW.value()), roundy(r.variableH.value()));
		Rect rect = r.rect.getFrame(null);
		// if (!rect.equals(oldRect))
		{
			preEditQueue.queueSingleUpdate(r, instance.addEditFor.updateable(r, q));
			queue.queueSingleUpdate(r, instance.addSuggestionFor.updateable(r, q));
		}
	}

}