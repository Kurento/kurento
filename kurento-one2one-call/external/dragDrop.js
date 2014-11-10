dragDrop = {
	keyHTML : '',
	keySpeed : 10, // pixels per keypress event
	initialMouseX : undefined,
	initialMouseY : undefined,
	startX : undefined,
	startY : undefined,
	dXKeys : undefined,
	dYKeys : undefined,
	draggedObject : undefined,
	initElement : function(element) {
		if (typeof element == 'string')
			element = document.getElementById(element);
		if (element) {
			element.onmousedown = dragDrop.startDragMouse;
			element.innerHTML += dragDrop.keyHTML;
			var links = element.getElementsByTagName('a');
			var lastLink = links[links.length - 1];
			if (lastLink) {
				lastLink.relatedElement = element;
				lastLink.onclick = dragDrop.startDragKeys;
			}
		}
	},
	startDragMouse : function(e) {
		dragDrop.startDrag(this);
		var evt = e || window.event;
		dragDrop.initialMouseX = evt.clientX;
		dragDrop.initialMouseY = evt.clientY;
		addEventSimple(document, 'mousemove', dragDrop.dragMouse);
		addEventSimple(document, 'mouseup', dragDrop.releaseElement);
		return false;
	},
	startDragKeys : function() {
		dragDrop.startDrag(this.relatedElement);
		dragDrop.dXKeys = dragDrop.dYKeys = 0;
		addEventSimple(document, 'keydown', dragDrop.dragKeys);
		addEventSimple(document, 'keypress', dragDrop.switchKeyEvents);
		this.blur();
		return false;
	},
	startDrag : function(obj) {
		if (dragDrop.draggedObject)
			dragDrop.releaseElement();
		dragDrop.startX = obj.offsetLeft;
		dragDrop.startY = obj.offsetTop;
		dragDrop.draggedObject = obj;
		obj.className += ' dragged';
	},
	dragMouse : function(e) {
		var evt = e || window.event;
		var dX = evt.clientX - dragDrop.initialMouseX;
		var dY = evt.clientY - dragDrop.initialMouseY;
		dragDrop.setPosition(dX, dY);
		return false;
	},
	dragKeys : function(e) {
		var evt = e || window.event;
		var key = evt.keyCode;
		switch (key) {
		case 37: // left
		case 63234:
			dragDrop.dXKeys -= dragDrop.keySpeed;
			break;
		case 38: // up
		case 63232:
			dragDrop.dYKeys -= dragDrop.keySpeed;
			break;
		case 39: // right
		case 63235:
			dragDrop.dXKeys += dragDrop.keySpeed;
			break;
		case 40: // down
		case 63233:
			dragDrop.dYKeys += dragDrop.keySpeed;
			break;
		case 13: // enter
		case 27: // escape
			dragDrop.releaseElement();
			return false;
		default:
			return true;
		}
		dragDrop.setPosition(dragDrop.dXKeys, dragDrop.dYKeys);
		if (evt.preventDefault) // also solves problem in Saf; keypress part of
			// default
			evt.preventDefault();
		return false;
	},
	setPosition : function(dx, dy) {
		dragDrop.draggedObject.style.left = dragDrop.startX + dx + 'px';
		dragDrop.draggedObject.style.top = dragDrop.startY + dy + 'px';
	},
	switchKeyEvents : function() {

		// for Opera and Safari 1.3

		removeEventSimple(document, 'keydown', dragDrop.dragKeys);
		removeEventSimple(document, 'keypress', dragDrop.switchKeyEvents);
		addEventSimple(document, 'keypress', dragDrop.dragKeys);
	},
	releaseElement : function() {
		removeEventSimple(document, 'mousemove', dragDrop.dragMouse);
		removeEventSimple(document, 'mouseup', dragDrop.releaseElement);
		removeEventSimple(document, 'keypress', dragDrop.dragKeys);
		removeEventSimple(document, 'keypress', dragDrop.switchKeyEvents);
		removeEventSimple(document, 'keydown', dragDrop.dragKeys);
		dragDrop.draggedObject.className = dragDrop.draggedObject.className
				.replace(/dragged/, '');
		dragDrop.draggedObject = null;
	}
};

function addEventSimple(obj, evt, fn) {
	if (obj.addEventListener)
		obj.addEventListener(evt, fn, false);
	else if (obj.attachEvent)
		obj.attachEvent('on' + evt, fn);
}

function removeEventSimple(obj, evt, fn) {
	if (obj.removeEventListener)
		obj.removeEventListener(evt, fn, false);
	else if (obj.detachEvent)
		obj.detachEvent('on' + evt, fn);
}
