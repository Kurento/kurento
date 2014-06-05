const PARTICIPANT_MAIN_CLASS = 'participant main';
const PARTICIPANT_CLASS = 'participant';

/**
 * Creates a video element for a new participant
 * 
 * @param {String} name - the name of the new participant, to be used as tag 
 *                        name of the video element. 
 *                        The tag of the new element will be 'video<name>'
 * @return
 */
function Participant(name) {
	this.name = name;
	var container = document.createElement('div');
	container.className = isPresentMainParticipant() ? PARTICIPANT_CLASS : PARTICIPANT_MAIN_CLASS;
	container.id = name;
	var span = document.createElement('span');
	var video = document.createElement('video');
	var rtcPeer;

	container.appendChild(video);
	container.appendChild(span);
	container.onclick = switchContainerClass;
	document.getElementById('room').appendChild(container);
	
	span.appendChild(document.createTextNode(name));

	video.id = 'video-' + name;
	video.autoplay = true;
	video.controls = false;


	this.getElement = function() {
		return container;
	}

	this.getVideoElement = function() {
		return video;
	}

	function switchContainerClass() {
		if (container.className === PARTICIPANT_CLASS) {
			var elements = Array.prototype.slice.call(document.getElementsByClassName(PARTICIPANT_MAIN_CLASS));
			elements.forEach(function(item) {
					item.className = PARTICIPANT_CLASS;
				});
	
				container.className = PARTICIPANT_MAIN_CLASS;
			} else {
			container.className = PARTICIPANT_CLASS;
		}
	}
	
	function isPresentMainParticipant() {
		return ((document.getElementsByClassName(PARTICIPANT_MAIN_CLASS)).length != 0);
	}
	
	this.offerToReceiveVideo = function(offerSdp, wp){
		console.log('Invoking SDP offer callback function');
		client.sendRequest("receiveVideoFrom", {
			sender : name,
			sdpOffer : offerSdp
		}, function(error, result) {
			if (error) return console.error(error);
			wp.processSdpAnswer(result.sdpAnswer);
		});
	}

	Object.defineProperty(this, 'rtcPeer', { writable: true});
	
	this.dispose = function() {
		console.log('Disposing participant ' + this.name);
		this.rtcPeer.dispose();
		container.parentNode.removeChild(container);
	};
}
