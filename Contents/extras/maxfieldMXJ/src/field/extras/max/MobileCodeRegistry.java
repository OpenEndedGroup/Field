package field.extras.max;

import org.python.core.PyDictionary;

public class MobileCodeRegistry {

	public class Environment
	{
		String name = "<unset>";
		PyDictionary globals = new PyDictionary();
		
		public Environment(String name) {
			this.name = name;
		}
	}
	
}
