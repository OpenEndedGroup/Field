package field.core.ui.text.protect;

public class ClassDocumentationProtect {
	static public class Divider {
	}

	static public class Comp {
		public String completes;
		public String shortDocumentation;
		public String longDocumentation;
		public boolean isTitle = false;

		public Comp setTitle(boolean isTitle) {
			this.isTitle = isTitle;
			return this;
		}

		public Comp(String completes, String shortDocumentation) {
			this.completes = completes;
			this.shortDocumentation = shortDocumentation;
		}

		public Comp(String longDocumentation) {
			this.longDocumentation = longDocumentation;
		}
	}

	static public class CompProxy extends Comp {
		public final Object proxyTo;

		public CompProxy(Object proxyTo) {
			super("", "");
			this.proxyTo = proxyTo;
		}
	}
}
