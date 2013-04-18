package com.lorepo.icf.uidesigner;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.lorepo.icf.uidesigner.IDesignerModel.IListener;

/**
 * Klasa bazowa dla paneli, które chcą aby ich dzieci mogly byc
 * przesuwane i mogly miec zmieniany rozmiar
 */
@SuppressWarnings("unchecked")
public class UIDesigner<T> extends Composite {

	private AbsolutePanel 	innerPanel;
	private IDesignerModel<T>	model = new DesignerModel<T>();
	private IWidgetFactory<T>	widgetFactory = new WidgetFactory<T>();
	private boolean		isMoving;
	private int			mouseLastX;
	private int			mouseLastY;
	private MultiSelectionModel<T> selectionModel = new MultiSelectionModel<T>();
	private Widget		selectionBox = null;
	private SelectionWidget<T>	selectionWidget = new SelectionWidget<T>();


	public UIDesigner(){

		innerPanel = new AbsolutePanel();
		initWidget(innerPanel);
	    DOM.sinkEvents(this.getElement(), Event.MOUSEEVENTS | Event.ONDBLCLICK);
	}
	
	public void setSelectionModel(MultiSelectionModel<T> selectionModel){
		
		this.selectionModel = selectionModel;
		selectionModel.addSelectionChangeHandler(new Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {
				updateSelection();
			}
		});
	}

	
	private void updateSelection() {

		selectionWidget.clear();
		for(int i = 0; i < innerPanel.getWidgetCount(); i++){
			
			if(innerPanel.getWidget(i) instanceof DesignerWidget<?>){
				Widget widget = innerPanel.getWidget(i);
				T item = getWidgetModel(widget);
				if(item != null && selectionModel.isSelected(item)){
					selectionWidget.addToSelection((DesignerWidget<T>) widget);
				}
			}
		}
		
		refreshSelectionWidgetPosition();
	}
	
	
	private void refreshSelectionWidgetPosition() {

		innerPanel.remove(selectionWidget);
		if(selectionWidget.isVisible()){
			innerPanel.add(selectionWidget, selectionWidget.getLeft(), selectionWidget.getTop());
		}
	}


	private T getWidgetModel(Widget widget){
		
		if(widget instanceof DesignerWidget<?>){
			DesignerWidget<T> designerWidget = (DesignerWidget<T>) widget;
			return designerWidget.getModel();
		}
		
		return null;
	}
	
	
	public void setModel(IDesignerModel<T> model){
		
		this.model = model;
		model.addListener(new IListener<T>() {

			@Override
			public void onItemChanged(T item) {
				itemModified(item);
			}

			@Override
			public void onItemAdded(T item) {
				addItem(item);
			}

			@Override
			public void onItemRemoved(T item) {
				removeItem(item);
			}
		});
	}

	
	protected void itemModified(T item) {
		
		if(!isMoving){
			refresh();
		}
	}


	protected void addItem(T item) {
	
		DesignerWidget<?> proxy = widgetFactory.getWidget(item);
		proxy.setPixelSize(proxy.getWidth(), proxy.getHeight());
		innerPanel.add(proxy, proxy.getLeft(), proxy.getTop());
	}


	protected void removeItem(T item) {
		
		selectionWidget.clear();
		Widget widget = findWidget(item);
		if(widget != null){
			innerPanel.remove(widget);
		}
	}


	public void setWidgetFactory(IWidgetFactory<T> factory){
		this.widgetFactory = factory;
	}

	
	/**
	 * Listen to mouse events
	 */
	@Override
	public void onBrowserEvent(Event event) {

		final int eventType = DOM.eventGetType(event);
		event.preventDefault();
		event.stopPropagation();

		if (Event.ONMOUSEDOWN == eventType) {

			mouseDown(event);

		} else if (Event.ONMOUSEMOVE == eventType) {

			mouseMove(event);
			
		} else if (Event.ONMOUSEUP == eventType) {

			mouseUp();
		}
		else if(Event.ONDBLCLICK == eventType){
			
			mouseDblClick(event);
		}
		
	}


	private void mouseDown(Event event) {
		
		mouseLastX = event.getClientX();
		mouseLastY = event.getClientY();
		if(selectionWidget.startMoving(mouseLastX, mouseLastY)){
			isMoving = true;
		}
		else if(!selectModuleAt(event)){
			createSelectionBox(mouseLastX, mouseLastY);
		}
		
		DOM.setCapture(this.getElement());
	}


	/**
	 * @return true if module was selected
	 */
	private boolean selectModuleAt(Event event) {
		
		DesignerWidget<T> foundChild = null;
		
		foundChild = findWidgetAtPos(event.getClientX(), event.getClientY());
		
		if(foundChild != null){
		
			if(event.getShiftKey()){
				T item = getWidgetModel(foundChild);
				selectionModel.setSelected(item, !selectionModel.isSelected(item));
			}
			else{
				selectItem(foundChild);
			}
			
			return true;
		}
		
		return false;
	}


	private DesignerWidget<T> findWidgetAtPos(int clientX, int clientY) {
		
		for(int i=innerPanel.getWidgetCount()-1 ; i >= 0 ; i--){
			
			if(innerPanel.getWidget(i) instanceof DesignerWidget<?>){
				
				DesignerWidget<T> child = (DesignerWidget<T>) innerPanel.getWidget(i);
				if(	clientX > child.getAbsoluteLeft() && 
					clientX < child.getAbsoluteLeft() + child.getOffsetWidth())
				{
					if(	clientY > child.getAbsoluteTop() && 
						clientY < child.getAbsoluteTop() + child.getOffsetHeight())
					{
						return child;
					}
				}
			}
		}

		return null;
	}


	private void mouseMove(Event event) {
		
		int dx = event.getClientX()-mouseLastX;
		int dy = event.getClientY()-mouseLastY;

		if(selectionBox != null){
			resizeSelectionBox(event.getClientX(), event.getClientY());
		}else if(isMoving){
			selectionWidget.onMouseMove(dx, dy);
		}

		mouseLastX = event.getClientX();
		mouseLastY = event.getClientY();
		setCursor(event);
	}


	private void mouseUp() {
		
		if(selectionBox != null){
			makeSelectionFromBox();
		}
		else if (isMoving){
			selectionWidget.stopMoving();
			refresh();
		}
		
		isMoving = false;
		DOM.releaseCapture(this.getElement());
	}


	private DesignerWidget<T> findWidget(T item) {

		for(int i = 0; i < innerPanel.getWidgetCount(); i++){
			
			if(innerPanel.getWidget(i) instanceof DesignerWidget<?>){
				DesignerWidget<T> widget = (DesignerWidget<T>) innerPanel.getWidget(i);
				if(widget.getModel() == item){
					return widget;
				}
			}
		}
		
		return null;
	}


	private void mouseDblClick(Event event) {
	}


	private void selectItem(Widget widget) {
		
		if(widget instanceof DesignerWidget<?>){
			T item = ((DesignerWidget<T>) widget).getModel();
			if( !selectionModel.isSelected(item) ){
				selectionModel.clear();
				selectionModel.setSelected(item, true);
			}
		}
	}


	/**
	 * Sprawdzenie czy dwa widget na siebie zachodzą
	 * @param selectionBox2
	 * @param child
	 * @return
	 */
	private boolean collide(Widget widget1, Widget widget2) {

		int left1 = widget1.getAbsoluteLeft();
		int top1 = widget1.getAbsoluteTop();
		int right1 = left1 + widget1.getOffsetWidth();
		int bottom1 = top1 + widget1.getOffsetHeight();
		
		int left2 = widget2.getAbsoluteLeft();
		int top2 = widget2.getAbsoluteTop();
		int right2 = left2 + widget2.getOffsetWidth();
		int bottom2 = top2 + widget2.getOffsetHeight();
		
		if((left2 > left1 && left2 < right1) || (right2 > left1 && right2 < right1)){
		
			if((top2 > top1 && top2 < bottom1) || (bottom2 > top1 && bottom2 < bottom1)){
				return true;
			}
		}
		
		return false;
	}


	/**
	 * Utworzenie widgetu do zaznaczania obiektów
	 */
	private void createSelectionBox(int clientX, int clientY) {

		int x = clientX-getAbsoluteLeft();
		int y = clientY-getAbsoluteTop();
		selectionBox = new HTML();
		selectionBox.setStyleName("ice_selectionBox");
		selectionBox.setPixelSize(0, 0);
		innerPanel.add(selectionBox, x, y);
	}


	/**
	 * Zmiana rozmiaru selection box-a
	 * @param event
	 */
	private void resizeSelectionBox(int clientX, int clientY) {

		int x = clientX-getAbsoluteLeft();
		int y = clientY-getAbsoluteTop();

		int width = x-innerPanel.getWidgetLeft(selectionBox);
		int height = y-innerPanel.getWidgetTop(selectionBox);
		selectionBox.setPixelSize(width, height);
		
	}


	/**
	 * Zaznaczac  obiekty nachodzące na selection box
	 */
	private void makeSelectionFromBox() {

		selectionModel.clear();

		if(selectionBox != null){
		
			for(int i=0 ; i < innerPanel.getWidgetCount() ; i++){
				Widget child = innerPanel.getWidget(i);
				if(child == selectionBox){
					continue;
				}
				
				if(collide(selectionBox, child)){
					T item = getWidgetModel(child);
					if(item != null){
						selectionModel.setSelected(item, true);
					}
				}
			}
			
			innerPanel.remove(selectionBox);
			selectionBox = null;
		}
	}


	/**
	 * Set cursor based on mouse position
	 * @param event
	 */
	private void setCursor(Event event) {
		
		String cursorType = selectionWidget.getCursorType(event.getClientX(), event.getClientY());
		
		if(cursorType == null){
			cursorType = "default";
		}
		
		DOM.setStyleAttribute(this.getElement(),"cursor", cursorType);
	}


	public void refresh() {
		
		innerPanel.clear();
		
		for(int i = 0; i < model.getItemsCount(); i++){
		
			T item = model.getItem(i);
			DesignerWidget<?> proxy = widgetFactory.getWidget(item);
				
			proxy.setPixelSize(proxy.getWidth(), proxy.getHeight());
			innerPanel.add(proxy, proxy.getLeft(), proxy.getTop());
		}

		updateSelection();
	}

	
	public void setGridSize(int gridSize) {
		selectionWidget.setGridSize(gridSize);  
	}
}
