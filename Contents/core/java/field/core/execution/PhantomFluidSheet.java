package field.core.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import field.core.StandardFluidSheet;
import field.core.iHasVisualElementRoot;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.windowing.components.PlainDraggableComponent;
import field.launch.SystemProperties;
import field.launch.iPhasic;
import field.math.abstraction.iFloatProvider;
import field.namespace.generic.Generics.Triple;
import field.util.ANSIColorUtils;

/**
 * a fluid sheet that can be open or closed \u2014 we do this by saving and loading
 * the sheet
 *
 * @author marc
 *
 */
public class PhantomFluidSheet implements iHasVisualElementRoot {

	public interface iPhasicForElement extends iPhasic {
		public PhantomFluidSheet getPhantom();

		public iVisualElement getVisualElement();
	}

	private FacelessFluidSheet faceless;

	private String filename;

	private StandardFluidSheet facefull;

	private BasicRunner runner;

	private float lastT;

	private TimeSystem ts;

	private Triple<VisualElement, PlainDraggableComponent, TemporalSliderOverrides> slider;

	private String windowIdentifier;


	boolean closed = false;

	boolean useTimeSystem = false;

	public PhantomFluidSheet(final String filename) {
		this(filename, true, false);
	}

	public PhantomFluidSheet(final String filename, boolean useTimeSystem, boolean startOpen) {
		this.useTimeSystem = useTimeSystem;
		this.filename = filename;

		if (startOpen) {
			facefull = StandardFluidSheet.versionedScratch(filename);
			runner = facefull.getBasicRunner();
			
			
		} else {
			faceless = new FacelessFluidSheet();
			faceless.standard(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml");
			runner = new BasicRunner(faceless.getRoot().getProperty(PythonScriptingSystem.pythonScriptingSystem), 1);
		}

		if (useTimeSystem) {

			ts = new TimeSystem();
			getRoot().setProperty(TemporalSliderOverrides.currentTimeSystem, ts);

			Triple<VisualElement, PlainDraggableComponent, TemporalSliderOverrides> created = TemporalSliderOverrides.newTemporalSlider("time", getRoot());
			if (!startOpen)
				created.right.drivenByTimeSystem(new Ref<TimeSystem>(ts));
			slider = created;
			getRoot().setProperty(TemporalSliderOverrides.currentTimeSystem, ts);
		}
	}

	public void close() {
		if (facefull != null) {
			facefull.close();

		} else if (faceless != null) {
			try {
				if (!isNosave())
					faceless.save(new BufferedWriter(new FileWriter(new File(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml"))));
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			faceless = null;
		}
		closed = true;

	}

	public void closeUI() {
		if (closed)
			return;
		if (faceless == null)
			fromVisual();
	}

	public iVisualElement findElement(String description) {
		return StandardFluidSheet.findVisualElementWithName(getRoot(), description);
	}

	public iVisualElement getRoot() {
		if (faceless != null)
			return faceless.getRoot();
		return facefull.getRoot();
	}

	public TimeSystem getTimeSystem()
	{
		return ts;
	}

	public StandardFluidSheet getUI() {
		if (closed)
			return null;
		return facefull;
	}

	public PhantomFluidSheet openUI() {
		if (closed)
			return this;

		if (facefull == null)
			toVisual();
		return this;
	}

	public iPhasicForElement phasicForDescription(final String description, final iFloatProvider fp, final boolean local) {

		// right now this is very simple (could be xpath, if we needed
		// it)

		return new iPhasicForElement() {
			private Ref<PythonScriptingSystem> refPss;

			boolean started = false;

			iExecutesPromise runner = null;

			iVisualElement running = null;

			boolean first = true;

			public void begin() {
				rebegin();
			}

			public void close() {
			}

			public void end() {
				if (runner != null)
					runner.stopAll(0);
			}

			public PhantomFluidSheet getPhantom() {
				return PhantomFluidSheet.this;
			}

			public iVisualElement getVisualElement() {
				return running;
			}

			public void open() {
			}

			public void rebegin() {
				if (started) {
					runner.stopAll(0);
				}

				running = findElement(description);
				if (running != null) {

					refPss = new Ref<PythonScriptingSystem>(null);
					new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(running).getProperty(running, PythonScriptingSystem.pythonScriptingSystem, refPss);
					assert refPss.get() != null;

					Ref<iExecutesPromise> refRunner = new Ref<iExecutesPromise>(null);
					if (!local) {
						new iVisualElementOverrides.MakeDispatchProxy().getOverrideProxyFor(running).getProperty(running, iExecutesPromise.promiseExecution, refRunner);
						assert refRunner.get() != null;
						//System.err.println(" runner is <" + refRunner.get().getClass() + ">");
					} else {
						refRunner.set(new BasicRunner(getRoot().getProperty(PythonScriptingSystem.pythonScriptingSystem), 1));
					}

					first = true;

					runner = refRunner.get();
				} else {
					System.err.println(" no element for <" + description + "> in sheet <" + filename + ">");
				}

			}

			public void update() {

				if (first) {
					System.err.println(" pss is <" + refPss.get() + ">");
					Promise promise = refPss.get().promiseForKey(running);

					if (promise != null) {
						runner.addActive(fp, promise);

						started = true;
					} else
						System.err.println(" no promis for <" + running + ">");
					first = false;
				}

				if (runner != null) {
					if (local)
						((BasicRunner) runner).update(0);
				} else {
				}
			}

		};

	}
	
	public void saveFacefull() throws IOException
	{
//		facefull.save(new BufferedWriter(new FileWriter(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml"), 1024 * 16 * 1024));
		facefull.saveTwoPart(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml");
	}

	public void setFilename(String f) {
		setFilename(f, true);
	}

	public void setFilename(String f, boolean b) {
		filename = f;
		if (facefull != null) {

			;//System.out.println(ANSIColorUtils.red(" setting filename on current sheet"));
			facefull.setFilename(f);
			if (!isNosave())
				try {
					;//System.out.println(ANSIColorUtils.red(" saving sheet to :"+(filename + "/sheet.xml")));
					facefull.save(new BufferedWriter(new FileWriter(filename + "/sheet.xml"), 1024 * 16 * 1024));
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				;//System.out.println(" about to call close");
				
			facefull.close();

			facefull = null;

			;//System.out.println(" about to go visual again");

			if (b)
				toVisual();
			else
				close();
		}
	}
	
	public PhantomFluidSheet setUseTimeSystem(boolean useTimeSystem) {
		this.useTimeSystem = useTimeSystem;
		return this;
	}

	public void setWindowIdentifier(String name) {
		windowIdentifier = name;
	}

	public void update(float time) {
		if (closed)
			return;

		if (facefull != null)
			return;

		if (!useTimeSystem) {
			lastT = time;

		//	System.err.println(" updating at time <"+time+">");

			runner.update(time);
			if (faceless != null)
				faceless.update();
		} else {
			ts.update();
			// if faceless
			double e = ts.evaluate();

			slider.left.setFrame(new Rect((float) e, 0, 10, 0));
			runner.update((float) e);

			if (faceless != null)
				faceless.update();
		}
	}

	protected void fromVisual() {
		try {
			if (!isNosave())
				facefull.save(new BufferedWriter(new FileWriter(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml"), 1024 * 16 * 1024));
			facefull.close();
			facefull = null;


			faceless = new FacelessFluidSheet();
			faceless.standard(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml");

			if (runner != null) {
				runner.stopAll(lastT);
			}
			if (slider != null) {
				slider.right.driveTimeSystem(new Ref<TimeSystem>(ts));
			}
			runner = new BasicRunner(faceless.getRoot().getProperty(PythonScriptingSystem.pythonScriptingSystem), 1);

			if (useTimeSystem) {
				Triple<VisualElement, PlainDraggableComponent, TemporalSliderOverrides> created = TemporalSliderOverrides.newTemporalSlider("time", getRoot());
				created.right.drivenByTimeSystem(new Ref<TimeSystem>(ts));
				slider = created;
				getRoot().setProperty(TemporalSliderOverrides.currentTimeSystem, ts);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void toVisual() {
		try {
			if (faceless != null)
				try {
					if (!isNosave())
						faceless.save(new BufferedWriter(new FileWriter(new File(SystemProperties.getDirProperty("versioning.dir") + "/" + filename + "/sheet.xml"))));
				} catch (FileNotFoundException e) {
				}
			faceless = null;

			facefull = StandardFluidSheet.versionedScratch(filename);

			if (runner != null) {
				runner.stopAll(lastT);
			}

			if (slider != null) {
				slider.right.driveTimeSystem(new Ref<TimeSystem>(ts));
			}

			runner = facefull.getBasicRunner();

			if (useTimeSystem) {
				// need to connect runner to time slider
				// right here
				Triple<VisualElement, PlainDraggableComponent, TemporalSliderOverrides> created = TemporalSliderOverrides.newTemporalSlider("time", getRoot());
				created.right.drivenByTimeSystem(new Ref<TimeSystem>(ts));
				slider = created;
				getRoot().setProperty(TemporalSliderOverrides.currentTimeSystem, ts);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isNosave() {
		return SystemProperties.getIntProperty("nosave", 0)==1;
	}

}
