package field.extras.graphics;

import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.plugins.BaseSimplePlugin;

public class FieldGraphicsPlugin extends BaseSimplePlugin {

	@Override
	protected String getPluginNameImpl() {
		return "fieldgraphics";
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);

		System.out.println(" -- field graphics plugin initializing --");
		
		new FieldExtensionsToBasic();

		PythonInterface.getPythonInterface().execString("from AdvancedGraphics import *");
		PythonInterface.getPythonInterface().execString("from field.extras.graphics.FieldExtensionsToBasic import _vertex");
		PythonInterface.getPythonInterface().execString("from field.extras.graphics.FieldExtensionsToBasic import _color");
		PythonInterface.getPythonInterface().execString("from field.extras.graphics.FieldExtensionsToBasic import _normal");
		PythonInterface.getPythonInterface().execString("from field.extras.graphics.FieldExtensionsToBasic import _texture0");
		PythonInterface.getPythonInterface().execString("from field.extras.graphics.FieldExtensionsToBasic import makeShaderFromElement");
		PythonInterface.getPythonInterface().execString("from field.extras.graphics.FieldExtensionsToBasic import makeTextureForArray");
		
		PythonInterface.getPythonInterface().execString("from field.graphics.core import Base");
		PythonInterface.getPythonInterface().execString("from field.graphics.core.Base import StandardPass");
		PythonInterface.getPythonInterface().execString("from field.graphics.core.Base.StandardPass import *");

		PythonInterface.getPythonInterface().execString("from field.graphics.core.BasicUtilities import *");
		PythonInterface.getPythonInterface().execString("from field.graphics.core.AdvancedTextures import BaseFastNoStorageTexture");

	}
}
