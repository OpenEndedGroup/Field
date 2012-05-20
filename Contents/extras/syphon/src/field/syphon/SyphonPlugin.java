package field.syphon;

import field.core.plugins.BaseSimplePlugin;

public class SyphonPlugin extends BaseSimplePlugin{

	/**
	 * this is just here to load the library (and start the server discovery, which blocks the main thread) early
	 */
	static {
		System.loadLibrary("syphon");
	}

	@Override
	protected String getPluginNameImpl() {
		return "syphon";
	}

	
}
