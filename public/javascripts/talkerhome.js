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
					'<p><span class="blacktext14">'+
					'<a href="/'+update[3]+'">'+topic+'</a>'+
					'<br />'+
					'<span class="blacktext12">1 members talking</span> | '+ 
				    '<span class="blacktext12">Started 0 mins ago</span> | '+
				    '<a href="#" class="bluetext12 followTopicLink" rel="'+update[2]+'">Unfollow</a>'+
				    '</span></p>';
			
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

