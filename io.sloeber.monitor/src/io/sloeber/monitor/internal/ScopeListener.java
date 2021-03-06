package io.sloeber.monitor.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.common.MessageConsumer;
import io.sloeber.monitor.oscilloscope.multichannel.Oscilloscope;
import io.sloeber.monitor.views.Messages;

public class ScopeListener implements MessageConsumer {

    /**
     * myScope The scope representing the data
     */
    Oscilloscope myScope;
    /**
     * myReceivedScopeData is a fixed size buffer holding the bytes that have
     * been received from the com port
     * 
     */
    private ByteBuffer myReceivedScopeData = ByteBuffer.allocate(2000); // if we
									// are
									// more
									// than
									// this
									// behind
									// we
									// will
									// loose
									// data
    /**
     * myEndPosition points to the last byte that is still valid in the buffer.
     * This is needed because myReceivedScopeData is fixed size.
     * 
     */
    private int myEndPosition = 0;

    public ScopeListener(Oscilloscope oscilloscope) {
	this.myReceivedScopeData.order(ByteOrder.LITTLE_ENDIAN);
	this.myScope = oscilloscope;
    }

    /**
     * Here the message comes in from the serial port. If there is not enough
     * place in If there is enough place in myReceivedScopeData the data is
     * added to myReceivedScopeData to hold all the data; all the data (that in
     * myReceivedScopeData and in s) is ignored and a warning is dumped If there
     * is enough place in myReceivedScopeData the data is added to
     * myReceivedScopeData and myReceivedScopeData is scanned for scope data
     * 
     */
    @Override
    public synchronized void message(byte[] newData) {
	if (this.myScope.isDisposed())
	    return;
	if (this.myEndPosition + newData.length >= this.myReceivedScopeData.capacity()) {
	    this.myEndPosition = 0;
	    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.serialListenerScopeSkippingData));
	} else {
	    this.myReceivedScopeData.position(this.myEndPosition);
	    this.myReceivedScopeData.put(newData, 0, newData.length);
	    this.myEndPosition = this.myReceivedScopeData.position();
	    internalExtractAndProcessScopeData();
	}

    }

    @Override
    public void dispose() {
	this.myScope = null;
	this.myReceivedScopeData = null;
    }

    /**
     * AddValuesToOsciloscope This method makes the scope to draw the values
     * that have been delivered to the scope
     */
    public void AddValuesToOsciloscope() {
	if (this.myScope.isDisposed())
	    return;
	// run on the gui thread only
	Display.getDefault().asyncExec(new Runnable() {
	    @Override
	    public void run() {
		try {
		    ScopeListener.this.myScope.getDispatcher(0).hookPulse(ScopeListener.this.myScope, 1);
		    ScopeListener.this.myScope.redraw();
		} catch (Exception e) {
		    // ignore as we get errors when closing down
		}
	    }
	});

    }

    /**
     * internalExtractAndProcessScopeData scans for scope data in
     * myReceivedScopeData If data is found it is send to the scope all data
     * that has been scanned is removed from myReceivedScopeData
     */
    private void internalExtractAndProcessScopeData() {
	int lastFoundData = this.myEndPosition - 6;
	if (lastFoundData < 0)
	    return;
	// Scan for scope data
	for (int scannnedDataPointer = 0; scannnedDataPointer < this.myEndPosition - 8; scannnedDataPointer++) {
	    if (this.myReceivedScopeData.getShort(scannnedDataPointer) == Const.SCOPE_START_DATA) {
		// we have a hit.
		lastFoundData = scannnedDataPointer;
		scannnedDataPointer = scannnedDataPointer + 2;
		int bytestoRead = this.myReceivedScopeData.getShort(scannnedDataPointer);
		if ((bytestoRead < 0) || (bytestoRead > 10 * 2)) {
		    Common.log(
			    new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.serialListenerErrorInputPart1
				    + bytestoRead / 2 + ' ' + Messages.serialListenerErrorInputPart2));
		} else {
		    if (bytestoRead + 2 + scannnedDataPointer < this.myEndPosition) {
			// all data is available
			int myNumDataSetsToReceive = this.myReceivedScopeData.getShort(scannnedDataPointer) / 2 + 0;
			for (int CurData = 0; CurData < myNumDataSetsToReceive; CurData++) {
			    int data = this.myReceivedScopeData.getShort(scannnedDataPointer + 2 + CurData * 2);
			    this.myScope.setValue(CurData, data);
			}
			AddValuesToOsciloscope();
			scannnedDataPointer = scannnedDataPointer + 2 + myNumDataSetsToReceive * 2;
			lastFoundData = scannnedDataPointer;

		    }
		}
	    }
	}
	// remove all the scanned data
	for (int curByte = 0; curByte <= this.myEndPosition - lastFoundData; curByte++) {
	    try {
		this.myReceivedScopeData.put(curByte, this.myReceivedScopeData.get(curByte + lastFoundData));
	    } catch (IndexOutOfBoundsException e) {
		Common.log(
			new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.scopeListenerBufferOverflow, e));
	    }

	}
	this.myEndPosition -= lastFoundData;

    }

    @Override
    public void event(String event) {
	// ignore events

    }
}
