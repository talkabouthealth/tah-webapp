/* Scripts for all pages */
var selectedTalkerId = '';

var flagType = "";
var flagConvoId = "";
var flagAnswerId = "";

var titleLimit = 150;

function openChat(convoId){
	window.open("/talk/"+convoId, "TalkerWindow", "width=730,height=565");
}

function openInvitationsWindow() {
	window.open("/home/invitations", "TalkAboutHealthInvitations", "width=600,height=350");
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

//saveProfileComment(String profileTalkerId, String parentId, String text)
function sendProfileComment() {
	var commentText = $("#commentText").val();
	$.fancybox.close();
	$("#commentText").val("");

	$.post("/actions/saveProfileComment", 
		{ profileTalkerId: selectedTalkerId, parentId: '', text: commentText}
	);
}

$(document).ready(function() {
	
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
function makeTopicsAutocomplete(id) {
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
				data: { term : currentTerm },
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



function loadMoreFeed(type) {
	var lastActionId = $("#"+type+"List").children().last().attr("id");

	//public static void conversationFeedAjax(String afterActionId) {
	$.get("/home/feedAjaxLoad", {afterActionId: lastActionId, feedType: type},
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

function showQuestionDialog() {
	$("#newConvoTypeQuestion").attr("checked", "checked");
	
	$("#newConvoForm").show();
	$("#newQuestionConfirm").hide();
	$("#newTalkConfirm").hide();
	if ($("#convoText") && $("#convoText").val() !== "") {
		$("#newConvoTitle").val($("#convoText").val());
	}
	showPopup("#startDialog", 350);
	
	return false;
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
					$("#newTalkConfirm").show();
				}
				else {
					$("#questionLink").attr("href", data.url).html(data.url);
					$("#newQuestionConfirm").show();
				}
				$("#newConvoForm").hide();
				
				if (!(type === "QUESTION" && page === "liveTalks")) {
					if ($(".conversationsList")) {
						$(data.html).prependTo($(".conversationsList"));
					}
				}

				$("#convoText").val("");
				$("#newConvoTitle").val("");
				$("#newConvoDetails").val("This is the place to provide context or further details for your request.");
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

	$.post("/actions/saveProfileComment", 
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