package field.core.plugins.history;

import java.util.Map;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.help.ContextualHelp;
import field.core.plugins.help.HelpBrowser;
import field.core.plugins.history.ElementFileSystemTree.SheetDropSupport;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;

@Woven
public class ElementFileSystemTreePlugin extends BaseSimplePlugin {

	static public final VisualElementProperty<ElementFileSystemTreePlugin> fileSystemTree = new VisualElementProperty<ElementFileSystemTreePlugin>("fileSystemTree");
	private ElementFileSystemTree efst;

	@Override
	protected String getPluginNameImpl() {
		return "efs";
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);

		efst = new ElementFileSystemTree();

		new SheetDropSupport(iVisualElement.enclosingFrame.get(root).getCanvas(), root);

		fileSystemTree.set(root, root, this);

		installHelpBrowser(root);
	}

	@NextUpdate(delay = 3)
	private void installHelpBrowser(final iVisualElement root) {
		HelpBrowser h = HelpBrowser.helpBrowser.get(root);
		ContextualHelp ch = h.getContextualHelp();
		ch.addContextualHelpForWidget("filesyste", efst.tree, ch.providerForStaticMarkdownResource("contextual/filesystem.md"), 50);
	}

}
