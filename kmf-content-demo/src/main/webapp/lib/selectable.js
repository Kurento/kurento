/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
var Command = {
	GET_PARTICIPANTS : "getParticipants",
	SELECT : "selectParticipant",
	CONNECT : "connectParticipant",
};

var Event = {
	ON_JOINED : "onJoined",
	ON_UNJOINED : "onUnjoined",
};

function Participant() {
	this.id = null;
	this.name = null;
}

function addParticipant(participant, selectName) {
	var participants = document.getElementById(selectName);
	var opt = document.createElement('option');
	opt.innerHTML = participant.name;
	opt.value = participant.id;
	participants.appendChild(opt);
}

function removeParticipant(participant, selectName) {
	var participants = document.getElementById(selectName);
	for (var i = 0; i < participants.length; i++) {
		if (participants.options[i].value == participant.id) {
			participants.remove(i);
			break;
		}
	}
}

function removeAllParticipants(selectName) {
	var participants = document.getElementById(selectName);
	while (participants.options.length > 0) {
		participants.remove(0);
	}
}

/* Events */
function onJoined(participant) {
	console.info(participant.name + " has joined");
	getParticipants();
}

function addButton(participant) {
	var element = document.createElement("a");
	var icon = document.createElement("span");
	var name = participant.name;
	var classIcon = "glyphicon glyphicon-user";
	var classElement = "btn btn-warning";
	if (name.toLowerCase().startsWith("android")) {
		classIcon = "glyphicon glyphicon-phone";
		classElement = "btn btn-yellow";
	}

	icon.setAttribute('class', classIcon);
	element.setAttribute('class', classElement);
	element.appendChild(icon);
	element.appendChild(document.createTextNode(" " + name));
	element.setAttribute('id', participant.id);
	element.setAttribute('title', name);
	element.setAttribute('href', '#');
	element.setAttribute('onClick', "selectParticipant(this.id, this.title);");

	var parent = document.getElementById('participantButtons');
	parent.appendChild(element);
	parent.appendChild(document.createTextNode(" "));
}

function removeAllButtons() {
	var parent = document.getElementById('participantButtons');
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
}

function onUnjoined(participant) {
	console.info(participant.name + " has gone");
	removeParticipant(participant, 'orig');
	removeParticipant(participant, 'dest');

	var e = document.getElementById(participant.id);
	e.parentElement.removeChild(e);

	if (participant.id == connected) {
		connected = null;
	}
}

/* Commands */
function getParticipants() {
	conn.execute(Command.GET_PARTICIPANTS, "", function(error, result) {
		removeAllParticipants('orig');
		removeAllParticipants('dest');

		// Remove all buttons and add new ones
		removeAllButtons();
		participantsList = JSON.parse(result);
		participantsList.forEach(function(item) {
			addParticipant(item, 'orig');
			addParticipant(item, 'dest');
			addButton(item);
		});
	});
}

function selectParticipant(partId, partName) {
	connected = partId;
	conn.execute(Command.SELECT, partId, function(error, result) {
		if (error) {
			console.error("Error " + error);
		} else {
			console.info("Connecting with " + partName);
		}
	});
}

function connectParticipants() {
	var origId = document.getElementById("orig").value;
	var destId = document.getElementById("dest").value;
	var list = [ origId, destId ];
	conn.execute(Command.CONNECT, JSON.stringify(list),
			function(error, result) {
				if (error) {
					console.error("Error " + error.message);
				}
			});
}

var conn = null;
var connected = null;

function terminate() {
	if (conn == null) {
		console.warn("Connection is not established");
		return false;
	}

	document.getElementById("terminate").disabled = true;

	conn.terminate();
	conn = null;
	console.info("Connection terminated by user");
	document.getElementById("start").disabled = false;
	return true;
}

function initConnection(conn) {
	document.getElementById("terminate").disabled = false;

	conn.on("start", function(event) {
		document.getElementById("terminate").disabled = false;
	});

	conn.on("mediaevent", function(event) {
		if (Event.ON_JOINED == event.type) {
			var part = JSON.parse(event.data);
			onJoined(part);
		} else if (Event.ON_UNJOINED == event.type) {
			var part = JSON.parse(event.data);
			onUnjoined(part);
		}
	});

	conn.on("error", function(error) {
		console.error("Error: " + error.message);
		terminate();
	});

	conn.on("terminate", function() {
		switchVideoPosters(false);
	});
}

function switchVideoPosters(showSpinner) {
	var localVideo = document.getElementById("localVideo");
	var remoteVideo = document.getElementById("remoteVideo");
	var spiner = showSpinner ? "center transparent url('../img/spinner.gif') no-repeat"
			: "";
	var transparentPixel = showSpinner ? "../img/transparent-1px.png"
			: "../img/logo/webrtc.png";
	localVideo.style.background = spiner;
	remoteVideo.style.background = spiner;
	localVideo.poster = transparentPixel;
	remoteVideo.poster = transparentPixel;
}

function start(servlet) {
	var name = document.getElementById("name").value;
	if (!name) {
		bootbox.alert("You must specify your name", function() {
			document.getElementById("name").focus();
		});
		return;
	}

	document.getElementById("start").disabled = true;
	switchVideoPosters(true);

	var options = {
		iceServers : [],
		localVideoTag : "localVideo",
		remoteVideoTag : "remoteVideo"
	};

	var endpoint = "../" + servlet + "/" + name;
	try {
		conn = new kwsContentApi.KwsWebRtcContent(endpoint, options);
		console.info("Creating connection to handler located at " + endpoint);
		initConnection(conn);
	} catch (error) {
		document.getElementById("start").disabled = false;
		console.error(error.message);
	}
}

// The focus on the name text box at the beginning
window.onload = function() {
	console = new Console("console", console);
	document.getElementById("name").focus();
	switchVideoPosters(false);
	dragDrop.initElement("videoSmall");
};

// Ensure the connection is terminated in the end
window.onbeforeunload = function() {
	var alertUser = terminate();
	if (alertUser) {
		return "Please click on \'Terminate\' button before leaving this page.";
	} else {
		window.onbeforeunload = undefined;
	}
};

if (typeof String.prototype.startsWith != 'function') {
	// see below for better implementation!
	String.prototype.startsWith = function(str) {
		return this.indexOf(str) == 0;
	};
}

$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
