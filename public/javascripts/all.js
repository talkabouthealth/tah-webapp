/*
 * Common JS for all pages.
 */

//used in 'thankYou' or 'comment' popups
var selectedTalkerId = "";
//used in 'flag' popups
var flagType = "";
var flagId = "";

$(document).ready(function() {

	$(".followConvoLink").live('click', function() {
		var convoId = $(this).attr("rel");
		if ($(this).html().indexOf("Unfollow") != -1) {
			$(this).html("Follow");
		}
		else {
			$(this).html("Unfollow");
		}
		$.post("/conversations/follow", 
				{ convoId: convoId },
				function(data) {
					//...
				}
			);
		return false;
	});
});

/* ---------------- OAuth and other popup windows ----------------- */
function openChat(convoId){
	window.open("/chat/"+convoId, "Chat", "resizable=1, scrollbars=false, width=810,height=610");
	return false;
}
function openInvitationsWindow() {
	window.open("/home/invitations", "TalkAboutHealthInvitations", "width=600,height=350");
	return false;
}
function openTwitter(redirectURL) {
	if (!redirectURL) {
		//prevents 'undefined' text
		redirectURL = "";
	}
	var popupWindow = window.open("/oauth/getauth?type=twitter&redirectURL="+redirectURL, 
		"TwitterLogin", "width=800,height=470,toolbar=no,location=no,menubar=no");
	
	return false;
}
function openFacebook(redirectURL) {
	if (!redirectURL) {
		//prevents 'undefined' text
		redirectURL = "";
	}
	var popupWindow = window.open("/oauth/getauth?type=facebook&redirectURL="+redirectURL,  
		"FacebookLogin", "width=1000,height=550,toolbar=no,location=no,menubar=no");
	return false;
}




/* ---------------- Conversations/Chats related ----------------- */
function showStartConvoDialog(type) {
	//select correct radio button
	if (type === "question") {
		$("#newConvoTypeQuestion").attr("checked", "checked");
	}
	else if (type === "chat"){
		$("#newConvoTypeConvo").attr("checked", "checked");
	}
	
	$("#newConvoForm").show();
	$("#newQuestionConfirm").hide();
	$("#newTalkConfirm").hide();
	if ($("#convoText") && $("#convoText").val() !== "") {
		$("#newConvoTitle").val($("#convoText").val());
	}
	showPopup("#startDialog", 350);
	
	return true;
}

//Save convo after entering data in StartConvo dialog
function saveConvo(page) {
	var type = "QUESTION";
	if ($("#newConvoTypeConvo").attr("checked")) {
		type = "CONVERSATION";
	}
	var title = $("#newConvoTitle").val();
	if (title === 'Enter your question or request here.') {
		title = '';
	}
	var details = $("#newConvoDetails").val();
	if (details === 'For example. Will the stage of disease, current medications, or patient history help to answer the question?') {
		details = '';
	}
	//var topics = $("#newConvoTopics").val();
	var topics = '';
	
	var questionCategory = $("#newConvoDisease").val();
	
	//if new question if follow-up - we save id of parent convo
	var parentConvoId = "";
	if (page === "conversationSummary") {
		parentConvoId = currentConvoId;
	}
	
	//check ccTwitter and ccFacebook buttons
	var ccTwitter = "";
	var ccFacebook = "";
	if (page === "home") {
		if (type === "QUESTION") {
			ccTwitter = $("#ccTwitterQuestion").attr("checked");
			ccFacebook = $("#ccFacebookQuestion").attr("checked");
		}
		else {
			ccTwitter = $("#ccTwitterChat").attr("checked");
			ccFacebook = $("#ccFacebookChat").attr("checked");
		}
	}

	if (title === "") {
		alert("Please input headline.");
		return false;
	}
	
	if(page === "profileRight") {
		parentConvoId = document.getElementById("newConvoTargetId").value;
	}
	
	$("#convoCreateImage").show();
	$.post("/conversations/create", 
				{ type: type, title: title, details: details, topics: topics, fromPage: page, questionCategory: questionCategory,
					parentConvoId: parentConvoId, ccTwitter: ccTwitter, ccFacebook: ccFacebook},
			function(data) {
				$("#convoCreateImage").hide();
				if (type === "CONVERSATION") {
					$("#startTalkBtn").click(function() {
	  					openChat(data.tid);
	  					$.post("/conversations/start}", {convoId: data.id});
	  					hideAll();
					});
					$("#startChatText").val("");
					$("#newTalkConfirm").show();
				}
				else {
					$("#questionLink").attr("href", data.url).html(data.url);
					$("#postQuestionText").val("");
					$("#newQuestionConfirm").show();
				}
				$("#newConvoForm").hide();
				
				//add new item to the feeds
				//exception: question items on LiveChats page
				if (!(type === "QUESTION" && page === "liveTalks")) {
					//alert(data.html);
					if ($(".conversationsList")) {
						$(data.html).prependTo($(".conversationsList"));
					}
					if ($("#communityFeedList")) {
						$(data.html).prependTo($("#communityFeedList"));
					}
				}

				$("#convoText").val("");
				$("#newConvoTitle").val("");
				$("#newConvoDetails").val("For example. Will the stage of disease, current medications, or patient history help to answer the question?");
				//$("#newConvoTopics").val("");
			}
		);

	return false;
}

function restartConvo(tid, convoId) {
	openChat(tid);
	$.post("/conversations/restart", {convoId: convoId});
	return false;
}

function closeLiveTalk(convoId) {
	$.post("/conversations/close", {convoId: convoId});
	
	$("#liveTalk"+convoId).remove();
	return false;
}

//flag conversation or answer
function flag() {
	var reason = $("#flagReason").val();
	$("#flagReason").val("");
	if (flagType === "convo") {
		$.post("/conversations/flag", 
				{reason: reason, convoId: flagId}
		);
	}
	else if (flagType === "answer") {
		$.post("/conversations/flaganswer", 
			{reason: reason, answerId: flagId}
		);
	}

	hideAll();
	showPopup("#flagConfirm", 200);
	return false;
}



/* ---------------- Talker related ----------------- */
function followTalker(followLink, followingId) {
	$.post("/actions/followTalker", 
		{ followingId: followingId },
		function(data) {
			$(followLink).html(data);
		}
	);
	
	return false;
}

//shows popup form for ThankYous/Comments/Flag
function showPopupForm (type, talkerId, userName) {
	$("#"+type+"User").html(userName);
	selectedTalkerId = talkerId;

	showPopup("#"+type+"Popup", 200);
	return true;
}

//void createThankYou(String toTalkerId, String note, String tagFile) {
function saveThankYou() {
	var noteText = $("#thankYouListNote").val();
	hideAll();
	$("#thankYouListNote").val("");

	$.post("/actions/createThankYou", 
		{ toTalkerId: selectedTalkerId, note: noteText}
	);
	return false;
}

function sendProfileComment() {
	var commentText = $("#commentText").val();
	hideAll();
	$("#commentText").val("");

	$.post("/actions/saveProfileComment", 
		{ profileTalkerId: selectedTalkerId, parentId: '', text: commentText}
	);
}

//saveProfileComment(String profileTalkerId, String parentId, String text)
function saveProfileComment(parentId, parentList, userName) {
	var parentListId = "";
	if (parentList) {
		parentListId = "#"+parentList+" ";
	}
	var commentText = $(parentListId+".replytext"+parentId).val();
//	alert(parentListId+".replytext"+parentId);
//	alert(commentText);
	$(parentListId+".saveThoughtImage"+parentId).show();
	$(parentListId+".replytext"+parentId).val("");
	
	//YURIY: FIX FOR URLS IN REPLIES NOT BEING TRANSLATED -- SPENT 2h TO HUNT THIS DOWN %-/
	//linkedText = linkify(commentText);
	linkedText = commentText;

	$.post("/actions/saveProfileComment", 
		{ parentId: parentId, text: linkedText,  parentList: parentList},
		function(html) {
			$("#firstcommentmessage").hide();
			$(parentListId+".saveThoughtImage"+parentId).hide();
			//put comment in the tree
			if (parentId == '') {
				//add as first element in the top
				$(html).prependTo($(".commentsarea"));

				//add inline edit for new comment
				$('.inline-edit').inlineEdit( { hover: ''} );
			}
			else {
				//add as last element in subtree
				$(parentListId+".reply"+parentId).before($(html));
			}
		}
	);
	return false;
}




/* --------------- Autocompletes ---------------- */
//URL of selected conversation in autocomplete
var selectedConvoURL;

//Uses jQuery Autocomplete plugin
function makeAutocomplete(id, type, parentTopic) {
	if ($(id).size() === 0) {
		return;
	}
	
	var url = "";
	if (type === "all") {
		url = "/search/ajaxSearch";
	}
	//'convo' - redirects user to selected convo
	//'convoedit' - just copies convo to an input
	else if (type === "convo" || type === "convoedit") {
		url = "/search/ajaxConvoSearch";
	}
	else if (type === "topic") {
		url = "/search/ajaxTopicSearch";
	}else if(type === "user") {
		url = "/search/ajaxUserSearch";
	}else if(type === "message"){
		url = "/search/ajaxMessageSearch";
	}
	
	var cache = {};
	$(id).autocomplete({
		minLength: 1,
		source: function(request, response) {
			//get list of items from cache or from Ajax request
			
			var term = ""
			if(type === "user") {
				var newTerm=extractLast( request.term )
				
				if ( newTerm in cache ) {
					response(cache[ newTerm ]);
								
				return;
				}
				request.term = newTerm;
			}else{
		
				if ( request.term in cache ) {
					response( cache[ request.term ] );
					return;
				}
				request.term = request.term;
			}
			//used when we need autocomplete of subtopics
			request.parent = parentTopic;
			$.ajax({
				url: url,
				dataType: "json",
				data: request,
				success: function( data ) {
					cache[ term ] = data;
					response( data );
				}
			});
		},
		focus: function() {
			// prevent value inserted on focus
			return false;
		},
		select: function(event, ui) {
			if (type === "all" || type === "convo") {
				var url = ui.item.url;
				if (url === "#fullsearch") {
					//go to full conversations search
					if (trim(ui.item.value) === '') {
						alert("Please enter search query");
						return false;
					}else{
						url = "/search?query="+ui.item.value;
					}
				}
				document.location = url;
			}else if (type === "message"){
				mailSearch("#searchMessage",ui.item.value);
			}
			else if (type === "user"){
				var terms = split( this.value );
				
				// remove the current input
				terms.pop();
				// add the selected item
				terms.push( ui.item.value );
				// add placeholder to get the comma-and-space at the end
				terms.push( "" );
				this.value = terms.join( ", " );
				
				$(this).change();
				$(this).focus();
				return false;
			}	
			else {
				$(id).val(ui.item.value);
				selectedConvoURL = ui.item.url;
			}			
			return false;
		}
	})
	.data( "autocomplete" )._renderItem = function( ul, item ) {
		return $( "<li></li>" )
			.data( "item.autocomplete", item )
			.append( "<a>" + item.label + "&nbsp;<span>" + item.type + "</span></a>" )
			.appendTo( ul );
	};
}

//Autocompete for topics - allows adding multiply topics (separated by space)
function makeTopicsAutocomplete(id, parent) {
	if ($(id).size() === 0) {
		return;
	}
	
	var cache = {};
	$(id).autocomplete({
		minLength: 1,
		source: function(request, response) {
			var currentTerm = extractLast( request.term );
			
			if ( currentTerm in cache ) {
				response( cache[ currentTerm ] );
				return;
			}
			
			$.ajax({
				url: "/search/ajaxTopicSearch",
				dataType: "json",
				data: { term : currentTerm, parent : parent },
				success: function( data ) {
					cache[ currentTerm ] = data;
					response( data );
				}
			});
		},
		search: function() {
			// custom minLength
			var term = extractLast( this.value );
			if ( term.length < 1 ) {
				return false;
			}
		},
		focus: function() {
			// prevent value inserted on focus
			return false;
		},
		select: function( event, ui ) {
			var terms = split( this.value );
			// remove the current input
			terms.pop();
			// add the selected item
			terms.push( ui.item.value );
			// add placeholder to get the comma-and-space at the end
			terms.push( "" );
			this.value = terms.join( ", " );
			$(this).change();
			return false;
		}
	})
	.data( "autocomplete" )._renderItem = function( ul, item ) {
		return $( "<li></li>" )
			.data( "item.autocomplete", item )
			.append( "<a>" + item.label + "&nbsp;<span>" + item.type + "</span></a>" )
			.appendTo( ul );
	};
}

//Autocompete for disease - allows adding multiple disease (separated by space)
function makeDiseaseAutocomplete(id, convoId) {
	if ($(id).size() === 0) {
		return;
	}
	
	var cache = {};
	$(id).autocomplete({
		minLength: 1,
		source: function(request, response) {
			var currentTerm = extractLast( request.term );
			
			if ( currentTerm in cache ) {
				response( cache[ currentTerm ] );
				return;
			}
			
			$.ajax({
				url: "/search/ajaxDiseaseSearch",
				dataType: "json",
				data: { term : currentTerm, convoId: convoId},
				success: function( data ) {
					cache[ currentTerm ] = data;
					response( data );
				}
			});
		},
		search: function() {
			// custom minLength
			var term = extractLast( this.value );
			if ( term.length < 1 ) {
				return false;
			}
		},
		focus: function() {
			// prevent value inserted on focus
			return false;
		},
		select: function( event, ui ) {
			var terms = split( this.value );
			// remove the current input
			terms.pop();
			// add the selected item
			terms.push( ui.item.value );
			// add placeholder to get the comma-and-space at the end
			terms.push( "" );
			this.value = terms.join( ", " );
			$(this).change();
			return false;
		}
	})
	.data( "autocomplete" )._renderItem = function( ul, item ) {
		return $( "<li></li>" )
			.data( "item.autocomplete", item )
			.append( "<a>" + item.label + "&nbsp;</a>" )
			.appendTo( ul );
	};
}

function split( val ) {
	return val.split( /,\s*/ );
}
function extractLast( term ) {
	return split( term ).pop();
}




/*------------------- Ajax forms ------------------ */
//timeout for result popup
var closeTimeout;
var options = { 
      //target:        '#output2',   // target element(s) to be updated with server response 
      //beforeSubmit:  showRequest,  // pre-submit callback 
      success:       showResponse  // post-submit callback 

      // other available options: 
      //clearForm: true        // clear all form fields after successful submit 
      //resetForm: true        // reset the form after successful submit 

      // $.ajax options can be used here too, for example: 
      //timeout:   3000 
  }; 

function addAjaxForm(formId) {
	//add change event hanlder to every input in a form
	$(formId+" input, "+formId+" select, "+formId+" textarea").change(function() {
		showStatus("Saving...");
		
		//submit all data of the form
		$(formId).ajaxSubmit(options); 
		return false;
	});
}

//Show save result in the top popup
function showResponse(responseText, statusText, xhr, $form)  {
	if (responseText.indexOf("Error:") === 0) {
		var errorText = responseText.replace("Error:", "");
		$("#savedHelpText").html(""); 
		$("#savedHelpError, #saveBtnText").html(errorText); 
		closeTimeout = setTimeout(function() { $("#savedHelp").fadeOut(200) }, 4000);
	}
	else {
		$("#savedHelpText, #saveBtnText").html("Saved!"); 
		closeTimeout = setTimeout(function() { $("#savedHelp").fadeOut(200) }, 2500);
	}
} 



/* --------------- Feeds ---------------- */
//used for paging in different feeds
function loadMoreFeed(type, talkerName) {
	var lastActionId = $("#"+type+"List").children().last().attr("id");
	
	//replace More button with loading image
	var moreBtn = $("#"+type+"Btn");
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();
	
	//Code added for multiple cancer types
	var feedType = "";
	if(type == "allFeed"){
		feedType = "allFeed"
	}else{
		feedType = type;
	}
	//public static void conversationFeedAjax(String afterActionId) {
	$.get("/home/feedAjaxLoad", {afterActionId: lastActionId, feedType: feedType, talkerName: talkerName},
			function(data) {
				$("#ajaxLoading").hide();
		
				//show more button if it isn't the end of a feed
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize >= feedsPerPage) {
					moreBtn.show();
				}
				$(data).appendTo($("#"+type+"List"));
				
				//for new items
				$('.inline-edit').inlineEdit( { hover: ''} );
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '...less' });
			}
		);
	
	return false;
}

//for comments/replies in feed
function showReplyForm(commentId) {
	$(".reply"+commentId+" .inline_display").hide();
	$(".reply"+commentId+" .inline_form").show();
	$(".reply"+commentId).show();
	return false;
}
function showAllReplies(commentId) {
	$(".comment"+commentId+" .comreply").fadeIn();
	return false;
}

function showAllThankYouReplies(commentId) {
	var elements = document.getElementsByClassName("comment"+commentId+" comreply");
	for(var i = 0;i < elements.length;i++){ 
		elements[i].style.display = '';
	}
	return false;
}

//used for paging in different feeds without logged in
function loadMoreFeedWithoutLogin(type, talkerName) {
	var lastActionId = $("#"+type+"List").children().last().attr("id");
	//replace More button with loading image
	var moreBtn = $("#"+type+"Btn");
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();

	//public static void conversationFeedAjax(String afterActionId) {
	$.get("/explore/feedAjaxLoad", {afterActionId: lastActionId, feedType: type, talkerName: talkerName},
			function(data) {
				$("#ajaxLoading").hide();
		
				//show more button if it isn't the end of a feed
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize >= feedsPerPage) {
					moreBtn.show();
				}
				
				$(data).appendTo($("#"+type+"List"));
				//for new items
				$('.inline-edit').inlineEdit( { hover: ''} );
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '...less' });
 			}
		);
	
	return false;
}



/* ---------------- Other common functions ----------------- */

//hide panel with help info (is showed on some pages)
function hideHelpInfo(type, saveFlag) {
	$("#"+type+"Help").hide();
	
	if (saveFlag) {
		$.post("/profile/hideHelpInfo", {type: type});
	}
	return false;
}

/*
 * Makes a link instead of plain text url.
 * Code's discussed here: http://stackoverflow.com/questions/37684/replace-url-with-html-links-javascript
 * 
 */
function linkify(inputText) {
	var replaceText, replacePattern1, replacePattern2, replacePattern3;

	//URLs starting with http://, https://, or ftp://
	replacePattern1 = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
	replacedText = inputText.replace(replacePattern1, '<a href="$1" target="_blank">$1</a>');

	//URLs starting with www. (without // before it, or it'd re-link the ones done above)
	replacePattern2 = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
	replacedText = replacedText.replace(replacePattern2, '$1<a href="http://$2" target="_blank">$2</a>');

	//Change email addresses to mailto:: links
	//replacePattern3 = /(\w+@[a-zA-Z_]+?\.[a-zA-Z]{2,6})/gim;
	//replacedText = replacedText.replace(replacePattern3, '<a href="mailto:$1">$1</a>');

	return replacedText;
}

//send verification link for email
function sendVerificationLink(link) {
	$(link).html("Sending...");
	var email = $(link).attr("title");
	var verificationLink = $(link);
	var verificationResult = $(link).next("span");
	$.post("/profile/resendVerificationEmail", 
		{email: email},
		function(data) {
			verificationLink.hide();
			verificationResult.html(data).show();
		}
	);
	return false;
}

//Shows popup window with mask background
function showPopup(id, popupWidth) {
	//Get the screen height and width
	var maskHeight = $(document).height();
	var maskWidth = $(window).width();

	//Set height and width to mask to fill up the whole screen
	$('#mask').css({'width':maskWidth,'height':maskHeight});
	
	//show background	
	$('#mask').fadeIn(500);	
	$('#mask').fadeTo("slow",0.6);	

	//Get the window height and width
	var winH = $(window).height();
	var winW = $(window).width();
          
	//Set the popup window to center and show
	$(id).css('top', 40);
	$(id).css('left', winW/2-popupWidth);
	$(id).fadeIn(200); 
	
	return false;
}

//hides background and popup
function hideAll() {
	$('#mask').hide();
	$('.window').hide();
	return false;
}

//limits number of symbols in a field
function limitText(limitField, limitNum) {
    if (limitField.value.length > limitNum) {
        limitField.value = limitField.value.substring(0, limitNum);
        return false;
    }
    return true;
}

function initOldTabs(activeTabName) {
	$(".tab_content").hide();
	//activate initial tab
	$("ul.tabs li a[href='#"+activeTabName+"']").parent().addClass("active").show(); 
	$("#"+activeTabName).show();

	$("ul.tabs li ").click(function() {
		$("ul.tabs li ").removeClass("active");
		//add "active" class to selected tab
		$(this).addClass("active"); 
		$(".tab_content").hide();
		
		//find the rel attribute value to identify the active tab + content
		var activeTab = $(this).find("a").attr("href"); 
		var type = $(this).find("a").attr("id");
		$(activeTab).fadeIn();
		loadMoreUser(type,'b');
		return false;
	});
}
function initNewTabs() {
	$(".tabLink").click(function() {
		$(".newActiveTab").removeClass("newActiveTab").addClass("newTab");
		$(this).parent().removeClass("newTab").addClass("newActiveTab");
		
		var id = $(this).attr("id");
		$(".tabContent").hide();
		$(".replytommentbox").hide();
		$("#"+id+"Content").fadeIn();
		
		$("#"+id+"Notifications").addClass("morenotification");
		
		//Code to show hide respective buttons also
		$("#"+id+"Btn").fadeIn();
		return false;
	});
}

//for javascript pagination
function prepareList(type) {
	$("#"+type+"List").children().each(
   		function(i, item) {
    	   		if (perPage <= i) {
   	    	   		$(item).css("display", "none");
    	   		}
    	   		//if needed - show More button
    	   		if (i == perPage) {
   	    	   		$("#"+type+"MoreBtn").show();
    	   		}
   		}
   	);
}

function showMore(type) {
	$("#"+type+"List").children(":hidden").each(
   		function(i, item) {
    	   		if (perPage > i) {
   	    	   		$(item).css("display", "block");
    	   		}
   		}
	);
	if ($("#"+type+"List").children(":hidden").size() == 0) {
		$("#"+type+"MoreBtn").hide();
	}
	return false;
}

function showStatus(statusText) {
	if (closeTimeout) {
		window.clearTimeout(closeTimeout);
	}
	$("#savedHelpText, #saveBtnText").html(statusText);
	$("#savedHelpError").html("");
	$("#savedHelp").fadeIn(300);
}

//Remove help text from text field
function clearTextArea (id, defaultText) {
	var value = $("#"+id).val();
	$("#"+id).removeClass("greyarea");
	if (value === defaultText) {
   		$("#"+id).val("");
	}
}

/* ------------------- Sharing ---------------------- */

function shareEmailPopup() {
	$("#shareResultError").html("");
	showPopup("#shareEmailDialog", 350);
}
function shareTwitterPopup() {
	if (hasTwitter) {
		$('#shareTwitterSize').html($("#sharetwitterNote").val().length);
		showPopup("#shareTwitterDialog", 350);
	}
	else {
		//show Twitter login
		openTwitter(currentURL+'?share=twitter');
	}
}
function shareFBPopup() {
	if (hasFacebook) {
		showPopup("#shareFBDialog", 450);
	}
	else {
		//show FB login
		openFacebook(currentURL+'?share=facebook');
	}
}

function sharePage(type, pageType, pageInfo) {
	var emails = $("#shareEmails").val();
	var userName = $("#shareUserName").val();
	var note = $("#share"+type+"Note").val();
	
	$("#shareResultError").html("");

	$.post("/home/share", 
		{ emails: emails, from: userName, note: note, type: type, pageType: pageType, pageInfo: pageInfo },
		function(data) {
			if (data.indexOf("Error") != -1) {
				$("#shareResultError").html(data);
			}
			else {
				/* 
When a user successfully shares a Topic or Conversation, 
let's close the popup and have a notification at the top of the screen that says 
"Successfully posted to Facebook." "Successfully posted to Twitter." or "Email sent successfully."				 
				 */ 
				hideAll();
				$("#shareEmails").val("");
				
				var resultText = "Email sent successfully.";
				if (type === "twitter") {
					resultText = "Successfully posted to Twitter.";
				}
				else if (type === "facebook") {
					resultText = "Successfully posted to Facebook.";
				}
				
				showStatus(resultText);
				closeTimeout = setTimeout(function() { $("#savedHelp").fadeOut(200) }, 2500);
			}
		}
	);

	return false;
}

/* --------------- Topics ---------------- */
//used for paging in topics on topic listing page
function loadMoreTopics() {
	var lastActionId = $("#topipsts").children().last().attr("id");

	var moreBtn = $("#moreTopicBtn");
	$("#ajaxLoading").appendTo(moreBtn).show();
	var topicCount = $("#currentTopicCount").val();
	$.get("/explore/topicAjaxLoad", {'topicCount':topicCount},
		function(data) {
			$("#ajaxLoading").hide();
			$(data).appendTo($("#topipsts"));
			if(jQuery.trim(data) == 'No'){
				$("#moreId").hide();
			}
		}
	);
}

/* --------------- Feeds ---------------- */
//used for paging in different feeds
function loadMoreTopicsFeed(type,title) {
	var lastActionId = $("#"+type+"Content").children().last().attr("id");
	//replace More button with loading image
	var moreBtn = $("#"+type+"Btn");
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();
	$.get("/topics/topicAjaxLoad", {"feedType": type, "afterActionId": lastActionId, "title":title },
			function(data) {
				$("#ajaxLoading").hide();
				//show more button if it isn't the end of a feed
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize >= feedsPerPage) {
					moreBtn.show();
				}
				$(data).appendTo($("#"+type+"Content"));
				//for new items
				$('.inline-edit').inlineEdit( { hover: ''} );
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '...less' });
			}
	);
	return false;
}

function refreshMembers(type,elm){
	var lastActionId = '';
	var moreBtn = $("#"+type+"List");
	lastActionId = $(moreBtn).children().last().attr("id");
	/*if(type == 'TOPIC'){
		lastActionId = $(moreBtn).children().last().attr("id");
	}*/
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();
	$.get("/home/feedAjaxLoad", {afterActionId: lastActionId, feedType: type, talkerName: ''},
			function(data) {
				$("#"+type+"List").html(data);
				$("#ajaxLoading").hide();
				moreBtn.show();
			}
	);
}

function loadMoreUser(type,from){
	

	var lastActionId = '';
	var moreBtn = $("#"+type+"Tab");
	if(from && from == 'b'){
		lastActionId = '';
	}else{
		lastActionId = $(moreBtn).children().last().attr("id");
	}

	var searchTerm = '';
	//For search page only
	if(type == 'search'){
		searchTerm  = document.searchForm.query.value;
	}

	var typeObj = type.replaceAll(' ', '-');
	typeObj = typeObj.replaceAll('&', 'and');
	$("#ajaxLoading").appendTo($("#"+typeObj+"TabFirst")).show();

	var typeParam = replaceAll(type,"-"," ");
	typeParam = replaceAll(typeParam,"and","&");

	//Useful for the special charactors in the string
	$.get("/explore/ajaxLoadMoreUser", {afterActionId: lastActionId, feedType: typeParam, searchTerm : searchTerm},
		function(data) {
			populateMemberArea(data,type,from);
		}
	);
	 
	return false;
}

String.prototype.replaceAll=function(s1, s2) {return this.split(s1).join(s2)}

function populateMemberArea(data,type,from){
	type = type.replaceAll(' ', '-');
	type = type.replaceAll('&', 'and');
	if(from && from == 'b'){
		$("#"+type+"TabFirst").html("");
		$(data).appendTo($("#"+type+"TabFirst"));
		$('.moretext').truncatable({ limit: 70, more: '... more', less: true, hideText: '...less' });
		$("#ajaxLoading").hide();
	}else{
		var moreBtn = $("#"+type+"Tab");
		$(data).appendTo($("#"+type+"Tab"));
		$('.moretext').truncatable({ limit: 70, more: '... more', less: true, hideText: '...less' });
		if(data == ""){
			$("#"+type+"Btn").hide();
		}else{
			moreBtn.show();
		}
		$("#ajaxLoading").hide();
	}
	
}

function replaceAll(strText,oldParam,newParam){
	var strReplaceAll = strText;
	var intIndexOfMatch = strReplaceAll.indexOf( oldParam );
	while (intIndexOfMatch != -1) {
		strReplaceAll = strReplaceAll.replace( oldParam, newParam )
		intIndexOfMatch = strReplaceAll.indexOf( oldParam );
	}
	return strReplaceAll;
}

//Used for displaying search result
function makeFullSearchAjaxLoad(inputId) {
	
	var limit =  $(".joinpic").size() + 10;
	var query = $(inputId).val();
	if (query === '') {
		alert("Please enter search query");
		return false;
	}
	var moreBtn = $("#convoFeedBtn");
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();
	
	$.get("/searchAjaxLoad", {"query": query, "limit": limit},
			function(data) {
				$("#ajaxLoading").hide();
		
				//show more button if it isn't the end of a feed
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize >= feedsPerPage && limit < totalCount) {
					moreBtn.show();
				}
				$("#convoList").html("");
				$(data).appendTo($("#convoList"));
				//for new items
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '...less' });
			}
		);
	
	return false;
}

//saveProfileComment(String profileTalkerId, String parentId, String text)
function saveProfileThankYouComment(parentId, parentList, userName) {
	alert("saveProfileThankYouComment");
	var parentListId = "";
	if (parentList) {
		parentListId = "#"+parentList+" ";
	}
	var commentText = $(parentListId+".replytext"+parentId).val();
//	alert(parentListId+".replytext"+parentId);
//	alert(commentText);
	$(parentListId+".saveThoughtImage"+parentId).show();
	$(parentListId+".replytext"+parentId).val("");
	
	//YURIY: FIX FOR URLS IN REPLIES NOT BEING TRANSLATED -- SPENT 2h TO HUNT THIS DOWN %-/
	//linkedText = linkify(commentText);
	linkedText = commentText;

	$.post("/actions/saveProfileThankYouReply", 
		{ parentId: parentId, text: linkedText,  parentList: parentList, from: "thankyou"},
		function(html) {
			$("#firstcommentmessage").hide();
			$(parentListId+".saveThoughtImage"+parentId).hide();
			//put comment in the tree
			if (parentId == '') {
				//add as first element in the top
				$(html).prependTo($(".commentsarea"));

				//add inline edit for new comment
				$('.inline-edit').inlineEdit( { hover: ''} );
			}
			else {
				//add as last element in subtree
				$(parentListId+".reply"+parentId).before($(html));
			}
		}
	);
	return false;
}

//search mail
function mailSearch(inputId, value){
	var limit =  $(".joinpic").size() + 10;
	var query = "";
	if(value == "" || value == undefined){
		query = $(inputId).val();
	}else{
		query = value;
	}
	if (query === '') {
		alert("Please enter search query");
		return false;
	}
	$.get("/search/messageSearch", {"mailSubject": query},
			function(data) {
				var url = "/message/email?id="+data;
				document.location = url;
			}
		);
}

function trim(stringToTrim) {
	return stringToTrim.replace(/^\s+|\s+$/g,"");
}

//used for paging in question feed for mobile
function loadMoreFeedMob(type) {
	var lastActionId = $("#"+type+"List").children().last().attr("id");
	//replace More button with loading image
	var moreBtn = $("#"+type+"Btn");
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();

	//public static void conversationFeedAjax(String afterActionId) {
	$.get("/m/feedAjaxLoad", {afterActionId: lastActionId, feedType: type},
			function(data) {
				$("#ajaxLoading").hide();
		
				//show more button if it isn't the end of a feed
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize >= feedsPerPage) {
					moreBtn.show();
				}
				
				$(data).appendTo($("#"+type+"List"));
				//for new items
				$('.inline-edit').inlineEdit( { hover: ''} );
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '...less' });
			}
		);
	
	return false;
}

//used for change health community or remove from health community
function changeHealthCommunity(community, operation, fromPage){
	$.get("/Profile/updateHealthCommunity",{community: community, operation: operation},
			function(data) {
				if(fromPage == 'PROFILE')
					window.location.href = "/profile/profileinfo";
				else
					window.location.href = "/home";
			}
		);
}

//used for paging in community feeds
function loadMoreCommunityFeed(type, cancerType) {
	var lastActionId = $("#"+type+"List").children().last().attr("id");
	
	//replace More button with loading image
	var moreBtn = $("#"+type+"Btn");
	$("#ajaxLoading").appendTo(moreBtn.parent()).show();
	moreBtn.hide();
	
	//Code added for multiple cancer types
	var feedType = "";
	if(cancerType == "All Cancers"){
		feedType = "allFeed"
	}else{
		feedType = type;
	}
	//public static void conversationFeedAjax(String afterActionId) {
	$.get("/explore/communityFeedAjaxLoad", {afterActionId: lastActionId, feedType: feedType, cancerType: cancerType},
			function(data) {
				$("#ajaxLoading").hide();
				
				//show more button if it isn't the end of a feed
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize >= feedsPerPage) {
					moreBtn.show();
				}
				$(data).appendTo($("#"+type+"List"));
				
				//for new items
				$('.inline-edit').inlineEdit( { hover: ''} );
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '...less' });
			}
		);
	
	return false;
}

function showMessagePopup(){
	showPopup("#messagePopup", 350);
}

function sendMessageToUser(){
	var user= document.getElementById("userName").value;
	var subject = document.getElementById("subject").value;
	var message = document.getElementById("message").value;
	var isValid = true;
	if(user == ""){
		isValid = false;
		alert("Please enter the user");
	}else if(subject == ""){
		isValid = false;
		alert("Please enter the subject");
	}else if(message == ""){
		isValid = false;
		alert("Please enter the message");
	}
	if(isValid == true){
		var messageForm = document.getElementById('messageForm');
		messageForm.submit();
	}
}

function hideMessagePopup(){
	$("#messagePopup").hide();
	$('#mask').hide();
}