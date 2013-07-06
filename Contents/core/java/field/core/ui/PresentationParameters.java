package field.core.ui;

import field.bytecode.protect.Notable;
import field.core.util.BetterPythonConstructors.SynthesizeFactory;

@Notable
@SynthesizeFactory
    public class PresentationParameters {

    public boolean hidden = false;
    public boolean clickBecomesOptionClick = false;
    public boolean rightClickBecomesSpace = false;
    public boolean rightClickBecomesOptionClick = false;
    
    public boolean fixedHeight = false;
    public boolean fixedWidth = false;
    public boolean fixedPosition = false;

    public boolean notSelectable = false;

    public boolean always = false;

}
