package field.core.execution;

import java.util.HashMap;

import field.core.execution.PhantomFluidSheet.iPhasicForElement;
import field.math.abstraction.iFloatProvider;


public class PhantomCache {

	HashMap<String, PhantomFluidSheet> loaded = new HashMap<String, PhantomFluidSheet>();
	
	public PhantomCache()
	{
	}
	
	public iPhasicForElement phasicForElement(String simpleSheetReference, iFloatProvider prov, boolean local)
	{
		String[] ref = simpleSheetReference.split("\\?");
		
		if (!ref[0].endsWith("sheet.xml")) ref[0]= ref[0]+"/sheet.xml";
		
		PhantomFluidSheet phantom = loaded.get(ref[0]);
		if (phantom == null)
		{
			phantom = new PhantomFluidSheet(ref[0]);
			loaded.put(ref[0], phantom);
		}
		
		return phantom.phasicForDescription(ref[1],prov, local);
		
	}
	
}
