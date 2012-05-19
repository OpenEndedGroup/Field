package field.core.dispatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.BaseRef;
import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.log.ElementInvocationLogging;
import field.core.plugins.log.Logging;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.math.graph.TopologyViewOfGraphNodes;
import field.namespace.dispatch.DispatchOverTopology;
import field.util.Dict.Prop;

@GenerateMethods
public interface iVisualElementOverrides {

	static class Adaptor implements iVisualElementOverrides {
		public VisitCode added(iVisualElement newSource) {
			return VisitCode.cont;
		}

		public VisitCode beginExecution(iVisualElement source) {
			return VisitCode.cont;
		}

		public VisitCode deleted(iVisualElement source) {
			return VisitCode.cont;
		}

		public <T> VisitCode deleteProperty(iVisualElement source, VisualElementProperty<T> prop) {
			return VisitCode.cont;
		}

		public VisitCode endExecution(iVisualElement source) {
			return VisitCode.cont;
		}

		public <T> VisitCode getProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> prop, Ref<T> ref) {
			return VisitCode.cont;
		}

		public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event) {
			return VisitCode.cont;
		}

		public VisitCode inspectablePropertiesFor(iVisualElement source, List<Prop> properties) {
			return VisitCode.cont;
		}

		public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is) {
			return VisitCode.cont;
		}

		public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items) {
			return VisitCode.cont;
		}

		public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible) {
			return VisitCode.cont;
		}

		public VisitCode prepareForSave() {
			return VisitCode.cont;
		}

		public <T> VisitCode setProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> prop, Ref<T> to) {
			return VisitCode.cont;
		}

		public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
			return VisitCode.cont;
		}

	}

	static public class DefaultOverride extends Adaptor implements iDefaultOverride {
		public iVisualElement forElement;

		public DefaultOverride() {
		}

		@Override
		public VisitCode deleted(iVisualElement source) {
			if (source == forElement) {
				source.dispose();
			}
			return VisitCode.cont;
		}

		@Override
		public <T> VisitCode deleteProperty(iVisualElement source, VisualElementProperty<T> prop) {
			if (source == forElement) {
				VisualElementProperty<T> a = prop.getAliasedTo();
				while (a != null) {
					prop = a;
					a = a.getAliasedTo();
				}

				forElement.deleteProperty(prop);
			}
			return VisitCode.cont;
		}

		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
			if ((ref.to == null || (source == forElement)) && forElement != null) {

				VisualElementProperty<T> a = prop.getAliasedTo();
				while (a != null) {
					prop = a;
					a = a.getAliasedTo();
				}
				assert forElement != null : "problem in class " + this.getClass();
				T property = forElement.getProperty(prop);
				if (property != null) {
					ref.set(property, forElement);
				}
			}
			return VisitCode.cont;
		}

		@Override
		public <T> VisitCode setProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> to) {
			if (source == forElement) {
				VisualElementProperty<T> a = prop.getAliasedTo();
				while (a != null) {
					prop = a;
					a = a.getAliasedTo();
				}

				if (Logging.enabled())
					Logging.logging.addEvent(new ElementInvocationLogging.DidSetProperty(prop, source, to.to, forElement.getProperty(prop)));
				forElement.setProperty(prop, to.get());
			}
			return VisitCode.cont;
		}

		public DefaultOverride setVisualElement(iVisualElement ve) {
			forElement = ve;
			return this;
		}

		@Override
		public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now) {
			if (source == forElement) {
				forElement.setFrame(newFrame);
				return VisitCode.cont;
			}
			return VisitCode.cont;
		}
	}

	static public interface iDefaultOverride {
		public iDefaultOverride setVisualElement(iVisualElement ve);
	}

	static class MakeDispatchProxy {

		static public int dispatchBackwardCount = 0;

		static public int dispatchForwardCount = 0;

		public iVisualElementOverrides getBackwardsOverrideProxyFor(final iVisualElement element) {
			final TopologyViewOfGraphNodes<iVisualElement> topView = new TopologyViewOfGraphNodes<iVisualElement>(true);

			return (iVisualElementOverrides) Proxy.newProxyInstance(element.getClass().getClassLoader(), new Class[] { iVisualElementOverrides.class }, new InvocationHandler() {

				DispatchOverTopology<iVisualElement> dispatch = new DispatchOverTopology<iVisualElement>(topView);

				DispatchOverTopology<iVisualElement>.Raw raw = dispatch.new Raw(true) {
					@Override
					public Object getObject(iVisualElement e) {
						dispatchBackwardCount++;
						return e.getProperty(iVisualElement.overrides);
					}

				};

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {

					VisitCode o = raw.dispatch(arg1, element, arg2);

					return o;
				}
			});
		}

		public iVisualElementOverrides getOverrideProxyFor(final iVisualElement element) {
			final TopologyViewOfGraphNodes<iVisualElement> topView = new TopologyViewOfGraphNodes<iVisualElement>();

			return (iVisualElementOverrides) Proxy.newProxyInstance(element.getClass().getClassLoader(), new Class[] { iVisualElementOverrides.class }, new InvocationHandler() {

				DispatchOverTopology<iVisualElement> dispatch = new DispatchOverTopology<iVisualElement>(topView);

				DispatchOverTopology<iVisualElement>.Raw raw = dispatch.new Raw(true) {
					@Override
					public Object getObject(iVisualElement e) {
						dispatchForwardCount++;
						iVisualElementOverrides o = e.getProperty(iVisualElement.overrides);
						return o;
					}

					

				};

				public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
					arg1.setAccessible(true);
					VisitCode o = raw.dispatch(arg1, element, arg2);
					return o;
				}
			});
		}
	}

	static public class Ref<T> extends BaseRef<T> {
		public iVisualElement storageSource;

		public Ref(T to) {
			super(to);
		}

		public iVisualElement getStorageSource() {
			return storageSource;
		}

		public Ref<T> set(T to, iVisualElement storedBy) {
			this.to = to;
			this.storageSource = storedBy;
			unset = false;
			return this;
		}

	}

	static public VisualElementContextTopology topology = new VisualElementContextTopology(null);

//	static public iVisualElementOverrides_m backward = new iVisualElementOverrides_m(new Dispatch<iVisualElement, iVisualElementOverrides>(topology).getBackwardsOverrideProxyFor(iVisualElementOverrides.class));

	static public iVisualElementOverrides_m backward = new iVisualElementOverrides_m(new FastVisualElementOverridesDispatch(true));
	static public iVisualElementOverrides_m forward = new iVisualElementOverrides_m(new FastVisualElementOverridesDispatch(false));

//	static public iVisualElementOverrides_m forward = new iVisualElementOverrides_m(new Dispatch<iVisualElement, iVisualElementOverrides>(topology).getOverrideProxyFor(iVisualElementOverrides.class));
//	static public iVisualElementOverrides_m forwardAbove = new iVisualElementOverrides_m(new Dispatch<iVisualElement, iVisualElementOverrides>(topology).getAboveOverrideProxyFor(iVisualElementOverrides.class));
	static public iVisualElementOverrides_m forwardAbove = new iVisualElementOverrides_m(new FastVisualElementOverridesDispatchAbove(false));

	@Mirror
	public VisitCode added(iVisualElement newSource);

	@Mirror
	public VisitCode beginExecution(iVisualElement source);

	@Mirror
	public VisitCode deleted(iVisualElement source);

	@Mirror
	public <T> VisitCode deleteProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> prop);

	@Mirror
	public VisitCode endExecution(iVisualElement source);

	@Mirror
	public <T> VisitCode getProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> prop, Ref<T> ref);

	@Mirror
	public VisitCode handleKeyboardEvent(iVisualElement newSource, Event event);

	@Mirror
	public VisitCode inspectablePropertiesFor(iVisualElement source, List<Prop> properties);

	@Mirror
	public VisitCode isHit(iVisualElement source, Event event, Ref<Boolean> is);

	@Mirror
	public VisitCode menuItemsFor(iVisualElement source, Map<String, iUpdateable> items);

	@Mirror
	public VisitCode paintNow(iVisualElement source, Rect bounds, boolean visible);

	@Mirror
	public VisitCode prepareForSave();

	@Mirror
	public <T> VisitCode setProperty(iVisualElement source, iVisualElement.VisualElementProperty<T> prop, Ref<T> to);

	@Mirror
	public VisitCode shouldChangeFrame(iVisualElement source, Rect newFrame, Rect oldFrame, boolean now);

}
