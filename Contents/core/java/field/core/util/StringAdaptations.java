package field.core.util;

import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem.Promise;
import field.math.abstraction.iFloatProvider;
import field.namespace.generic.Adaptation;
import field.namespace.key.OKey;

public class StringAdaptations {

	static public final OKey<Promise> stringAdaptation_promise = new OKey<Promise>("stringAdaptation_promise").rootSet(null);
	
	static public void register(Adaptation adaptation) {

		adaptation.declare(String.class, Double.class, new Adaptation.iAdaptor<String, Double>(){

			public Double adapt(Class<String> from, Class<Double> to, String object) {
				return new Double(object);
			}

		}, new iFloatProvider.Constant(1));

		adaptation.declare(String.class, Double.TYPE, new Adaptation.iAdaptor<String, Double>(){

			public Double adapt(Class<String> from, Class<Double> to, String object) {
				return new Double(object);
			}

		}, new iFloatProvider.Constant(1));
		
		adaptation.declare(String.class, Integer.TYPE, new Adaptation.iAdaptor<String, Integer>(){

			public Integer adapt(Class<String> from, Class<Integer> to, String object) {
				return new Integer(object);
			}

		}, new iFloatProvider.Constant(1));
		
		
		adaptation.declare(Double.class, Integer.class, new Adaptation.iAdaptor<Double, Integer>(){

			public Integer adapt(Class<Double> from, Class<Integer> to, Double object) {
				return object.intValue();
			}

		}, new iFloatProvider.Constant(1));
		
		adaptation.declare(Double.class, Float.class, new Adaptation.iAdaptor<Double, Float>(){

			public Float adapt(Class<Double> from, Class<Float> to, Double object) {
				return object.floatValue();
			}

		}, new iFloatProvider.Constant(1));

		adaptation.declare(Double.class, Float.TYPE, new Adaptation.iAdaptor<Double, Float>(){

			public Float adapt(Class<Double> from, Class<Float> to, Double object) {
				return object.floatValue();
			}

		}, new iFloatProvider.Constant(1));

		adaptation.declare(String.class, Boolean.class, new Adaptation.iAdaptor<String, Boolean>(){

			public Boolean adapt(Class<String> from, Class<Boolean> to, String object) {
				return  Boolean.parseBoolean(object);
			}

		}, new iFloatProvider.Constant(1));

		adaptation.declare(String.class, Boolean.TYPE, new Adaptation.iAdaptor<String, Boolean>(){

			public Boolean adapt(Class<String> from, Class<Boolean> to, String object) {
				return  Boolean.parseBoolean(object);
			}

		}, new iFloatProvider.Constant(1));

	}
		

	public static Object perhapsPython(String object) {
		Promise promise = stringAdaptation_promise.get(null);
		if (promise == null) return object;
		if (object.startsWith("^"))
		{
			object = object.substring(1);
			if (promise!=null)
			{
				promise.beginExecute();
				promise.willExecuteSubstring(object, -1, -1);
				Object ret = PythonInterface.getPythonInterface().eval(object);
				promise.endExecute();
				return ret;
			}
		}
		return object;
	}
}
