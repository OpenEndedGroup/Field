package field.graphics.imageprocessing;

import field.graphics.core.BasicUtilities;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicUtilities.TwoPassElement;
import field.math.abstraction.iProvider;

public interface iImageProcessor {

	public abstract void addChild(iSceneListElement e);

	public abstract iProvider<Integer> getOutput(int num);

	public abstract BasicUtilities.TwoPassElement getOutputElement(int num);

}