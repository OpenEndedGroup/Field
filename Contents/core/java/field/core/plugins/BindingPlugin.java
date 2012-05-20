package field.core.plugins;

import java.util.List;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.plugins.python.PythonPluginEditor;
import field.math.graph.GraphNodeSearching.VisitCode;

@Woven
public class BindingPlugin extends BaseSimplePlugin {

	@Override
	protected String getPluginNameImpl() {
		return "binding";
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);
	}

	boolean first = true;

	@Override
	public void update() {
		super.update();
		if (first) {
			first = false;
			addAll();

		}
	}

	private void addAll() {
		List<iVisualElement> all = StandardFluidSheet.allVisualElements(root);
		for (iVisualElement e : all) {
			added(e);
		}
	}

	@NextUpdate(delay=1)
	protected void addAllNext() {
		List<iVisualElement> all = StandardFluidSheet.allVisualElements(root);
		for (iVisualElement e : all) {
			added(e);
		}
	}

	protected void added(iVisualElement e) {
		String b = e.getProperty(iVisualElement.boundTo);
		if (b != null && b.trim().length() > 0) {

			;//System.out.println(" initializing boundto with <" + b + "> for <" + e + ">");

			PythonPluginEditor.makeBoxLocalEverywhere(b.trim());
			List<iVisualElement> c = e.getChildren();
			for (iVisualElement ee : c)
				ee.setProperty(new VisualElementProperty<iVisualElement>(b.trim() + "_"), e);
			e.setProperty(new VisualElementProperty<iVisualElement>(b.trim() + "_"), e);
		}
	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		return new DefaultOverride() {
			@Override
			public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
				if (prop.equals(iVisualElement.boundTo)) {
					String was = source.getProperty(iVisualElement.boundTo);

					if (was == null || was.trim().length() == 0) {
						if (to.get() == null || ((String) to.get()).trim().length() == 0) {
						} else {
							PythonPluginEditor.makeBoxLocalEverywhere(((String) to.get()).trim());
							List<iVisualElement> c = source.getChildren();
							for (iVisualElement ee : c)
								ee.setProperty(new VisualElementProperty<iVisualElement>(((String) to.get()).trim() + "_"), source);
							source.setProperty(new VisualElementProperty<iVisualElement>(((String) to.get()).trim() + "_"), source);
						}
					} else {
						if (to.get() == null || ((String) to.get()).trim().length() == 0) {
							PythonPluginEditor.removeBoxLocalEverywhere(was.trim());
							List<iVisualElement> c = source.getChildren();
							for (iVisualElement ee : c)
								ee.deleteProperty(new VisualElementProperty<iVisualElement>(was + "_"));
							source.deleteProperty(new VisualElementProperty<iVisualElement>(was + "_"));

							addAllNext();

						} else {
							PythonPluginEditor.removeBoxLocalEverywhere(was.trim());
							List<iVisualElement> c = source.getChildren();
							for (iVisualElement ee : c)
								ee.deleteProperty(new VisualElementProperty<iVisualElement>(was + "_"));
							source.deleteProperty(new VisualElementProperty<iVisualElement>(was + "_"));

							PythonPluginEditor.makeBoxLocalEverywhere(((String) to.get()).trim());
							for (iVisualElement ee : c)
								ee.setProperty(new VisualElementProperty<iVisualElement>(((String) to.get()).trim() + "_"), source);
							source.setProperty(new VisualElementProperty<iVisualElement>(((String) to.get()).trim() + "_"), source);

						}

					}
				}
				return super.setProperty(source, prop, to);
			}

			@Override
			public VisitCode deleted(iVisualElement source) {

				String was = source.getProperty(iVisualElement.boundTo);
				;//System.out.println(" handling deleted for <" + source + " -> " + was);

				if (was == null || was.trim().length() == 0) {
				} else {

					;//System.out.println(" children are <" + source.getChildren() + ">");
					PythonPluginEditor.removeBoxLocalEverywhere(was.trim());
					List<iVisualElement> c = source.getChildren();
					for (iVisualElement ee : c)
						ee.deleteProperty(new VisualElementProperty<iVisualElement>(was + "_"));
					source.deleteProperty(new VisualElementProperty<iVisualElement>(was + "_"));
				}

				addAllNext();

				return super.deleted(source);
			}

			@Override
			public VisitCode added(iVisualElement source) {
				BindingPlugin.this.added(source);
				return super.added(source);
			}

		};
	}

}
