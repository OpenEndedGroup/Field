package field.core.dispatch;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import field.namespace.context.CT.ContextTopology;
import field.namespace.context.CT.iContextStorage;



public class VisualElementContextTopology extends ContextTopology<iVisualElement, iVisualElementOverrides> {

	private iVisualElement rootElement;

	public VisualElementContextTopology(iVisualElement root) {
		super(iVisualElement.class, iVisualElementOverrides.class);
		this.rootElement = root;
		this.storage = new iContextStorage<iVisualElement, iVisualElementOverrides>() {
			public iVisualElementOverrides get(iVisualElement at, Method m) {
				return at.getProperty(iVisualElement.overrides);
			}
		};
	}

	ThreadLocal<Stack<iVisualElement>> atStack = new ThreadLocal<Stack<iVisualElement>>() {
		@Override
		protected Stack<iVisualElement> initialValue() {
			return new Stack<iVisualElement>();
		}
	};

	public void begin(iVisualElement e) {
		Stack<iVisualElement> stack = atStack.get();
		stack.push(e);
		setAt(e);
	}

	public void end(iVisualElement e) {
		Stack<iVisualElement> stack = atStack.get();
		assert stack.peek() == e;
		setAt(stack.pop());
	}

	@Override
	public Set<iVisualElement> childrenOf(iVisualElement p) {
		return new LinkedHashSet<iVisualElement>(p.getChildren());
	}

	@Override
	public void deleteChild(iVisualElement parent, iVisualElement child) {
	}

	@Override
	public Set<iVisualElement> parentsOf(iVisualElement k) {
		return new LinkedHashSet<iVisualElement>((Collection<? extends iVisualElement>) k.getParents());
	}

	@Override
	public iVisualElement root() {
		return rootElement;
	}

}
