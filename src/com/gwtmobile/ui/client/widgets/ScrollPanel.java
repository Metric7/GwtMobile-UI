/*
 * Copyright (c) 2010 Zhihua (Dennis) Jiang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gwtmobile.ui.client.widgets;

import java.util.Iterator;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtmobile.ui.client.event.DragController;
import com.gwtmobile.ui.client.event.DragEvent;
import com.gwtmobile.ui.client.event.DragEventsHandler;
import com.gwtmobile.ui.client.event.SwipeEvent;
import com.gwtmobile.ui.client.event.SwipeEventsHandler;

public class ScrollPanel extends WidgetBase 
implements HasWidgets, DragEventsHandler, SwipeEventsHandler {

	protected SimplePanel _panel = new SimplePanel();
	
    public ScrollPanel() {
    	initWidget(_panel);
        setStyleName("Scroller");
    }
    
    @Override
	public void onLoad() {
        DragController.get().addDragEventsHandler(this);
        DragController.get().addSwipeEventHandler(this);
	}
	
	@Override
	public void onUnload() {
        DragController.get().removeDragEventsHandler(this);
        DragController.get().removeSwipeEventHandler(this);
	}
	
	@Override
    public Widget getWidget() {
    	return _panel.getWidget();
    }
    
	public void reset() {
        setTransitionDuration(getWidget().getElement(), 0);
        setTranslateY(getWidget().getElement(), 0);
	}
	
	public void setPostionToTop() {
        setTransitionDuration(getWidget().getElement(), 0);
	    setTranslateY(getWidget().getElement(), 0);
	}
	
	public void setPositionToBottom() {
        setTransitionDuration(getWidget().getElement(), 0);
        setTranslateY(getWidget().getElement(), 
                this.getElement().getClientHeight() - this.getElement().getScrollHeight());
	}
	
	private native void setTranslateY(Element ele, double value) /*-{
		ele.style.webkitTransform = "translate3d(0px, " + value + "px ,0px)";
	}-*/;

	private native int getTranslateY(Element ele) /*-{
        var transform = ele.style.webkitTransform;
        var translateY = 0;        
        if (transform && transform !== "") {
            translateY = parseInt((/translate3d\(0px, (\-?.*)px, 0px\)/).exec(transform)[1]);
        }        
        return translateY;
    }-*/;

	private native int getMatrixY(Element ele) /*-{
		var matrix = new WebKitCSSMatrix(window.getComputedStyle(ele).webkitTransform);
		return matrix.f;
    }-*/;
	
	private native void setTransitionDuration(Element ele, double value) /*-{
		ele.style.webkitTransitionDuration = "" + value + "ms";
	}-*/;


	@Override
    public void onDragStart(DragEvent e) {
		Element element = getWidget().getElement();
		int matrix = getMatrixY(element);
		int current = getTranslateY(element);
		setTransitionDuration(element, 0);
		if (current != matrix) {  //scroll on going
			int diff = current - matrix;
			int offset = diff > 2 ? 2 : diff > -2 ? diff : -2;
			setTranslateY(element, matrix + offset);
			DragController.get().suppressNextClick();
		}
	}

	@Override
    public void onDragMove(DragEvent e) {
		Element widgetEle = getWidget().getElement();
		int current = getTranslateY(widgetEle);
		if (current > 0) {//exceed top boundary
			if (e.OffsetY > 0) { 	//resist scroll down.
				current += e.OffsetY / 2;
			}
			else {				
				current += e.OffsetY * 2;				
			}
		}
		else if (-current + this.getElement().getClientHeight() > widgetEle.getScrollHeight()) { //exceed bottom boundary
			if (e.OffsetY < 0) { 	//resist scroll up.
				current += e.OffsetY / 2;
			}
			else {				
				current += e.OffsetY * 2;				
			}
		}
		else {
			current += e.OffsetY;
		}
		setTranslateY(widgetEle, current);		
	}

	@Override
    public void onDragEnd(DragEvent e) {
		Element widgetEle = getWidget().getElement();
		int current = getTranslateY(widgetEle);
		if (current == 0) {
			return;
		}
		if (current > 0 //exceed top boundary
				|| getElement().getClientHeight() > widgetEle.getScrollHeight()) {
			setTransitionDuration(widgetEle, 500);
			setTranslateY(widgetEle, 0);
		}
		else if (-current + this.getElement().getClientHeight() > widgetEle.getScrollHeight()) { //exceed bottom boundary
			setTransitionDuration(widgetEle, 500);
			setTranslateY(widgetEle, this.getElement().getClientHeight() - widgetEle.getScrollHeight());
		}
	}

	@Override
    public void onSwipeVertical(SwipeEvent e) {
		Element widgetEle = getWidget().getElement();
		long current = getTranslateY(widgetEle);
		if ((current >= 0) // exceed top boundary
			|| (-current + this.getElement().getClientHeight() >= widgetEle.getScrollHeight())) { // exceed bottom boundary
			return;
		}
		
		double speed = e.getSpeed() * 2000;
		double dicstanceFactor = 0.5;
		double timeFactor = 2;
		long distance = (long) (speed / Math.abs(speed)) * Math.round(speed * speed * dicstanceFactor / 1000);
		long time =  Math.round(speed * speed * timeFactor / 2000);
		current += distance;
		if (current > 0) {//exceed top boundary
			double timeAdj = 1 - (double)current / distance;
			time = (long) (time * timeAdj * timeAdj);
			current = 0;
		}
		else if (-current + this.getElement().getClientHeight() > widgetEle.getScrollHeight()) { //exceed bottom boundary
			long bottom = this.getElement().getClientHeight() - widgetEle.getScrollHeight();
			double timeAdj = 1 - (double)(current - bottom) / distance;
			time = (long) (time * timeAdj * timeAdj);
			current = bottom;
		}
		setTransitionDuration(widgetEle, time);
		setTranslateY(widgetEle, current);
	}

    @Override
    public void onSwipeHorizontal(SwipeEvent e) {
    }

	@Override
	public void add(Widget w) {
		assert _panel.getWidget() == null : "Can only add one widget to Scroller.";
		_panel.setWidget(w);
	}

	@Override
	public void clear() {
		_panel.clear();
	}

	@Override
	public Iterator<Widget> iterator() {
		return _panel.iterator();
	}

	@Override
	public boolean remove(Widget w) {
		return _panel.remove(w);
	}
	
}
