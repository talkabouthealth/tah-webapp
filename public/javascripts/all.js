/* Scripts for all pages */
var selectedTalkerId = '';

function openChat(convoId){
	window.open("/talk/"+convoId, "TalkerWindow", "width=730,height=565");
}

function openInvitationsWindow() {
	window.open("/home/invitations", "TalkAboutHealthInvitations", "width=600,height=350");
}

function restartConvo(tid, convoId) {
	openChat(tid);
	$.post("/conversations/restart", {topicId: convoId});
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
	
	//Dialog popus
	//Hack to use Fancybox with 'live' event - fancybox on mouseover
	$(".commentLink, .thankYouLink").live('mouseover', function(event) {
			selectedTalkerId = $(this).attr("rel2");
			$(this).fancybox({
				    'hideOnContentClick': false,
				    'showCloseButton': false
				});
		})
});

function showPopup(id) {
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
	$(id).css('top',  winH/3-$(id).height()/2);
	$(id).css('left', winW/2-$(id).width()/2);

	//transition effect
	$(id).fadeIn(200); 
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
	var url = "";
	if (type === "all") {
		url = "/search/ajaxSearch";
	}
	else if (type === "convo") {
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
			if (type === "all") {
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
