package io.sloeber.core.ui;

import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.swt.widgets.Composite;

/**
 * ArduinoPropertyPage is a wrapper class for ArduinoSelectionPage. It wraps this class for the project properties
 * 
 * @author Jan Baeyens
 * 
 */
public class PropertyPage extends AbstractPage {
    BoardSelectionPage page = null;

    @Override
    protected boolean isSingle() {
	return false;
    }

    @Override
    public void createControl(Composite parent) {
	// TODO Auto-generated method stub
	super.createControl(parent);

	this.forEach(222, this);
    }

}
