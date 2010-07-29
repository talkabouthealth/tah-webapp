function createRequestObject() {
	var ro;
 	var browser = navigator.appName;
 	if(browser == "Microsoft Internet Explorer"){
    	ro = new ActiveXObject("Microsoft.XMLHTTP");
 	}else{
    	ro = new XMLHttpRequest();
 	}
 	return ro;
}

var httpRequest = createRequestObject();

function moreOldTopics() {
	//alert("More Old");
	httpRequest.open("GET", "/tah-java/MoreOldTopicsServlet", true);
	httpRequest.onreadystatechange = moreOldTopicsResponse;
	httpRequest.send(null);
}

function moreOldTopicsResponse() {
	if(httpRequest.readyState == 4) {
		if(httpRequest.status == 200) { 
			var xmlDoc = httpRequest.responseXML.documentElement;
			// while topics, print each to topic list
			var x = xmlDoc.getElementsByTagName("topic");
			var i = 0;
			insertOldTopics(i, x.length, x);
		}
	}
}
function insertOldTopics(i, l, x) {
	var tid = x[i].firstChild.firstChild.data;
	var t = x[i].lastChild.firstChild.data;
	
	// create element for new topic and add to document
	var topic = t.replace(/&#39;/g, "\'");
	
	var mc_TopicTxtNode = document.createTextNode(topic);
	
	var mc_box = document.createElement("div");
    mc_box.className = 'box';
    mc_box.appendChild(mc_TopicTxtNode);
	
	var mc_aAddLink = document.createElement("a");
	mc_aAddLink.setAttribute("href", "javascript:addTopicToQueue('" + tid + "')");
	mc_aAddLink.appendChild(mc_box);

	var mc_d = document.createElement("div");
    mc_d.id = tid;
    mc_d.className = 'odd';
    mc_d.appendChild(mc_aAddLink);

    var pelement = document.createElement("p");
    
	var tts2 = document.getElementById("topicstable");
	var fte = tts2.lastChild;
	//alert("Before 1st insert");
	tts2.insertBefore(mc_d, fte);
	//alert("Before 2nd insert");
	tts2.insertBefore(pelement, mc_d);
	
	i++;
	if (i > l){return;}
	setTimeout(function(){ insertOldTopics(i, l, x);}, 0);
}

function moreNewTopics() {
	httpRequest.open("GET", "/tah-java/MoreNewTopicsServlet", true);
	httpRequest.onreadystatechange = moreNewTopicsResponse;
	httpRequest.send(null);
}
function moreNewTopicsResponse() {
	if(httpRequest.readyState == 4) {
		if(httpRequest.status == 200) { 
			var xmlDoc = httpRequest.responseXML.documentElement;
    		// while topics, print each to topic list
			var x = xmlDoc.getElementsByTagName("topic");
			if(x.length != 0){
				var i = 0;
				insertNewTopics(i, x.length, x);
			}
		}
	}
}
function insertNewTopics(i, l, x) {
	var tid = x[i].firstChild.firstChild.data;
	var t = x[i].lastChild.firstChild.data;
	
	// create element for new topic and add to document
	var topic = t.replace(/&#39;/g, "\'");
	
	var mc_TopicTxtNode = document.createTextNode(topic);
	
	var mc_box = document.createElement("div");
    mc_box.className = 'box';
    mc_box.appendChild(mc_TopicTxtNode);
	
	var mc_aAddLink = document.createElement("a");
	mc_aAddLink.setAttribute("href", "javascript:addTopicToQueue('" + tid + "')");
	mc_aAddLink.appendChild(mc_box);

	var mc_d = document.createElement("div");
    mc_d.id = tid;
    mc_d.className = 'odd';
    mc_d.appendChild(mc_aAddLink);

    var pelement = document.createElement("p");
    
	var tts2 = document.getElementById("topicstable");
	var fte = tts2.firstChild;
	loop = 'y';
	count = 0;
	while (loop == 'y' && count < tts2.childNodes.length) {
		if (fte.nodeType == '1') {
			if (fte.nodeName == 'P' || fte.nodeName == 'DIV') {
				tts2.insertBefore(mc_d, fte);
				tts2.insertBefore(pelement, mc_d);
				loop = 'n';
			} 
		}
		if (loop == 'y') {
			fte = fte.nextSibling;
			count++;
		}
	}
	i++;
	if (i >= l){return;}
	setTimeout(function(){ insertNewTopics(i, l, x);}, 500);
}
function postNewTopic(form) {
	//alert("Form.value: " + form.value);
	
	var topic = encodeURIComponent(form.value);
	//alert(topic);
	var params = form.name + "=" + topic;
	form.value = '';
	httpRequest.open("POST", "/topics/create", true);
	httpRequest.setRequestHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
	httpRequest.setRequestHeader("Content-length", params.length);
	httpRequest.setRequestHeader("Connection", "close");
	httpRequest.onreadystatechange = postNewTopicResponse;
	httpRequest.send(params);
}
function postNewTopicResponse() {
	if(httpRequest.readyState == 4) {
		if(httpRequest.status == 200) { 
			//alert("Response");
			var response = httpRequest.responseText;
			var update = new Array();
			update = response.split('|');
			
			if (update[1] != '' && update[0] != '') {
				//show chat window
				openChat(update[0]);
				
				var topicLink = update[1].replace(/&#39;/g, "\\'");
				var topic = update[1].replace(/&#39;/g, "'");
				topic = topic.replace(/&#124;/g, "|");
			    
				//alert("Topic: " + topic);
				//alert("TopicLink: " + topicLink);
				
				// add topic to main topic list 
				var newTopic = createDiv('area');
				
				//left side - author information
				var areaLeft = createDiv('arealeft2');
				areaLeft.innerHTML = 
					//TODO: handle user data in other way?
					'<a href="/'+username+'" class="bluetext11">'+
					'<img src="/image/'+username+'" width="71" height="71" border="0" />'+
					'<br /><span class="bluetext11">'+username+'</span>'+
					'</a><br/>'+
					'<span class="currenttext">'+levelOfRecognition+'</span>';
				newTopic.appendChild(areaLeft);
				
				//right side
				var areaRight = createDiv('arearight');
				newTopic.appendChild(areaRight);
				
				areaRight.appendChild(createDiv('areatop'));
				var areaMid = createDiv('areamid');
				areaRight.appendChild(areaMid); 
				areaRight.appendChild(createDiv('areabot'));
				
				//fill aremid with data
				areaMidLeft = createDiv('areamidleft');
				areaMid.appendChild(areaMidLeft);
				areaMidLeft.innerHTML = 
					'<p><span class="blacktext14">'+topic+'<br />'+
					'<span class="bluetext12">1 people talking</span> | '+ 
				    '<span class="red12">Started 0 mins ago</span><br /></span>'+
				    '<span class="blacktext">Community: Breast Cancer</span></p>';
			
				joinArea = createDiv('join');
				areaMid.appendChild(joinArea);
				joinArea.innerHTML = 
					'<a href="#" onclick="openChat(\''+update[0]+'\')">'+
					'<img border="0" src="/public/images/join_conv.gif" width="178" height="27" />'+ 
				    '</a>';    
				
				//add topic as first topic in list
				var topicList = document.getElementById("innermid");
				var topicListFirstChild = topicList.firstChild;
                topicList.insertBefore(newTopic, topicListFirstChild);				
			}
		}
	}
}

function createDiv(className) {
	var newDiv = document.createElement("div");
	newDiv.className = className;
	return newDiv;
}

