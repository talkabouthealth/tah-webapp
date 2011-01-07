/*
 * TODO: structure this JavaScript file
 */

/* Scripts for all pages */
var selectedTalkerId = '';

var flagType = "";
var flagConvoId = "";
var flagAnswerId = "";

var titleLimit = 150;

function openChat(convoId){
	window.open("/chat/"+convoId, "Chat", "resizable=1, scrollbars=false, width=810,height=610");
}

function openInvitationsWindow() {
	window.open("/home/invitations", "TalkAboutHealthInvitations", "width=600,height=350");
}

function openTwitter(redirectURL) {
	if (!redirectURL) {
		redirectURL = "";
	}
	var popupWindow = window.open("/oauth/getauth?type=twitter&redirectURL="+redirectURL, 
		"TwitterLogin", "width=800,height=430,toolbar=no,location=no,menubar=no");
	
	//TODO: center pop-up on the screen and make one function for all pages
	//for prototype library
	//var offsets = document.viewport.getScrollOffsets();
	//var x = Math.ceil((document.viewport.getWidth()/2)-425);
	//var y = Math.ceil(200);
	//popupWindow.moveTo(x,y);
	return false;
}

function openFacebook(redirectURL) {
	if (!redirectURL) {
		redirectURL = "";
	}
	var popupWindow = window.open("/oauth/getauth?type=facebook&redirectURL="+redirectURL,  
		"FacebookLogin", "width=1000,height=550,toolbar=no,location=no,menubar=no");
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

function flag() {
	var reason = $("#flagReason").val();
	$("#flagReason").val("");
	if (flagType === "convo") {
		$.post("/conversations/flag", 
				{reason: reason, convoId: flagConvoId}
		);
	}
	else if (flagType === "answer") {
		$.post("/conversations/flaganswer", 
			{reason: reason, answerId: flagAnswerId}
		);
	}

	hideAll();
	showPopup("#flagConfirm", 200);
	return false;
}

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

//createThankYou(String toTalker, String note)
function sendThankYou() {
	var noteText = $("#thankYouListNote").val();
	$.fancybox.close();
	$("#thankYouListNote").val("");

	$.post("/actions/createThankYou", 
		{ toTalker: selectedTalkerId, note: noteText}
	);
}

$(document).ready(function() {
	
	$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '' });
	
	//links to show more content (e.g. full bio)
	$(".more11").live("click", 
		function() {
			$(this)
				.parent().hide()
				.next(".fulltext").show();
		});
	
	$(".followActionLink").live('click', 
		function() {
			var followingId = $(this).attr("rel");
			var followLink = $(this);
  			$.post("/actions/follow", 
 				{ followingId: followingId },
 				function(data) {
 					followLink.html(data);
 				}
 			);
  			
  			return false;
		});
	
	$(".followConvoLink").live('click', function() {
		var convoId = $(this).attr("rel");
		if ($(this).html().indexOf("Unfollow") != -1) {
			$(this).html("Follow");
		}
		else {
			$(this).html("Unfollow");
		}
		$.post("/conversations/follow}", 
				{ convoId: convoId },
				function(data) {
					//...
				}
			);
		return false;
	});
	
	
	
	//if close button is clicked
	$('.window .close, .window .cancelLink').click(function (e) {
		//Cancel the link behavior
		e.preventDefault();
		hideAll();
	});
});

function showPopup(id, popupWidth) {
	//Get the screen height and width
	var maskHeight = $(document).height();
	var maskWidth = $(window).width();

	//Set heigth and width to mask to fill up the whole screen
	$('#mask').css({'width':maskWidth,'height':maskHeight});
	
	//transition effect		
	$('#mask').fadeIn(500);	
	$('#mask').fadeTo("slow",0.6);	

	//Get the window height and width
	var winH = $(window).height();
	var winW = $(window).width();
          
	//Set the popup window to center
	$(id).css('top', 50);
	$(id).css('left', winW/2-popupWidth);

	//transition effect
	$(id).fadeIn(200); 
	
	return false;
}

function hideAll() {
	$('#mask').hide();
	$('.window').hide();
	
	return false;
}

function limitText(limitField, limitNum) {
    if (limitField.value.length > limitNum) {
        limitField.value = limitField.value.substring(0, limitNum);
    } 
}

function makeAutocomplete(id, type) {
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
	}
	
	var cache = {};
	$(id).autocomplete({
		minLength: 1,
		source: function(request, response) {
			if ( request.term in cache ) {
				response( cache[ request.term ] );
				return;
			}
			
			$.ajax({
				url: url,
				dataType: "json",
				data: request,
				success: function( data ) {
					cache[ request.term ] = data;
					response( data );
				}
			});
		},
		select: function(event, ui) {
			if (type === "all" || type === "convo") {
				var url = ui.item.url;
				if (url === "#fullsearch") {
					//go to full conversations search
					url = "/search/conversations?query="+ui.item.value;
				}
				document.location = url;
			}
			else {
				$(id).val(ui.item.value);
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

/* Autocompete for topis - allows adding multiply topics (separated by space) */
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
function split( val ) {
	return val.split( /,\s*/ );
}
function extractLast( term ) {
	return split( term ).pop();
}



function loadMoreFeed(type, talkerName) {
	var lastActionId = $("#"+type+"List").children().last().attr("id");

	//public static void conversationFeedAjax(String afterActionId) {
	$.get("/home/feedAjaxLoad", {afterActionId: lastActionId, feedType: type, talkerName: talkerName},
			function(data) {
				var feedSize = $(data).find(".joinpic").size();
				if (feedSize < feedsPerPage) {
					//no more feeds - hide 'More' link
					$("#"+type+"Btn").hide();
				}
				
				$(data).appendTo($("#"+type+"List"));
				
				$('.inline-edit').inlineEdit( { hover: ''} );
				$('.moretext').truncatable({ limit: 160, more: '... more', less: true, hideText: '' });
			}
		);
	
	return false;
}

function showStartConvoDialog(type) {
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

	if (title === "") {
		alert("Please input headline.");
		return false;
	}

	$.post("/conversations/create", 
			{ type: type, title: title, details: details, topics: topics, fromPage: page},
			function(data) {
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
				
				if (!(type === "QUESTION" && page === "liveTalks")) {
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



//'thankYou' or 'comment' popups
var selectedTalkerId = '';

function showPopupForm (type, talkerId, userName) {
	$("#"+type+"User").html(userName);
	selectedTalkerId = talkerId;

	showPopup("#"+type+"Popup", 200);
	return false;
}

function saveThankYou() {
	var noteText = $("#thankYouListNote").val();
	hideAll();
	$("#thankYouListNote").val("");

	$.post("/actions/createThankYou", 
		{ toTalker: selectedTalkerId, note: noteText}
	);
	return false;
}

function sendProfileComment() {
	var commentText = $("#commentText").val();
	hideAll();
	$("#commentText").val("");

	$.post("/actions/saveProfileComment2", 
		{ profileTalkerId: selectedTalkerId, parentId: '', text: commentText}
	);
}


function hideHelpInfo(type, saveFlag) {
	$("#"+type+"Help").hide();
	
	if (saveFlag) {
		$.post("/profile/hideHelpInfo", {type: type});
	}
	return false;
}

/*
 * Code's discussed here: http://stackoverflow.com/questions/37684/replace-url-with-html-links-javascript
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


/* Ajax form saves */
//TODO: maybe timeout would be better?
var closeInterval;
var options = { 
        //target:        '#output2',   // target element(s) to be updated with server response 
        //beforeSubmit:  showRequest,  // pre-submit callback 
        success:       showResponse  // post-submit callback 
 
        // other available options: 
        //url:       url         // override for form's 'action' attribute 
        //type:      type        // 'get' or 'post', override for form's 'method' attribute 
        //dataType:  null        // 'xml', 'script', or 'json' (expected server response type) 
        //clearForm: true        // clear all form fields after successful submit 
        //resetForm: true        // reset the form after successful submit 
 
        // $.ajax options can be used here too, for example: 
        //timeout:   3000 
    }; 


function addAjaxForm(formId) {
	$(formId+" input, "+formId+" select, "+formId+" textarea").change(function() {
		if (closeInterval) {
			window.clearInterval(closeInterval);
		}
		$("#savedHelpText, #saveBtnText").html("Saving...");
		$("#savedHelpError").html("");
		$("#savedHelp").fadeIn(300);
		
		$(formId).ajaxSubmit(options); 
		return false;
	});
}

function showResponse(responseText, statusText, xhr, $form)  {
	if (responseText.indexOf("Error:") === 0) {
		var errorText = responseText.replace("Error:", "");
		$("#savedHelpText").html(""); 
		$("#savedHelpError, #saveBtnText").html(errorText); 
		closeInterval = setInterval(function() { $("#savedHelp").fadeOut(200) }, 4000);
	}
	else {
		$("#savedHelpText, #saveBtnText").html("Saved!"); 
		closeInterval = setInterval(function() { $("#savedHelp").fadeOut(200) }, 2500);
	}
} 



//for comments/replies in feed
function showReplyForm(commentId) {
	$("#reply"+commentId+" .inline_display").hide();
	$("#reply"+commentId+" .inline_form").show();
	$("#reply"+commentId).show();
	return false;
}
function showAllReplies(commentId) {
	$("#comment"+commentId+" .comreply").fadeIn();
	return false;
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
		$(activeTab).fadeIn();
		return false;
	});
}
function initNewTabs() {
	$(".tabLink").click(function() {
		$(".newActiveTab").removeClass("newActiveTab").addClass("newTab");
		$(this).parent().removeClass("newTab").addClass("newActiveTab");
		
		var id = $(this).attr("id");
		$(".tabContent").hide();
		$("#"+id+"Content").fadeIn();
		return false;
	});
	
}


//saveProfileComment(String profileTalkerId, String parentId, String text)
function saveProfileComment(parentId) {
	var commentText = $("#replytext"+parentId).val();
	$("#replytext"+parentId).val("");

	$.post("/actions/saveProfileComment2", 
		{ parentId: parentId, text: commentText},
		function(html) {
			$("#firstcommentmessage").hide();
			//put comment in the tree
			if (parentId == '') {
				//add as first element in the top
				$(html).prependTo($(".commentsarea"));

				//add inline edit for new comment
				$('.inline-edit').inlineEdit( { hover: ''} );
			}
			else {
				//add as last element in subtree
				$("#reply"+parentId).before($(html));
			}
		}
	);

	
	return false;
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

function shareTopic(type, itemName) {
	var emails = $("#shareEmails").val();
	var userName = $("#shareUserName").val();
	var note = $("#share"+type+"Note").val();
	
	$("#shareResultError").html("");
	$("#shareResultText").html("");

	$.post("/home/share", 
		{ emails: emails, from: userName, note: note, type: type, item: itemName },
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
				
				//TODO: similar code?
				if (closeInterval) {
					window.clearInterval(closeInterval);
				}
				$("#savedHelpText").html(resultText);
				$("#savedHelpError").html("");
				$("#savedHelp").fadeIn(300);
				closeInterval = setInterval(function() { $("#savedHelp").fadeOut(200) }, 2500);
			}
		}
	);

	return false;
}