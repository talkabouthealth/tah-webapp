/* Scripts for all pages */
var selectedTalkerId = '';

function openChat(convoId){
	window.open("/talk/"+convoId, "TalkerWindow", "width=730,height=565");
}

function openInvitationsWindow() {
	window.open("/home/invitations", "TalkAboutHealthInvitations", "width=600,height=350");
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
